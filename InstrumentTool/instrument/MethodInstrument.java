package instrument;

import instrument.tool.LogCode;
import javassist.*;

/**
 * instrument for:
 * FORK/JOIN_CH: java.lang.Runnable.run()
 * LOCK/REL: synchronized method
 */
public class MethodInstrument {
    CtClass c;

    public MethodInstrument(CtClass c) {
        this.c = c;
    }

    public CtClass getC() {
        return c;
    }

    public void instrument() {
        try {
            for (CtMethod m : c.getDeclaredMethods()) {
                threadRunInst(m);
                SynMethodInst(m);
            }
        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
            System.out.println(c.getName());
            System.exit(1);
        }
    }

    // children fork join
    public void threadRunInst(CtMethod m) throws CannotCompileException, NotFoundException {
        if (m.getName().equals("run") && m.getSignature().equals("()V")) {
            int line = m.getMethodInfo().getLineNumber(0);
            String hash = "\"+this.hashCode()+\"";

            // instrument to extending java.lang.Thread(wrapper.ThreadWrapper) class and implements java.lang.Runnable
            if (m.getDeclaringClass().getSuperclass().getName().equals("wrapper.ThreadWrapper")) {
                m.insertBefore(LogCode.out("FORK_CH", hash, c.getName(), m.getName(), line));
                m.insertAfter(LogCode.out("JOIN_CH", hash, c.getName(), m.getName(), line));
                System.out.println(String.format("\t[OK]Trace: wrapper.ThreadWrapper.run() at %s%n", c.getName()));
                return;
            } else {
                for (CtClass interFace : m.getDeclaringClass().getInterfaces()) {
                    if (interFace.getName().equals("java.lang.Runnable")) {
                        m.insertBefore(LogCode.out("FORK_CH", hash, c.getName(), m.getName(), line));
                        m.insertAfter(LogCode.out("JOIN_CH", hash, c.getName(), m.getName(), line));
                        System.out.println(String.format("\t[OK]Trace: run() at %s%n", c.getName()));
                        return;
                    }
                }
            }
        }
    }

    // Synchronized Method
    public void SynMethodInst(CtMethod m) throws CannotCompileException {
        int line = m.getMethodInfo().getLineNumber(0);
        if (Modifier.isSynchronized(m.getModifiers())) {
            if (Modifier.isStatic(m.getModifiers())) return;
            m.addLocalVariable("synId", CtClass.intType);
            m.insertBefore("synId=TraceID.getID();" + LogCode.out("LOCK", "\"+synId+\"", c.getName(), m.getName(), line));
            m.insertAfter(LogCode.out("REL", "\"+synId+\"", c.getName(), m.getName(), line));
            System.out.println(String.format("\t[OK]Trace: sync method %s", m.getName()));
        }
    }

}
