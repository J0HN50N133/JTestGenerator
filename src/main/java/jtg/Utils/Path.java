package jtg.Utils;

import jtg.solver.Z3Solver;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.graph.UnitGraph;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Path {
    // component of the path
    private final List<Unit> units;
    // the expectResult
    private String expectRes;
    // the UnitGraph this Path belong to
    private UnitGraph ug;

    public Path(Path src){
        this.units = new LinkedList<>(src.getUnits());
        this.expectRes = src.expectRes;
        this.ug = src.ug;
    }

    public Path(List<Unit> units){
        this.units = units;
    }

    public List<Unit> getUnits() {
        return units;
    }

    @Override
    public String toString() {
       StringBuilder sb = new StringBuilder();
       for(Unit unit: units){
           sb.append(unit.toString()).append("\n");
       }
       return sb.toString();
    }

    public String solve(){
        try {
            return Z3Solver.solve(calPathConstraint());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 把Jimple生成的变量递归替换回原有变量
    private static final Pattern p = Pattern.compile("\\$[a-z]+[0-9]+");
    private String washVariable(Map<String, String> assignList, String cond) {
        if (cond.contains("$")){
            StringBuilder afterWash = new StringBuilder();
            String[] strArr = cond.split(" ");
            for(int i = 0; i<strArr.length; ++i){
                String str = strArr[i];
                if(str.startsWith("$")){
                    strArr[i] = assignList.get(str.trim());
                }else if(str.contains("$")){
                    Matcher m = p.matcher(str);
                    String leftOp = "";
                    if(m.find()){
                        leftOp = m.group(0);
                    }
                    String replacement = assignList.get(leftOp).replaceAll("\\$", "RGX_CHAR_DOLLAR");// encode replacement;
                    str = str.replaceAll("\\$[a-z]+[0-9]+", replacement);
                    str = str.replaceAll("RGX_CHAR_DOLLAR", "\\$"); // decode replacement
                    strArr[i] = str;
                }
                afterWash.append(strArr[i]).append(" ");
            }
            return washVariable(assignList, afterWash.toString());
        }
        return cond;
    }

    public void collectPathConstraint(){
        List<Unit> pathConstraint = new LinkedList<>();
        List<Unit> assignList = new LinkedList<>();
        for (Unit unit : units) {
            if (unit instanceof JIfStmt) {
                JIfStmt ifStmt = (JIfStmt) unit;
                pathConstraint.add(unit);
            }
            if (unit instanceof JAssignStmt) {
                assignList.add(unit);
            }
        }
    }

    public String calPathConstraint() {

        String pathConstraint;

        HashMap<String, String> assignList = new HashMap<>();
        ArrayList<String> stepConditionsWithJimpleVars = new ArrayList<String>();
        ArrayList<String> stepConditions = new ArrayList<String>();

        for (Unit stmt : units) {

            if (stmt instanceof JAssignStmt) { //赋值语句
                assignList.put(((JAssignStmt) stmt).getLeftOp().toString(), ((JAssignStmt) stmt).getRightOp().toString());
                continue;
            }
            if (stmt instanceof JIfStmt) { //如果这个unit是if语句也就是控制语句

                String ifstms = ((JIfStmt) stmt).getCondition().toString();
                int nextUnitIndex = units.indexOf(stmt) + 1;
                Unit nextUnit = units.get(nextUnitIndex);

                //如果ifstmt的后继语句不是ifstmt中goto语句，说明ifstmt中的条件为假
                if (!((JIfStmt) stmt).getTarget().equals(nextUnit))
                    ifstms = "!( " + ifstms + " )";
                else
                    ifstms = "( " + ifstms + " )";
                stepConditionsWithJimpleVars.add(ifstms);
                continue;
            }
            if (stmt instanceof JReturnStmt){
                expectRes = washVariable(assignList, ((JReturnStmt) stmt).getOp().toString());
            }
        }

        //bug 没有考虑jVars为空的情况
       //把所有生成的中间变量用参数来表示
        for (String cond : stepConditionsWithJimpleVars) {
                stepConditions.add(washVariable(assignList, cond));
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


    public String getExpectRes() {
        return expectRes;
    }

    public void appendUnit(Unit unit) {
        units.add(unit);
    }

    public void addUnitInHead(Unit unit) {
        units.add(0, unit);
    }
}
