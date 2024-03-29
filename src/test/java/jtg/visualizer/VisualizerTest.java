package jtg.visualizer;

import org.junit.jupiter.api.Test;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.Locale;

class VisualizerTest {

    @Test
    void if_else_cfgDot() {

        String sourceDirectory = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        System.out.println(sourceDirectory);

        String clsName = "cut.LogicStructure";
        String methodName = "ifElse";
        //setupSoot
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();

        // Retrieve method's body
        SootClass mainClass = Scene.v().getSootClass(clsName);
        SootMethod sm = mainClass.getMethodByName(methodName);
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();

        UnitGraph ug = new ClassicCompleteUnitGraph(sm.retrieveActiveBody());
        Visualizer.printCFGDot("ifElse_cfg_label_with_index.png",ug,true);
        Visualizer.printCFGDot("ifElse_cfg_label_with_SootCode.png",ug,false);
    }

    @Test
    void solo_if_cfgDot() {

        String sourceDirectory = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        System.out.println(sourceDirectory);

        String clsName = "cut.LogicStructure";
        String methodName = "soloIf";
        //setupSoot
        System.out.println(sourceDirectory);
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();

        // Retrieve method's body
        SootClass mainClass = Scene.v().getSootClass(clsName);
        // 使用签名来获取避免override冲突
        SootMethod sm = mainClass.getMethodByName(methodName);
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();

        UnitGraph ug = new ClassicCompleteUnitGraph(sm.retrieveActiveBody());
        Visualizer.printCFGDot("soloIf_cfg_label_with_index.dot",ug,true);
        Visualizer.printCFGDot("soloIf_cfg_label_with_SootCode.dot",ug,false);
    }

    @Test
    void multiple_if_cfgDot() {

        String sourceDirectory = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        System.out.println(sourceDirectory);

        String clsName = "cut.VariableType";
        String methodName = "generateFloat";
        //setupSoot
        System.out.println(sourceDirectory);
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();

        // Retrieve method's body
        SootClass mainClass = Scene.v().getSootClass(clsName);
        SootMethod sm = mainClass.getMethodByName(methodName);
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();

        UnitGraph ug = new ClassicCompleteUnitGraph(sm.retrieveActiveBody());
        Visualizer.printCFGDot(methodName.toLowerCase()+"_cfg_label_with_index.dot",ug,true);
        Visualizer.printCFGDot(methodName.toLowerCase()+"_cfg_label_with_SootCode.dot",ug,false);
    }
}