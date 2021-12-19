package jtg.generator;

import jtg.utils.Path;
import jtg.graphics.SootCFG;
import jtg.solver.Z3Solver;
import jtg.visualizer.Visualizer;
import soot.Body;
import soot.Local;
import soot.Unit;
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
            Set<Path> primePaths = calculatePrimePath();
            List<Path> testPaths = calculateTestPath(primePaths);
            for (Path testPath : testPaths) {
                System.out.println("The path is: \n" + testPath);
                pathConstraint = testPath.calPathConstraint();
                //如果路径约束为空字符串，表示路径约束为恒真
                if (pathConstraint.isEmpty())
                    testSet.add(randomTC(body.getParameterLocals()));
                System.out.println("The corresponding path constraint is: " + pathConstraint);
                if (!pathConstraint.isEmpty()){
                    testSet.add(solve(pathConstraint));//!( i0 <= 0 )
                }

                expectResSet.add(testPath.getExpectRes());
            }
        } catch (Exception e) {
            System.err.println("Error in generating test cases: ");
            System.err.println(e);
        }
        if (!testSet.isEmpty()) {
            System.out.println();
            System.out.println("The generated test case inputs:");
            for (int count = 0; count < testSet.size(); ++count) {
                System.out.printf("(%d) %s, expected result: %s\n", count+1, testSet.get(count), expectResSet.get(count));
            }
        }
        return testSet;
    }

    public String solve(String pathConstraint) throws Exception {//根据路径生成测试用例
        return Z3Solver.solve(pathConstraint);
    }

    public String randomTC(List<Local> parameters) { //此时路径对于变量没有要求,可以随机赋值变量

        String varName;
        String varValue = "";
        StringBuilder testInput = new StringBuilder();

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
            testInput.append(" ").append(varName).append("=").append(varValue);
        }
        return testInput.toString();
    }

    public Set<Path> calculatePrimePath(){
        System.out.println(ug.getBody().toString());
        Set<Path> set = new LinkedHashSet<>();
        for (Unit unit : ug) {
            // 计算以unit为根的结点树
            dfs(unit,new ArrayList<>(), set);
        }
        return set;
    }

    public List<Path> calculateTestPath(Set<Path> primePaths){
        PriorityQueue<Path> pq = new PriorityQueue<>((o1, o2) -> o2.getUnits().size() - o1.getUnits().size());
        List<Path> testPaths = new LinkedList<>();
        pq.addAll(primePaths);
        while (!pq.isEmpty()) {
            loop: {
                Path path = pq.poll();
                String pathStr = path.toString();
                for (Path testPath : testPaths) {
                    // 如果已经被覆盖那么不需要拓展了
                    if (testPath.toString().contains(pathStr)) {
                        break loop;
                    }
                }
                List<Path> extendPaths = extendPath(path);
                testPaths.addAll(extendPaths);
            }
        }
        return testPaths;
    }

    private boolean isEntryNode(Unit unit){
        return ug.getHeads().contains(unit);
    }

    private boolean isEndNode(Unit unit){
        return ug.getTails().contains(unit);
    }

    private List<Path> extendTail(Path path){
        List<Unit> unitList = path.getUnits();
        List<Path> result = new LinkedList<>();
        Unit lastUnit = unitList.get(unitList.size() - 1);
        if (isEndNode(lastUnit)){
            result.add(path);
            return result;
        }else {
            List<Unit> succs = ug.getSuccsOf(lastUnit);
            if (succs.size() == 1) {
                // 单节点没办法，直接加
                Path newPath = new Path(path);
                newPath.appendUnit(succs.get(0));
                result.addAll(extendTail(newPath));
            }else {
                // 多结点的可能可以破环
                for (Unit unit : ug.getSuccsOf(lastUnit)) {
                    if (!unitList.contains(unit)){
                        Path newPath = new Path(path);
                        newPath.appendUnit(unit);
                        result.addAll(extendTail(newPath));
                    }
                }
            }
        }
        return result;
    }

    private List<Path> extendHead(Path path){
        List<Unit> unitList = path.getUnits();
        List<Path> result = new LinkedList<>();
        Unit firstUnit = unitList.get(0);
        if (isEntryNode(firstUnit)){
            result.add(path);
            return result;
        }else {
            for (Unit unit : ug.getPredsOf(firstUnit)) {
                if (!unitList.contains(unit)){
                    Path newPath = new Path(path);
                    newPath.addUnitInHead(unit);
                    result.addAll(extendHead(newPath));
                }
            }
        }
        return result;
    }
    private List<Path> extendPath(Path path) {
        List<Path> result = new LinkedList<>();
        List<Path> pathsExtendedTail = extendTail(path);
        for (Path pathWithTail : pathsExtendedTail) {
            result.addAll(extendHead(pathWithTail));
        }
        return result;
    }

    private void dfsDone(Unit unit,List<Unit> currentPath, Set<Path> set){
        List<Unit> primepathList = new ArrayList<>(currentPath);
        if(unit != null){
            primepathList.add(unit);
        }
        Path primePath = new Path(primepathList);
        if (set.contains(primePath)){
            return;
        }
        String s = primePath.toString();
        for (Path p: set){
            if (p.toString().contains(s)){
                return;
            }
        }
        set.add(primePath);
    }

    private void dfs(Unit unit,List<Unit> currentPath, Set<Path> set) {
        if (currentPath.isEmpty()){
            // 第一个结点
            currentPath.add(unit);
            for (Unit succ : ug.getSuccsOf(unit)) {
                dfs(succ, new LinkedList<>(currentPath), set);
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
                        dfs(succ, new LinkedList<>(currentPath), set);
                    }
                }
            }
        }
    }
}
