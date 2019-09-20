import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;

public class Instrument {
    private static void InstrumentMain() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");

        mainMethod.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", false);\n" +
                "    fw.write(\"----------------(taskName,ThreadID,taskID)\\n\");\n" +
                "    fw.close();");

        CtField field1 = new CtField(CtClass.intType, "wrapper_eventID", mainClass);
        field1.setModifiers(Modifier.STATIC);
        mainClass.addField(field1, "0");

        ExprEditor exprEditor = new ExprEditor();
        mainMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("invoke")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"ThreadFork(\" + ((WrapperRecursiveTask) $1).taskName + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRecursiveTask) $1).taskID + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed($$);\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"ThreadJoin(\" + ((WrapperRecursiveTask) $1).taskName + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRecursiveTask) $1).taskID + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        ((WrapperRecursiveTask) $1).taskID++;\n" +
                            "    }");
                }
            }
        });
        mainClass.writeFile();
    }

//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", false);
//    fw.write("----------------(taskName,ThreadID,taskID)\n");
//    fw.close();

//    {
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("ThreadFork(" + ((WrapperRecursiveTask) $1).taskName + "," + Thread.currentThread().getId() + "," + ((WrapperRecursiveTask) $1).taskID + ")\n");
//        fw.close();
//        $_ = $proceed($$);
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("ThreadJoin(" + ((WrapperRecursiveTask) $1).taskName + "," + Thread.currentThread().getId() + "," + ((WrapperRecursiveTask) $1).taskID + ")\n");
//        fw.close();
//        ((WrapperRecursiveTask) $1).taskID++;
//    }

    private static void InstrumentParallelSum() throws NotFoundException, IOException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass parallelSumClass = classPool.get("ParallelSum");

        CtClass wrapperClass = classPool.get("WrapperRecursiveTask");
        parallelSumClass.setSuperclass(wrapperClass);

        CtMethod computeMethod = parallelSumClass.getDeclaredMethod("compute");
        computeMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("join")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"ThreadFork(\" + taskName + \",\" + Thread.currentThread().getId() + \",\" + taskID + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed();\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"ThreadJoin(\" + taskName + \",\" + Thread.currentThread().getId() + \",\" + taskID + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "    }");
                }
            }
        });
        computeMethod.insertBefore("begin();");
        computeMethod.insertAfter("end();");
        parallelSumClass.writeFile();
    }

//    {
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("ThreadFork(" + taskName + "," + Thread.currentThread().getId() + "," + taskID + ")\n");
//        fw.close();
//        $_ = $proceed();
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("ThreadJoin(" + taskName + "," + Thread.currentThread().getId() + "," + taskID + ")\n");
//        fw.close();
//    }


    public static void main(String[] args) throws IOException, CannotCompileException, NotFoundException {
        InstrumentMain();
        InstrumentParallelSum();
        System.out.println("instrument!!");
    }
}
