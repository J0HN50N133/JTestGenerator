package jtg.solver;

import com.microsoft.z3.*;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import jtg.utils.Path;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import java.util.*;

public class Z3Solver {

    static Sort getSortType(Type type, Context ctx){
        if (type instanceof ByteType || type instanceof IntType
                || type instanceof LongType || type instanceof ShortType || type instanceof CharType) {
            return ctx.mkIntSort();
        } else if (type instanceof DoubleType || type instanceof FloatType) {
            return ctx.mkFPSortDouble();
        } else if (type instanceof BooleanType) {
            return ctx.mkBoolSort();
        } else if (type instanceof ArrayType) {
            Type elementType = ((ArrayType) type).getElementType();
            return ctx.mkArraySort(ctx.mkIntSort(), getSortType(elementType, ctx));
        }
            return null;
    }

    static Expr cnvConst(Context ctx, Value value) {
        assert (value instanceof Constant);
        if (value instanceof IntConstant) {
            return ctx.mkInt(((IntConstant) value).value);
        } else if (value instanceof FloatConstant) {
            return ctx.mkFP(((FloatConstant) value).value, ctx.mkFPSortDouble());
        } else if (value instanceof DoubleConstant){
            return ctx.mkFP(((DoubleConstant) value).value, ctx.mkFPSortDouble());
        } else if (value instanceof StringConstant) {
            return ctx.mkString(((StringConstant) value).value);
        } else if (value instanceof LongConstant) {
            return ctx.mkInt(((LongConstant) value).value);
        }
        return null;
    }

    private static Expr getExpr(Context ctx, Value value, Map<Value, Expr> valueExprMap) {
        return value instanceof Constant ? cnvConst(ctx, value) : valueExprMap.get(value);
    }
    private static Expr cnvJAssignStmt(Context ctx, JAssignStmt stmt, Map<Value, Expr> valueExprMap){
        Value right = stmt.getRightOp();
        return parseJimp2Z3(right, ctx, valueExprMap);
    }
    private static Expr cnvJIdentityStmt(Context ctx, JIdentityStmt stmt){
        Type type = stmt.getLeftOp().getType();
        Sort sort = getSortType(type, ctx);
        if (sort == null) return null;
        return ctx.mkConst(stmt.getLeftOp().toString() ,sort);
    }
    public static Set<Expr> calculatePathConstraint(Context ctx, Path path){
        Map<Value, Expr> valueExprMap = new HashMap<>();
        Set<Expr> constraints = new HashSet<>();
        List<Unit> units = path.getUnits();
        for (Unit unit : units) {
            if (unit instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) unit;
                Value left = stmt.getLeftOp();
                valueExprMap.put(left,  cnvJAssignStmt(ctx, stmt, valueExprMap));
            } else if (unit instanceof JIdentityStmt) {
                JIdentityStmt stmt = (JIdentityStmt) unit;
                Value left = stmt.getLeftOp();
                Expr identity = cnvJIdentityStmt(ctx, stmt);
                if (identity == null) continue;
                valueExprMap.put(left, identity);
            } else if (unit instanceof JIfStmt) {
                Expr condExpr = cnvJIfStmt((JIfStmt) unit, ctx, valueExprMap, units);
                constraints.add(condExpr);
            }
        }
        return constraints;
    }

    static class ExprPair{
        Expr expr1;
        Expr expr2;
        boolean isFP;

        public ExprPair(Expr expr1, Expr expr2, boolean isFP) {
            this.expr1 = expr1;
            this.expr2 = expr2;
            this.isFP = isFP;
        }
    }

    private static ExprPair dealWithFP(Context ctx, Value value, Map<Value, Expr> valueExprMap) {
        Value op1 = ((BinopExpr) value).getOp1();
        Value op2 = ((BinopExpr) value).getOp2();
        Expr expr1 = getExpr(ctx, op1, valueExprMap);
        Expr expr2 = getExpr(ctx, op2, valueExprMap);
        boolean isFP = false;
        if ((!expr1.isInt()) && (op2 instanceof IntConstant)) {
            expr2 = cnvConst(ctx, DoubleConstant.v((double) ((IntConstant) op2).value));
            isFP = true;
        }
        return new ExprPair(expr1, expr2, isFP);
    }
    private static Expr dealWithEq(Context ctx, Value value, Map<Value, Expr> valueExprMap){
        ExprPair exprPair = dealWithFP(ctx, value, valueExprMap);
        Expr expr1 = exprPair.expr1;
        Expr expr2 = exprPair.expr2;
        if (exprPair.isFP){
            return ctx.mkFPEq(expr1, expr2);
        }
        return ctx.mkEq(expr1, expr2);
    }

    private static Expr dealWithCmpOp(Context ctx, Value value, Map<Value, Expr> valueExprMap) {
        ExprPair exprPair = dealWithFP(ctx, value, valueExprMap);
        Expr expr1 = exprPair.expr1;
        Expr expr2 = exprPair.expr2;
        if (exprPair.isFP) {
            if (value instanceof JLeExpr) {
                return ctx.mkFPLEq(expr1, expr2);
            } else if (value instanceof JLtExpr) {
                return ctx.mkFPLt(expr1, expr2);
            } else if (value instanceof JGeExpr) {
                return ctx.mkFPGEq(expr1, expr2);
            } else if (value instanceof JGtExpr) {
                return ctx.mkFPGt(expr1, expr2);
            } else if (value instanceof JEqExpr) {
                return dealWithEq(ctx, value, valueExprMap);
            } else if (value instanceof JNeExpr) {
                return ctx.mkNot(dealWithEq(ctx, value, valueExprMap));
            }
        }else {
            if (value instanceof JLeExpr) {
                return ctx.mkLe(expr1, expr2);
            } else if (value instanceof JLtExpr) {
                return ctx.mkLt(expr1, expr2);
            } else if (value instanceof JGeExpr) {
                return ctx.mkGe(expr1, expr2);
            } else if (value instanceof JGtExpr) {
                return ctx.mkGt(expr1, expr2);
            } else if (value instanceof JEqExpr) {
                return dealWithEq(ctx, value, valueExprMap);
            } else if (value instanceof JNeExpr) {
                return ctx.mkNot(dealWithEq(ctx, value, valueExprMap));
            }
        }
        return null;
    }

    private static Expr parseJimp2Z3(Value value, Context ctx, Map<Value, Expr> valueExprMap){

        if(value instanceof BinopExpr){
            Value op1 = ((BinopExpr) value).getOp1();
            Value op2 = ((BinopExpr) value).getOp2();
            Expr expr1 = getExpr(ctx, op1, valueExprMap);
            Expr expr2 = getExpr(ctx, op2, valueExprMap);

            if(value instanceof JAddExpr){
                return ctx.mkAdd(expr1, expr2);
            } else if(value instanceof JSubExpr){
                return ctx.mkSub(expr1, expr2);
            } else if(value instanceof JMulExpr){
                return ctx.mkMul(expr1, expr2);
            } else if(value instanceof JDivExpr){
                return ctx.mkDiv(expr1, expr2);
            }else if (value instanceof JRemExpr) {
                return ctx.mkRem(expr1, expr2);
            } else if(value instanceof JLeExpr
                    || value instanceof JLtExpr
                    || value instanceof JGeExpr
                    || value instanceof JGtExpr
                    || value instanceof JEqExpr
                    || value instanceof JNeExpr
            ){
                return dealWithCmpOp(ctx, value, valueExprMap);
            } else if(value instanceof JAndExpr){
                return ctx.mkAnd(expr1, expr2);
            } else if(value instanceof JOrExpr){
                return ctx.mkOr(expr1, expr2);
            } else if(value instanceof JXorExpr){
                return ctx.mkXor(expr1, expr2);
            } else if (value instanceof JShlExpr) {
                return ctx.mkMul(expr1, ctx.mkPower(ctx.mkInt(2), expr2));
            } else if (value instanceof JShrExpr) {
                return ctx.mkDiv(expr1, ctx.mkPower(ctx.mkInt(2), expr2));
            } else if (value instanceof JCmpExpr){
                return ctx.mkSub(expr1, expr2);
            } else if (value instanceof JCmplExpr || value instanceof JCmpgExpr){
                FPExpr fpSub = ctx.mkFPSub(ctx.mkFPRoundNearestTiesToAway(), expr1, expr2);
                return ctx.mkFPRoundToIntegral(ctx.mkFPRoundNearestTiesToEven(), fpSub);
            }
        } else if(value instanceof UnopExpr){
            Value op = ((UnopExpr) value).getOp();
            Expr expr = getExpr(ctx, op, valueExprMap);

            if(value instanceof JNegExpr){
                return ctx.mkNot(expr);
            } else if(value instanceof JLengthExpr){
                return ctx.mkLength(expr);
            }
        } else if(value instanceof JimpleLocal){
            return getExpr(ctx, value, valueExprMap);
        } else if(value instanceof Constant){
            return cnvConst(ctx, value);
        } else if (value instanceof JCastExpr) {
            return getExpr(ctx, ((JCastExpr) value).getOp(), valueExprMap);
        }
        return null;
    }

    private static Expr cnvJIfStmt(JIfStmt unit, Context ctx, Map<Value, Expr> valueExprMap, List<Unit> units) {
        JIfStmt stmt = (JIfStmt) unit;
        Value condition = stmt.getCondition();
        Expr condExpr = parseJimp2Z3(condition, ctx, valueExprMap);
        int i = units.indexOf(unit);
        if (!units.get(i+1).equals(((JIfStmt) unit).getTarget())){
            condExpr = ctx.mkNot(condExpr);
        }
        return condExpr;
    }


    public static String solve(Path path) throws Exception {
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        Context ctx = new Context(cfg);
        Set<Expr> constraint = calculatePathConstraint(ctx, path);
        Solver s = ctx.mkSolver();
        for (Expr expr : constraint) {
            s.add(expr);
        }
        StringBuilder res = new StringBuilder();
        try {
            String status = s.check().toString();
            if (status.equals("SATISFIABLE")) {
                res.append(s.getModel().toString());
            } else {
                res.append("");//无解
            }

        }catch (Exception e){
            res.append(e);
        }
        return res.toString();
    }

    public static String solve(String str) throws Exception {
        Set<String> declareBools = new HashSet<>();
        Set<Expr> varList = new HashSet<>();
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
        String asserts = expressionEvaluator.buildExpression(str, declareBools);
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        Context ctx = new Context(cfg);
        Solver s = ctx.mkSolver();
        StringBuilder exprs = new StringBuilder();
        for (String expr : declareBools){
            exprs.append(expr);
            String temp = expr.replaceAll("\\(declare-const ", "").replaceAll(" Real\\)","");
            varList.add(ctx.mkRealConst(temp));
        }
        BoolExpr boolExpr = ctx.parseSMTLIB2String(exprs.toString()+asserts,null,null,null,null)[0];
        s.add(boolExpr);

        StringBuilder res = new StringBuilder();
        try {
            String status = s.check().toString();
            if (status.equals("SATISFIABLE")) {
                for (Expr var : varList) {
                    res.append(var + "=" + s.getModel().eval(var, false) + " ");
                }
            } else {
                res.append("");//无解
            }

        }catch (Exception e){
            res.append(e);
        }
        return res.toString();
    }

}