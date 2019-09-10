import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;

public class Instrument {

    //まだ計装できていない
    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        ClassPool cp = ClassPool.getDefault();
        cp.importPackage("java.util.concurrent.ExecutorService");
        CtClass p1 = cp.get("java.util.concurrent.ExecutorService");
        CtMethod m_sub = p1.getDeclaredMethod("submit");
        m_sub.setModifiers(Modifier.PUBLIC);


        CtMethod oldM = p1.getDeclaredMethod("submit");
        CtMethod newM = CtNewMethod.make("public void mysubmit(Runnable Task){System.out.println(\"submit task\"); submit(Task);}", p1);
        p1.addMethod(newM);
        p1.writeFile();

        CtClass main = cp.get("Main");
        CtMethod m_main = main.getDeclaredMethod("main");
        m_main.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClass().equals("ExecutorService") && m.getClassName().equals("submit"))
                    m.replace("$0.mysubmit");
            }
        });
        main.writeFile();
    }
}
