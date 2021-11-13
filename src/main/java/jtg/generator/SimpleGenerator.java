package jtg.generator;

import jtg.exception.ConstraintSolveException;
import jtg.graphics.SootCFG;
import jtg.solver.Z3Solver;
import jtg.visualizer.Visualizer;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleGenerator {

    private String clsPath;
    private String clsName;
    private String mtdName;
    private UnitGraph ug;
    private Body body;


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


    private List<Local> getJVars() {
        //Jimple自身增加的Locals，不是被测代码真正的变量
        ArrayList<Local> jimpleVars = new ArrayList<Local>();
        for (Local l : body.getLocals()) {
            if (l.toString().startsWith("$")) jimpleVars.add(l);
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

        List<Unit> path = null;
        ArrayList<String> testSet = null;
        String pathConstraint = "";

        System.out.println("============================================================================");
        System.out.println("Generating test case inputs for method: " + clsName + "." + mtdName + "()");
        System.out.println("============================================================================");
        try {
            testSet = new ArrayList<String>();
            for (Unit h : ug.getHeads())
                for (Unit t : ug.getTails()) {
                    path = ug.getExtendedBasicBlockPathBetween(h, t);
                    System.out.println("The path is: " + path.toString());

                    pathConstraint = calPathConstraint(path);
                    //如果路径约束为空字符串，表示路径约束为恒真
                    if (pathConstraint.isEmpty()) testSet.add(randomTC(body.getParameterLocals()));
                    System.out.println("The corresponding path constraint is: " + pathConstraint);
                    if (!pathConstraint.isEmpty())
                        testSet.add(solve(pathConstraint));
                }
        } catch (Exception e) {
            System.err.println("Error in generating test cases: ");
            System.err.println(e.toString());
        }
        if (!testSet.isEmpty()) {
            System.out.println("");
            System.out.println("The generated test case inputs:");
            int count = 1;
            for (String tc : testSet) {
                System.out.println("( " + count + " ) " + tc.toString());
                count++;
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

            if (stmt instanceof JAssignStmt) {
                assignList.put(((JAssignStmt) stmt).getLeftOp().toString(), ((JAssignStmt) stmt).getRightOp().toString());
                continue;
            }
            if (stmt instanceof JIfStmt) {

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
            if (stmt instanceof JReturnStmt) {
                expectedResult = stmt.toString().replace("return", "").trim();
            }
        }
        System.out.println("The step conditions with JimpleVars are: " + stepConditionsWithJimpleVars);

        //bug 没有考虑jVars为空的情况
        if (jVars.size() != 0) {
            for (String cond : stepConditionsWithJimpleVars) {
                //替换条件里的Jimple变量
                for (Local lv : jVars) {
                    if (cond.contains(lv.toString())) {
                        stepConditions.add(cond.replace(lv.toString(), assignList.get(lv.toString()).trim()));
                    }
                }
            }
        } else
            stepConditions = stepConditionsWithJimpleVars;

        if (stepConditions.isEmpty())
            return "";
        pathConstraint = stepConditions.get(0);
        int i = 1;
        while (i < stepConditions.size()) {
            pathConstraint = pathConstraint + " && " + stepConditions.get(i);
            i++;
        }
        //System.out.println("The path expression is: " + pathConstraint);
        return pathConstraint;
    }

    public String solve(String pathConstraint) throws Exception {
        return Z3Solver.solve(pathConstraint);
    }

    public String randomTC(List<Local> parameters) {

        String varName;
        String varValue = "";
        String testinput = "";

        for (Local para : parameters) {
            varName = para.getName();
            if ("int".equals(para.getType().toString())) {
                varValue = String.valueOf((int)(Math.random() * 10));
            }
            if ("String".equals(para.getType().toString())) {
                varValue = "abc";
            }
            //其它的基本类型没写
            testinput = testinput + " " + varName + "=" + varValue;
        }
        return testinput;
    }

}
