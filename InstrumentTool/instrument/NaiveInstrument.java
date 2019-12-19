package instrument;

import instrument.tool.LogCode;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * instrument for:
 * Method Call: all method call
 */
public class NaiveInstrument extends ExprEditor {
    private ClassPool cpool;
    private CtClass currentCtClass;
    private CtMethod currentMethod;
    private String longName;

    public NaiveInstrument(ClassPool cpool, CtClass currentCtClass) {
        this.cpool = cpool;
        this.currentCtClass = currentCtClass;
    }

    @Override
    public void edit(MethodCall m) {
        try {
            String methodName = m.getMethodName();
            String longMethodName = m.getMethod().getLongName();
            String className = currentCtClass.getName();
            int line = m.getLineNumber();

            m.replace(
                    LogCode.out("Method Call", "0", className, line) +
                            "$_ = $proceed($$);"
            );

            System.out.println("\t[OK]Trace: Method Call " + methodName + " at " + className);

        } catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
        }
    }
}
