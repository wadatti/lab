import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class Instrument {

    private static void instrumentMain() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");
        mainMethod.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", false);\n" +
                "    fw.write(\"------(eventKind,ThreadID,eventID)\\n\");\n" +
                "    fw.close();");
        mainMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("execute")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"ThreadPoolExecute(\" + $1.getClass().getName() + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRunnable) $1).eventID + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed($$);\n" +
                            "    }");
                }
            }
        });
        mainClass.writeFile();
    }

//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", false);
//    fw.write("------(eventKind,ThreadID,eventID)\n");
//    fw.close();

//    {
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("ThreadPoolExecute(" + $1.getClass().getName() + "," + Thread.currentThread().getId() + "," + ((WrapperRunnable) $1).eventID + ")\n");
//        fw.close();
//        $_ = $proceed($$);
//    }

    private static void instrumentTask() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass taskClass = classPool.get("Task");
        CtClass wrapperClass = classPool.get("WrapperRunnable");
        taskClass.setSuperclass(wrapperClass);

        CtMethod runMethod = taskClass.getDeclaredMethod("run");
        runMethod.insertBefore("begin();");
        runMethod.insertAfter("end();");
        taskClass.writeFile();
    }


    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        instrumentMain();
        instrumentTask();
        System.out.println("instrument!!");
    }
}
