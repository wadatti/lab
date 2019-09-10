import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class Test {
    public static void main(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = cp.get("Hello");
        CtMethod m = cc.getDeclaredMethod("main");
        m.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("Hello")
                        && m.getMethodName().equals("say"))
                    m.replace("$0.hi();");
            }
        });
        cc.writeFile();
    }
}