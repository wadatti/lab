import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class Instrument {

    private static void InstrumentMyThreadPool() throws CannotCompileException, NotFoundException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass myThreadPoolClass = classPool.get("MyThreadPool");
        CtClass param[] = {CtClass.intType};

        CtMethod method = myThreadPoolClass.getDeclaredMethod("execute");
        method.insertBefore("MyLogger.writeLog(\"createEvent(\" + ((WrapperRunnable)$1).eventKind + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRunnable)$1).eventID + \")\");");

        myThreadPoolClass.writeFile();


    }


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

        mainMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("MyThreadPool") && m.getMethodName().equals("execute")) {
                    m.replace("{\n" +
                            "        Task task = new Task(i);\n" +
                            "        task.eventKind = \"Task\";\n" +
                            "        task.eventID = wrapper_eventID;\n" +
                            "        executor.execute(task);\n" +
                            "        wrapper_eventID += 1;\n" +
                            "    }");
                }
            }
        });
        mainClass.writeFile();
    }

    private static void runnableInstrument() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        File file = new File("./src");
        File[] files = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean flag = true;
                if (name.equals(".DS_Store") || name.equals("WrapperRunnable.java") || name.equals("Instrument.java") || name.equals("DispatchTask.java"))
                    flag = false;
                return flag;
            }
        });
        for (File classFile : files) {
            CtClass cc = classPool.get(classFile.getName().substring(0, classFile.getName().lastIndexOf(".")));
            CtClass[] interfaces = cc.getInterfaces();
            for (CtClass tmp : interfaces) {
                if (tmp.getName().equals("java.lang.Runnable")) {
                    CtClass wrapperClass = classPool.get("WrapperRunnable");
                    cc.setSuperclass(wrapperClass);
                    System.out.println("wralled:" + cc.getName());
                    CtMethod runMethod = cc.getDeclaredMethod("run");
                    runMethod.insertBefore(" MyLogger.writeLog(\"BeginEvent(\" + this.eventKind +\",\" + Thread.currentThread().getId() +\",\" + this.eventID +\")\");");
                    cc.writeFile();
                }
            }
        }
    }


//    MyThreadPool.execute replace
//    {
//        Task task = new Task(i);
//        task.eventKind = "Task";
//        task.eventID = wrapper_eventID;
//        executor.execute(task);
//        wrapper_eventID += 1;
//    }

    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        runnableInstrument();
        InstrumentMyThreadPool();
        InstrumentMain();
        System.out.println("instrument!!");
    }
}