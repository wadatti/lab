import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;

public class Instrument {
    private static void InstrumentMain() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");

        mainMethod.insertBefore("MyLogger log = new MyLogger();\n" +
                "        log.setDaemon(true);\n" +
                "        log.start();");

        CtField field1 = new CtField(CtClass.intType, "wrapper_eventID", mainClass);
        field1.setModifiers(Modifier.STATIC);
        mainClass.addField(field1, "0");

        ExprEditor exprEditor = new ExprEditor();
        mainMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("invoke")) {
                    m.replace("{" +
                            "        MyLogger.writeLog(\"ThreadFork(\" + ((WrapperRecursiveTask) $1).taskName + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRecursiveTask) $1).taskID + \")\");\n" +
                            "        $_ = $proceed($$);\n" +
                            "        MyLogger.writeLog(\"ThreadJoin(\" + ((WrapperRecursiveTask) $1).taskName + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRecursiveTask) $1).taskID + \")\");\n" +
                            "        ((WrapperRecursiveTask) $1).taskID++;\n" +
                            "    }");
                }
            }
        });
        mainClass.writeFile();
    }


//    {
//        MyLogger.writeLog("ThreadFork(" + ((WrapperRecursiveTask) $1).taskName + "," + Thread.currentThread().getId() + "," + ((WrapperRecursiveTask) $1).taskID + ")\n");
//        $_ = $proceed($$);
//        MyLogger.writeLog("ThreadJoin(" + ((WrapperRecursiveTask) $1).taskName + "," + Thread.currentThread().getId() + "," + ((WrapperRecursiveTask) $1).taskID + ")\n");
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
                            "        MyLogger.writeLog(\"ThreadFork(\" + taskName + \",\" + Thread.currentThread().getId() + \",\" + taskID + \")\");\n" +
                            "        $_ = $proceed();\n" +
                            "        MyLogger.writeLog(\"ThreadJoin(\" + taskName + \",\" + Thread.currentThread().getId() + \",\" + taskID + \")\");\n" +
                            "    }");
                }
            }
        });
        computeMethod.insertBefore("begin();");
        computeMethod.insertAfter("end();");
        parallelSumClass.writeFile();
    }

//    {
//        MyLogger.writeLog("ThreadFork(" + taskName + "," + Thread.currentThread().getId() + "," + taskID + ")\n");
//        $_ = $proceed();
//        MyLogger.writeLog("ThreadJoin(" + taskName + "," + Thread.currentThread().getId() + "," + taskID + ")\n");
//    }


    public static void main(String[] args) throws IOException, CannotCompileException, NotFoundException {
        InstrumentMain();
        InstrumentParallelSum();
        System.out.println("instrument!!");
    }
}
