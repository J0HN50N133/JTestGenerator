package jtg.Utils;

import jtg.solver.Z3Solver;
import soot.Local;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PrimePath {
    private List<Unit> path;

    public PrimePath(){}

    public PrimePath(List<Unit> path){
        this.path = path;
    }

    @Override
    public String toString() {
       String re = "";
       for(Unit unit:path){
           re += unit.toString();
       }
       return re;
    }

    public String solve(){
        try {
            return Z3Solver.solve(calPathConstraint(path));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }



    public String calPathConstraint(List<Unit> path) {

        String pathConstraint = "";
        String expectedResult = "";

        HashMap<String, String> assignList = new HashMap<>();
        ArrayList<String> stepConditionsWithJimpleVars = new ArrayList<String>();
        ArrayList<String> stepConditions = new ArrayList<String>();

        for (Unit stmt : path) {

            if (stmt instanceof JAssignStmt) { //赋值语句
                assignList.put(((JAssignStmt) stmt).getLeftOp().toString(), ((JAssignStmt) stmt).getRightOp().toString());
                continue;
            }
            if (stmt instanceof JIfStmt) { //如果这个unit是if语句也就是控制语句

                String ifstms = ((JIfStmt) stmt).getCondition().toString();
                int nextUnitIndex = path.indexOf(stmt) + 1;
                Unit nextUnit = path.get(nextUnitIndex);

                //如果ifstmt的后继语句不是ifstmt中goto语句，说明ifstmt中的条件为假
                if (!((JIfStmt) stmt).getTarget().equals(nextUnit))
                    ifstms = "!( " + ifstms + " )";
                else
                    ifstms = "( " + ifstms + " )";
                stepConditionsWithJimpleVars.add(ifstms);
                continue;
            }
            if (stmt instanceof JReturnStmt) {//返回语句
                expectedResult = stmt.toString().replace("return", "").trim();
//
//                System.out.println("expectedResult is "+expectedResult);
//                expectResSet.add(expectedResult);
            }
        }
        stepConditions = stepConditionsWithJimpleVars;

        //bug 没有考虑jVars为空的情况
       //把所有生成的中间变量用参数来表示
        for (String cond : stepConditionsWithJimpleVars) {
            //替换条件里的Jimple变量
            String temp = "";//作为替换后的字符串
            if (cond.contains("$")) {
                String[] strArr = cond.split(" ");

                for(int i = 0; i<strArr.length; ++i){
                    String str = strArr[i];
                    if(str.startsWith("$")){
                        strArr[i] = assignList.get(str.trim());
                    }
                    temp += strArr[i] + " ";
                }
                stepConditions.add(temp);
//                stepConditions.add(cond.replace(lv.toString(), assignList.get(lv.toString()).trim()));
            }

        }


        if (stepConditions.isEmpty())
            return "";
        pathConstraint = stepConditions.get(0);
        int i = 1;
        while (i < stepConditions.size()) {
            pathConstraint = pathConstraint + " && " + stepConditions.get(i);
            i++;
        }
        System.out.println("The path expression is: " + pathConstraint);
        return pathConstraint;
    }


}
