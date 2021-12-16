package jtg.generator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VariableTypeTest {

    @Test
    void generateInt() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateInt";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateShort() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateShort";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateLong() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateLong";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateChar() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateChar";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateFloat() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateFloat";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateDouble() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateDouble";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateBool() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateBool";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateString() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateString";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }

    @Test
    void generateArr() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.VariableType";
        String methodName = "generateArr";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
        assertTrue(!ts.isEmpty());
    }
}