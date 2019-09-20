import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;

public class Instrument {

    private static void instrumentAccount() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass accountClass = classPool.get("Account");

        CtMethod depositMethod = accountClass.getDeclaredMethod("deposit");
        depositMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("lock")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"Lock(\" + $0.getClass().getName() + \",\" + Thread.currentThread().getId() + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed($$);\n" +
                            "    }");
                } else if (m.getMethodName().equals("unlock")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"unLock(\" + $0.getClass().getName() + \",\" + Thread.currentThread().getId() + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed($$);\n" +
                            "    }");
                }
            }
        });

        CtMethod withdrawMethod = accountClass.getDeclaredMethod("withdraw");
        withdrawMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("lock")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"Lock(\" + $0.getClass().getName() + \",\" + Thread.currentThread().getId() + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed($$);\n" +
                            "    }");
                } else if (m.getMethodName().equals("unlock")) {
                    m.replace("{\n" +
                            "        java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", true);\n" +
                            "        fw.write(\"unLock(\" + $0.getClass().getName() + \",\" + Thread.currentThread().getId() + \")\\n\");\n" +
                            "        fw.close();\n" +
                            "        $_ = $proceed($$);\n" +
                            "    }");
                }
            }
        });

        accountClass.writeFile();
    }

//    {
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("Lock(" + $0.getClass().getName() + "," + Thread.currentThread().getId() + ")\n");
//        fw.close();
//        $_ = $proceed($$);
//    }
//    {
//        java.io.FileWriter fw = new java.io.FileWriter("log.txt", true);
//        fw.write("unLock(" + $0.getClass().getName() + "," + Thread.currentThread().getId() + ")\n");
//        fw.close();
//        $_ = $proceed($$);
//    }

    private static void instrumentMain() throws CannotCompileException, NotFoundException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");

        mainMethod.insertBefore("java.io.FileWriter fw = new java.io.FileWriter(\"log.txt\", false);\n" +
                "    fw.write(\"----------------(ClassName,ThreadID)\\n\");\n" +
                "    fw.close();");
        mainClass.writeFile();
    }


    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        instrumentAccount();
        instrumentMain();
        System.out.println("instrument!!");

    }
}
