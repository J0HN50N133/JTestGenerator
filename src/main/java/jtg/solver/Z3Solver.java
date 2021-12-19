package jtg.solver;

import com.microsoft.z3.*;
import com.microsoft.z3.Context;
import jdk.nashorn.internal.runtime.regexp.joni.constants.StringType;
import soot.*;
import soot.jimple.BinopExpr;
import soot.jimple.UnopExpr;
import soot.jimple.internal.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Z3Solver {

    private static Value replaceValue(Value value, List<JAssignStmt> assignList) {
        //替换掉局部变量
        Value thisValue = (Value) value.clone();
        for (JAssignStmt jAssignStmt : assignList) {
            if(thisValue instanceof BinopExpr){
                //thisValue.toString().contains(jAssignStmt.getLeftOp().toString());
                //todo:ljx
                BinopExpr binopExpr = (BinopExpr) thisValue.clone();
                if(binopExpr.getOp1().equals(jAssignStmt.getLeftOp())){
                    binopExpr.setOp1(jAssignStmt.getRightOp());
                }
                if(binopExpr.getOp2().equals(jAssignStmt.getLeftOp())){
                    binopExpr.setOp2(jAssignStmt.getRightOp());
                }
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
    private static Sort getSortType(Type type, Context ctx){
        if(type instanceof ByteType || type instanceof IntType
                || type instanceof LongType || type instanceof ShortType)
            return ctx.mkIntSort();
        else if(type instanceof CharType || type instanceof StringType)
            return ctx.mkStringSort();
        else if(type instanceof DoubleType || type instanceof FloatType)
            return ctx.mkFPSortDouble();
        else
            return null;
    }
    private static Expr parseJimp2Z3(Value value, Context ctx){
        if(value instanceof BinopExpr){
            Value left = ((BinopExpr) value).getOp1();
            Value right = ((BinopExpr) value).getOp2();
            if(value instanceof JAddExpr){
                return ctx.mkAdd(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JSubExpr){
                return ctx.mkSub(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JMulExpr){
                return ctx.mkMul(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JDivExpr){
                return ctx.mkDiv(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JLeExpr){
                return ctx.mkLe(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JLtExpr){
                return ctx.mkLt(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JGeExpr){
                return ctx.mkGe(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JGtExpr){
                return ctx.mkGt(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JEqExpr){
                return ctx.mkEq(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
            else if(value instanceof JNeExpr){
                return ctx.mkNot(ctx.mkEq(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx)));
            }
            else if(value instanceof JAndExpr){
                return ctx.mkAnd(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            } else if(value instanceof JOrExpr){
                return ctx.mkOr(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            } else if(value instanceof JXorExpr){
                return ctx.mkXor(parseJimp2Z3(left, ctx), parseJimp2Z3(right, ctx));
            }
        }
        else if(value instanceof UnopExpr){
            Value value1 = ((UnopExpr) value).getOp();
            if(value instanceof JNegExpr){
                return ctx.mkNot(parseJimp2Z3(value1, ctx));
            }
            else if(value instanceof JLengthExpr){
                return ctx.mkLength(parseJimp2Z3(value1, ctx));
            }
        }
        else if(value instanceof JimpleLocal){
            //do: 根据JimpleLocal的type选择用ctx来make什么基本变量
            Type type = value.getType();
            if(type instanceof ArrayType){
                Type elementType = ((ArrayType) type).getElementType();
                return ctx.mkConst(value.toString(),
                        ctx.mkArraySort(ctx.getIntSort(), getSortType(elementType, ctx)));
            }
            else {
                return ctx.mkConst(value.toString(), getSortType(type, ctx));
            }
        }
        return null;
    }

    public static BoolExpr parseIf(JIfStmt stmt, Context ctx, List<JAssignStmt> asignList){
        Value value = stmt.getConditionBox().getValue();
        if(value instanceof BinopExpr){
            BinopExpr binopExpr = (BinopExpr) value;
            Value leftValue = binopExpr.getOp1();
            Value rightValue = binopExpr.getOp2();
            leftValue = replaceValue(leftValue, asignList);
            rightValue = replaceValue(rightValue, asignList);
            ((BinopExpr) value).setOp1(leftValue);
            ((BinopExpr) value).setOp2(rightValue);
        }
        else if(value instanceof UnopExpr){
            UnopExpr unopExpr = (UnopExpr) value;
            ((UnopExpr) value).setOp(replaceValue(unopExpr.getOp(), asignList));
        }

        return null;
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