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
//            if (c.getName().contains("IFileInputStream"))
//                iFileInputStreamInstrument();
//            if (c.getName().contains("ReduceTask"))
//                reduceTaskInstrument();
        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void iFileInputStreamInstrument() throws CannotCompileException {
        for (CtConstructor constructor : c.getDeclaredConstructors()) {
            constructor.insertBeforeBody("$2 -= 4L;");
        }
    }

    public void reduceTaskInstrument() throws CannotCompileException {
        for (CtMethod method : c.getDeclaredMethods()) {
            if (method.getName().contains("shuffleInMemory")) {
                method.insertAfter(
                        "int i = $_.data.length - 4; byte[] temporary = new byte[i];" +
                                "for(i=4;i<$_.data.length;i++){temporary[i-4]=$_.data[i];}" +
                                "$_.data = temporary;"
                );
            }
        }
    }

//    public static void do

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
            m.insertBefore(LogCode.out("LOCK", "0", c.getName(), m.getName(), line));
            m.insertAfter(LogCode.out("REL", "0", c.getName(), m.getName(), line));
            System.out.println(String.format("\t[OK]Trace: sync method %s", m.getName()));
        }
    }

}
