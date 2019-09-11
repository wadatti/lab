import javassist.*;

import java.io.IOException;

public class Instrument {
    private static void InstrumentTask() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass taskClass = classPool.get("Task");
        CtField field2 = new CtField(CtClass.intType, "threadID", taskClass);
        taskClass.addField(field2);
        taskClass.writeFile();

    }

    private static void InstrumentMyThreadPool() throws CannotCompileException, NotFoundException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass myThreadPoolClass = classPool.get("MyThreadPool");
        CtClass param[] = {CtClass.intType};

        CtField field1 = new CtField(CtClass.intType, "threadID", myThreadPoolClass);

        myThreadPoolClass.addField(field1);

        CtMethod method = myThreadPoolClass.getDeclaredMethod("execute");
        method.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", false);\n" +
                "        fw.write(\"execute log\\n\");\n" +
                "        fw.close();");

        myThreadPoolClass.writeFile();
    }

    public static void InstrumentMain() throws NotFoundException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
    }

    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        InstrumentTask();
        InstrumentMyThreadPool();
        InstrumentMain();
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", false);
//        fw.write("execute log\n");
//        fw.close();
    }
}
