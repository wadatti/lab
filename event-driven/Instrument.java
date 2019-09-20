import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;

public class Instrument {
    private static void InstrumentTask() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass taskClass = classPool.get("Task");
        CtClass wrapperClass = classPool.get("WrapperRunnable");
        taskClass.setSuperclass(wrapperClass);
        CtMethod runMethod = taskClass.getDeclaredMethod("run");
        runMethod.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                "    fw.write(\"BeginEvent(\" + this.eventKind +\",\" + Thread.currentThread().getId() +\",\" + this.eventID +\")\\n\");\n" +
                "    fw.close();");
        taskClass.writeFile();
    }

//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//    fw.write("BeginEvent(" + this.eventKind +"," + java.lang.Thread().getID() +"," + this.eventID +")\n");
//    fw.close();

    private static void InstrumentMyThreadPool() throws CannotCompileException, NotFoundException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass myThreadPoolClass = classPool.get("MyThreadPool");
        CtClass param[] = {CtClass.intType};

        CtMethod method = myThreadPoolClass.getDeclaredMethod("execute");
        method.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                "        fw.write(\"createEvent(\" + ((WrapperRunnable)$1).eventKind + \",\" + Thread.currentThread().getId() + \",\" + ((WrapperRunnable)$1).eventID + \")\\n\");\n" +
                "        fw.close();");

        myThreadPoolClass.writeFile();
    }
//    MyThreadPool.execute insertbefore
//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//    fw.write("createEvent(" + ((WrapperRunnable)$1).eventKind + "," + Thread.currentThread().getId() + "," + ((WrapperRunnable)$1).eventID + ")\n");
//    fw.close();

    private static void InstrumentMain() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");

        mainMethod.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", false);\n" +
                "    fw.write(\"------Event(eventKind,ThreadID,eventID)\\n\");\n" +
                "    fw.close();");

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

//    Main insertbefore
//    java.io.FileWriter fw = new java.io.FileWriter("log.txt", false);
//    fw.write("------Event(eventKind,ThreadID,eventID)\n");
//    fw.close();


//    MyThreadPool.execute replace
//    {
//        Task task = new Task(i);
//        task.eventKind = "Task";
//        task.eventID = wrapper_eventID;
//        executor.execute(task);
//        wrapper_eventID += 1;
//    }

    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        InstrumentTask();
        InstrumentMyThreadPool();
        InstrumentMain();
        System.out.println("instrument!!");
    }
}