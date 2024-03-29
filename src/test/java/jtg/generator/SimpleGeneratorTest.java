package jtg.generator;

import jtg.utils.Path;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleGeneratorTest {


    @Test
    void solo_if_correct() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "soloIf";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
     }

    @Test
    void if_else_correct() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "ifElse";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void multiple_if_correct() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "multipleIf";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }
    @Test
    void loop_test(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LoopTest";
        String methodName = "isPrime";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }
    @Test
    void sequence_correct() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "sequence";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<Path> paths = sg.calculateTestPath(sg.calculatePrimePath());
        for (Path path : paths) {
        }
//        List<String> ts = sg.generate();
//        assertTrue(!ts.isEmpty());

    }

    @Test
    void test_path_generate() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LoopTest";
        String methodName = "isPrime";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        Set<Path> primePaths = sg.calculatePrimePath();
        List<Path> paths = sg.calculateTestPath(primePaths);
        sg.generate();
//        System.out.println(paths.size()+"条测试路径");

    }
    @Test
    void test_cut_into_bb(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LoopTest";
        String methodName = "isPrime";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        int count = 0;
        for (BasicBlock basicBlock : sg.cutIntoBB()) {
            System.out.println("blocks " + count++ + ":");
            System.out.println(basicBlock);
        }
    }
}