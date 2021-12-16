package jtg.generator;

import jtg.Utils.PrimePath;
import jtg.graphics.SootCFG;
import jtg.solver.Z3Solver;
import jtg.visualizer.Visualizer;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.*;

public class SimpleGenerator {

    private String clsPath;
    private String clsName;
    private String mtdName;
    private UnitGraph ug;
    private Body body;
    private ArrayList<String> expectResSet;


    public SimpleGenerator(String className, String methodName) {
        String defaultClsPath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "classes";
        new SimpleGenerator(defaultClsPath, className, methodName);
    }

    public SimpleGenerator(String classPath, String className, String methodName) {
        clsPath = classPath;
        clsName = className;
        mtdName = methodName;
        ug = SootCFG.getMethodCFG(clsPath, clsName, mtdName);
        body = SootCFG.getMethodBody(clsPath, clsName, mtdName);
    }

    public void drawCFG(String graphName, boolean indexLabel) {
        Visualizer.printCFGDot(graphName, ug, indexLabel);
    }


    private List<Local> getJVars() {//获得除了参数之外的其他的生成的变量
        //Jimple自身增加的Locals，不是被测代码真正的变量
        ArrayList<Local> jimpleVars = new ArrayList<Local>();
        for (Local l : body.getLocals()) {
            if (l.toString().startsWith("$")) {
                jimpleVars.add(l);
//                System.out.println("getJVars(): " + l.toString());
            }
        }
        return jimpleVars;
    }

    private List<Local> getParameter() {
        ArrayList<Local> paras = new ArrayList<Local>();
        for (Local para : body.getParameterLocals()) {
            paras.add(para);
        }
        return paras;
    }

    public List<String> generate() {

        ArrayList<String> testSet = null;
        expectResSet = null;
        String pathConstraint = "";

        System.out.println("============================================================================");
        System.out.println("Generating test case inputs for method: " + clsName + "." + mtdName + "()");
        System.out.println("============================================================================");
        try {
            expectResSet = new ArrayList<String>();
            testSet = new ArrayList<String>();
            Set<PrimePath> primePaths = calculatePrimePath();
            for (PrimePath primePath : primePaths) {
                System.out.println("The path is: \n" + primePath);
                pathConstraint = primePath.calPathConstraint();
                //如果路径约束为空字符串，表示路径约束为恒真
                if (pathConstraint.isEmpty())
                    testSet.add(randomTC(body.getParameterLocals()));
                System.out.println("The corresponding path constraint is: " + pathConstraint);
                if (!pathConstraint.isEmpty())
                    testSet.add(solve(pathConstraint));//!( i0 <= 0 )
                expectResSet.add(primePath.getExpectRes());
            }
        } catch (Exception e) {
            System.err.println("Error in generating test cases: ");
            System.err.println(e.toString());
        }
        if (!testSet.isEmpty()) {
            System.out.println("");
            System.out.println("The generated test case inputs:");
            for (int count = 0; count < testSet.size(); ++count) {
                System.out.printf("(%d) %s, expected result: %s\n", count+1, testSet.get(count), expectResSet.get(count));
            }
        }
        return testSet;
    }

    public String calPathConstraint(List<Unit> path) {

        List<Local> jVars = getJVars();

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

                System.out.println("expectedResult is "+expectedResult);
                expectResSet.add(expectedResult);
            }
        }
        System.out.println("The step conditions with JimpleVars are: " + stepConditionsWithJimpleVars);

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

    public String solve(String pathConstraint) throws Exception {//根据路径生成测试用例
        return Z3Solver.solve(pathConstraint);
    }

    public String randomTC(List<Local> parameters) { //此时路径对于变量没有要求,可以随机赋值变量

        String varName;
        String varValue = "";
        String testinput = "";

        for (Local para : parameters) {
            varName = para.getName();
            if ("int".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10));
            }
            if ("short".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10)%32768);
            }
            if ("long".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10));
            }
            if ("byte".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10)%128);
            }
            if ("java.lang.String".equals(para.getType().toString())) {//张智渊修复
                varValue = "abc";
            }
            if ("boolean".equals(para.getType().toString())) {
                varValue = "true";
            }
            if ("float".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10) / 3);
            }
            if ("double".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10) / 3);
            }
            //其它的基本类型没写
            testinput = testinput + " " + varName + "=" + varValue;
        }
        return testinput;
    }
    private List<Unit> getAllUnit(UnitGraph ug){
        List<Unit> units = new LinkedList<>();
        for (Unit unit : ug) {
            units.add(unit);
        }
        return units;
    }
    public Set<PrimePath> calculatePrimePath(){
        System.out.println(ug.getBody().toString());
        Set<PrimePath> set = new LinkedHashSet<>();
        for (Unit unit : ug) {
            // 计算以unit为根的结点树
            dfs(unit,new ArrayList<>(), set);
        }
        return set;
    }

    private void dfsDone(Unit unit,List<Unit> currentPath, Set<PrimePath> set){
        List<Unit> primepathList = new ArrayList<>(currentPath);
        if(unit != null){
            primepathList.add(unit);
        }
        PrimePath primePath = new PrimePath(primepathList);
        if (set.contains(primePath)){
            return;
        }
        String s = primePath.toString();
        for (PrimePath p: set){
            if (p.toString().contains(s)){
                return;
            }
        }
        set.add(primePath);
    }

    private void dfs(Unit unit,List<Unit> currentPath, Set<PrimePath> set) {
        if (currentPath.isEmpty()){
            // 第一个结点
            currentPath.add(unit);
            for (Unit succ : ug.getSuccsOf(unit)) {
                dfs(succ, currentPath, set);
            }
        }else{
            if(ug.getTails().contains(unit)){
                // unit is final node this branch done
                dfsDone(unit, currentPath, set);
            }else{
                if (currentPath.get(0).equals(unit)){
                    // head and tails same, done
                    dfsDone(unit, currentPath, set);
                }else if(currentPath.contains(unit)) {
                    dfsDone(null, currentPath, set);
                }else{
                    currentPath.add(unit);
                    for (Unit succ : ug.getSuccsOf(unit)) {
                        dfs(succ, currentPath, set);
                    }
                }
            }
        }
    }
}
