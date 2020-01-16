package instrument;

import instrument.tool.LogCode;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.Arrays;

/**
 * instrument for:
 * Method Call: all method call
 */
public class NaiveInstrument extends ExprEditor {
    CtClass c;
    ClassPool cp;

    public NaiveInstrument(CtClass c, ClassPool cp) {
        this.c = c;
        this.cp = cp;
    }

    public CtClass getC() {
        return c;
    }

    public void instrumnet() {
        instrumentAll();
//        instrumentRPCMethodCall();
    }

    private void instrumentAll() {
        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) return;
        try {
            for (CtMethod m : c.getDeclaredMethods()) {
                m.insertBefore(LogCode.out("Method Call Begin ", "-", c.getName(), m.getName(), m.getMethodInfo().getLineNumber(0)));
                m.insertAfter(LogCode.out("Method Call End ", "-", c.getName(), m.getName(), m.getMethodInfo().getLineNumber(0)));
                System.out.println("\t[OK]Trace: Method Call " + m.getName() + " at " + c.getName());
            }
        } catch (CannotCompileException e) {
            e.printStackTrace();
            System.err.println(c.getName());
            System.exit(1);
        }
    }

    private void instrumentRPCMethodCall() {

        try {
            c.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    try {
                        CtMethod[] rpcMethods = cp.get("org.apache.hadoop.ipc.RPC").getDeclaredMethods();
                        CtMethod method = m.getMethod();
                        String methodName = method.getName();
                        int line = m.getLineNumber();

                        if (Arrays.asList(rpcMethods).contains(method)) {
                            m.replace(LogCode.out("RPC class Method Call Begin ", "-", c.getName(), methodName, line) +
                                    "$_ = $proceed($$);" +
                                    LogCode.out("RPC class Method Call End ", "-", c.getName(), methodName, line)
                            );
                            System.out.println("\t[OK]Trace:RPC class Method Call " + methodName + " at " + c.getName());
                        }
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
        } catch (CannotCompileException e) {
            e.printStackTrace();
            System.err.println(c.getName());
            System.exit(1);
        }
    }


}
