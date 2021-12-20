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

    private static Value replaceValue(Value value, List<JAssignStmt> assignList) {
        //替换掉局部变量
        Value thisValue = (Value) value.clone();
        for (JAssignStmt jAssignStmt : assignList) {
            if(thisValue instanceof BinopExpr){
                //thisValue.toString().contains(jAssignStmt.getLeftOp().toString());
                //todo:ljx
                BinopExpr binopExpr = (BinopExpr) thisValue.clone();
                JAddExpr binopExpr1 = (JAddExpr) binopExpr;
            }
            else if(thisValue instanceof UnopExpr){
                UnopExpr unopExpr = (UnopExpr) thisValue.clone();
                if(unopExpr.getOp().equals(jAssignStmt.getLeftOp())){
                    unopExpr.setOp(jAssignStmt.getRightOp());
                }
            }
        }
        return thisValue;
    }

     static Sort getSortType(Type type, Context ctx){
        if(type instanceof ByteType || type instanceof IntType
                || type instanceof LongType || type instanceof ShortType)
            return ctx.mkIntSort();
        else if(type instanceof CharType)
            return ctx.mkStringSort();
        else if(type instanceof DoubleType || type instanceof FloatType)
            return ctx.mkFPSortDouble();
        else
            return null;
    }

    static Expr convConst(Context ctx, Value value) {
        assert (value instanceof Constant);
        if (value instanceof IntConstant) {
            return ctx.mkInt(((IntConstant) value).value);
        }
        return null;
    }
    public static void calculatePathConstraint(Path path){
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        Context ctx = new Context(cfg);
        Map<Value, Value> valueMap = new HashMap<>();
        Map<Value, Expr> valueExprMap = new HashMap<>();
        Set<Expr> constraints = new HashSet<>();
        List<Unit> units = path.getUnits();
        for (Unit unit : units) {
            if (unit instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) unit;
                Value left = stmt.getLeftOp();
                Value right = stmt.getRightOp();
//                valueMap.put(stmt.getLeftOp(), right);
                if (right instanceof BinopExpr) {
                    Value op1 = ((BinopExpr) right).getOp1();
                    Value op2 = ((BinopExpr) right).getOp2();
                    if (right instanceof JAddExpr) {
                        valueExprMap.put(left, ctx.mkAdd(valueExprMap.get(op1), valueExprMap.get(op2)));
                    }
                } else if (right instanceof UnopExpr) {
                } else if (right instanceof Constant) {
                }
            } else if (unit instanceof JIdentityStmt) {
                JIdentityStmt stmt = (JIdentityStmt) unit;
                Type type = stmt.getLeftOp().getType();
                Sort sort = getSortType(type, ctx);
                if (sort == null) continue;
                // TODO: support Array
                valueExprMap.put(stmt.getLeftOp(), ctx.mkConst(stmt.getLeftOp().toString() ,sort));
            } else if (unit instanceof JIfStmt) {
                cnvJIfStmt((JIfStmt) unit, ctx, constraints, valueExprMap, units);
            }
        }
        System.out.println("");
    }


    private static Expr parseJimp2Z3(Value value, Context ctx, Map<Value, Expr> valueExprMap){

        if(value instanceof BinopExpr){
            Value op1 = ((BinopExpr) value).getOp1();
            Value op2 = ((BinopExpr) value).getOp2();
            Expr expr1 = (op1 instanceof Constant)?convConst(ctx, op1):valueExprMap.get(op1);
            Expr expr2 = (op2 instanceof Constant)?convConst(ctx, op2):valueExprMap.get(op2);

            Value left = ((BinopExpr) value).getOp1();
            Value right = ((BinopExpr) value).getOp2();
            if(value instanceof JAddExpr){
                return ctx.mkAdd(expr1, expr2);
            }
            else if(value instanceof JSubExpr){
                return ctx.mkSub(expr1, expr2);
            }
            else if(value instanceof JMulExpr){
                return ctx.mkMul(expr1, expr2);
            }
            else if(value instanceof JDivExpr){
                return ctx.mkDiv(expr1, expr2);
            }
            else if(value instanceof JLeExpr){
                return ctx.mkLe(expr1, expr2);
            }
            else if(value instanceof JLtExpr){
                return ctx.mkLt(expr1, expr2);
            }
            else if(value instanceof JGeExpr){
                return ctx.mkGe(expr1, expr2);
            }
            else if(value instanceof JGtExpr){
                return ctx.mkGt(expr1, expr2);
            }
            else if(value instanceof JEqExpr){
                return ctx.mkEq(expr1, expr2);
            }
            else if(value instanceof JNeExpr){
                return ctx.mkNot(ctx.mkEq(expr1, expr2));
            }
            else if(value instanceof JAndExpr){
                return ctx.mkAnd(expr1, expr2);
            } else if(value instanceof JOrExpr){
                return ctx.mkOr(expr1, expr2);
            } else if(value instanceof JXorExpr){
                return ctx.mkXor(expr1, expr2);
            }
        }
        else if(value instanceof UnopExpr){
            Value op1 = ((UnopExpr) value).getOp();
            Expr expr1 = (op1 instanceof Constant)?convConst(ctx, op1):valueExprMap.get(op1);

            if(value instanceof JNegExpr){
                return ctx.mkNot(expr1);
            }
            else if(value instanceof JLengthExpr){
                return ctx.mkLength(expr1);
            }
        }
//        else if(value instanceof JimpleLocal){
//            //do: 根据JimpleLocal的type选择用ctx来make什么基本变量
//            Type type = value.getType();
//            if(type instanceof ArrayType){
//                Type elementType = ((ArrayType) type).getElementType();
//                return ctx.mkConst(value.toString(),
//                    ctx.mkArraySort(ctx.getIntSort(), getSortType(elementType, ctx)));
//            }
//            else {
//                return ctx.mkConst(value.toString(), getSortType(type, ctx));
//            }
//        }
        return null;
    }

    private static void cnvJIfStmt(JIfStmt unit, Context ctx, Set<Expr> constraints, Map<Value, Expr> valueExprMap, List<Unit> units) {
        JIfStmt stmt = (JIfStmt) unit;
        Value condition = stmt.getCondition();
        Expr condExpr = parseJimp2Z3(condition, ctx, valueExprMap);
        int i = units.indexOf(unit);
        if (!units.get(i+1).equals(((JIfStmt) unit).getTarget())){
            condExpr = ctx.mkNot(condExpr);
        }
        constraints.add(condExpr);
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

    Expr converValueToZ3Expr(Context ctx, Value v, List<Unit> assignList) {
        if (v instanceof soot.jimple.Expr) {
        }else {
            Type tp = v.getType();
            if (tp instanceof RefType) {
            } else if (tp instanceof ArrayType) {
            } else if (tp instanceof PrimType) {
            } else if (tp instanceof ByteType) {
            } else if (tp instanceof CharType) {
            } else if (tp instanceof DoubleType) {
            } else if (tp instanceof FloatType) {
            } else if (tp instanceof LongType) {
            } else if (tp instanceof ShortType) {
            } else if (tp instanceof BooleanType) {
            } else {
            }
        }
        return null;
    }
}