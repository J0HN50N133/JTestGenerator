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
            return ctx.mkSeqSort(getSortType(elementType, ctx));
        } else if (type instanceof RefType) {
            String className = ((RefType) type).getClassName();
            switch (className){
            case "java.lang.String":
                return ctx.getStringSort();
            default:
                return null;
            }
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

    private enum PairType { IntInt, FPInt, BoolInt }
    static class ExprPair{
        Expr expr1;
        Expr expr2;
        PairType pairType;
        public ExprPair(Expr expr1, Expr expr2, PairType pairType) {
            this.expr1 = expr1;
            this.expr2 = expr2;
            this.pairType = pairType;
        }
    }

    private static ExprPair dealWithType(Context ctx, Value value, Map<Value, Expr> valueExprMap) {
        Value op1 = ((BinopExpr) value).getOp1();
        Value op2 = ((BinopExpr) value).getOp2();
        Expr expr1 = getExpr(ctx, op1, valueExprMap);
        Expr expr2 = getExpr(ctx, op2, valueExprMap);
        PairType pairType = PairType.IntInt;
        if (expr1.isBool() && (op2 instanceof IntConstant)) {
            int intValue = ((IntConstant) op2).value;
            if (intValue == 1) {
                expr2 = ctx.mkTrue();
            } else if (intValue == 0) {
                expr2 = ctx.mkFalse();
            }
            pairType = PairType.BoolInt;
        }else if ((!expr1.isInt()) && (op2 instanceof IntConstant)) {
            expr2 = cnvConst(ctx, DoubleConstant.v((double) ((IntConstant) op2).value));
            pairType = PairType.FPInt;
        }
        return new ExprPair(expr1, expr2, pairType);
    }
    private static Expr dealWithEq(Context ctx, Value value, Map<Value, Expr> valueExprMap){
        ExprPair exprPair = dealWithType(ctx, value, valueExprMap);
        Expr expr1 = exprPair.expr1;
        Expr expr2 = exprPair.expr2;
        if (exprPair.pairType == PairType.FPInt){
            return ctx.mkFPEq(expr1, expr2);
        }
        return ctx.mkEq(expr1, expr2);
    }

    private static Expr dealWithCmpOp(Context ctx, Value value, Map<Value, Expr> valueExprMap) {
        ExprPair exprPair = dealWithType(ctx, value, valueExprMap);
        Expr expr1 = exprPair.expr1;
        Expr expr2 = exprPair.expr2;
        if (exprPair.pairType == PairType.FPInt) {
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
        } else if (value instanceof JArrayRef) {
            Value base = ((JArrayRef) value).getBase();
            Value index = ((JArrayRef) value).getIndex();
            Expr expr1 = getExpr(ctx, base, valueExprMap);
            Expr expr2 = getExpr(ctx, index, valueExprMap);
            return ctx.mkNth(expr1, expr2);
        } else if (value instanceof JVirtualInvokeExpr) {
            Value base = ((JVirtualInvokeExpr) value).getBase();
            String className = base.getType().toString();
            if (className.equals("java.lang.String")) {
                return cnvStringMethod(ctx, value, valueExprMap);
            }
        }
        return null;
    }

    private static Expr cnvStringMethod(Context ctx, Value value, Map<Value, Expr> valueExprMap) {
        JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) value;
        Value base = invokeExpr.getBase();
        SootMethod method = invokeExpr.getMethod();
        String stringMtdName = method.getName();
        switch (stringMtdName) {
        case "length":
            return ctx.mkLength(getExpr(ctx, base, valueExprMap));
        case "substring":
            return ctx.mkExtract(
                    getExpr(ctx, base, valueExprMap),
                    getExpr(ctx, invokeExpr.getArg(0), valueExprMap),
                    ctx.mkSub(
                            getExpr(ctx, invokeExpr.getArg(1), valueExprMap),
                            ctx.mkInt(1)));
        case "equals":
            return ctx.mkEq(
                    getExpr(ctx, base, valueExprMap),
                    getExpr(ctx, invokeExpr.getArg(0), valueExprMap));
        case "contains":
            return ctx.mkContains(
                    getExpr(ctx, base, valueExprMap),
                    getExpr(ctx, invokeExpr.getArg(0), valueExprMap));
        case "indexOf":
            if (invokeExpr.getArgCount() == 1 && invokeExpr.getArg(0).getType() instanceof RefType) {
                return ctx.mkIndexOf(
                        getExpr(ctx, base, valueExprMap),
                        getExpr(ctx, invokeExpr.getArg(0), valueExprMap),
                        ctx.mkInt(0));
            } else if (invokeExpr.getArgCount() == 2
                    && invokeExpr.getArg(0).getType() instanceof RefType
                    && invokeExpr.getArg(1) instanceof IntConstant) {
                return ctx.mkIndexOf(
                        getExpr(ctx, base, valueExprMap),
                        getExpr(ctx, invokeExpr.getArg(0), valueExprMap),
                        getExpr(ctx, invokeExpr.getArg(1), valueExprMap));
            }
        case "startsWith":
            if (invokeExpr.getArgCount() == 1){
                return ctx.mkPrefixOf(
                        getExpr(ctx, base, valueExprMap),
                        getExpr(ctx, invokeExpr.getArg(0), valueExprMap));
            }
        case "endsWith":
            return ctx.mkSuffixOf(
                    getExpr(ctx, base, valueExprMap),
                    getExpr(ctx, invokeExpr.getArg(0), valueExprMap));
        default:
            return null;
        }
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
                String result = cnvModelToReadable(ctx, s.getModel(), path);
                res.append(result);
            } else {
                res.append("Unfeasible");//无解
            }

        }catch (Exception e){
            res.append(e);
        }
        return res.toString();
    }

    private static String cnvModelToReadable(Context ctx, Model model, Path path) {
        StringBuilder sb = new StringBuilder();
        for (Unit unit : path.getUnits()) {
            if (unit instanceof JIdentityStmt) {
                Value rightOp = ((JIdentityStmt) unit).getRightOp();
                if (rightOp instanceof ParameterRef) {
                    ParameterRef param = (ParameterRef) rightOp;
                    Expr expr = cnvJIdentityStmt(ctx, (JIdentityStmt) unit);
                    Expr eval = model.eval(expr, true);
                    sb
                            .append("\n")
                            .append("@parameter")
                            .append(param.getIndex())
                            .append(": ");
                    if (param.getType() instanceof ArrayType) {
                        sb.append("new ")
                                .append(rightOp.getType())
                                .append("{");
                    }
                    sb.append(cnvZ3ExprToString(eval));
                    if (param.getType() instanceof ArrayType) {
                        sb.append("}");
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String cnvZ3ExprToString(Expr expr){
        if (expr.toString().startsWith("(as seq.empty")){
            return "";
        }
        if (expr.getArgs().length == 0) {
            return expr.toString();
        }else if(expr.getArgs().length == 1){
            Expr[] args = expr.getArgs();
            return cnvZ3ExprToString(args[0]);
        }else {
            Expr[] args = expr.getArgs();
            return String.format("%s, %s", cnvZ3ExprToString(args[0]) , cnvZ3ExprToString(args[1]));
        }
    }
}