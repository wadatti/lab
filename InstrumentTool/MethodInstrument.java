import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;

/**
 * 各クラスのメソッドへの計装コード挿入
 */
public class MethodInstrument {
    CtClass c;

    public MethodInstrument(CtClass c) {
        this.c = c;
    }

    public CtClass getC() {
        return c;
    }

    public void instrumnet() {
        try {
            for (CtMethod m : c.getDeclaredMethods()) {
                ThreadRunInst(c, m);
                SynMethodInst(c, m);
            }
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    // fork @ 子スレッド
    public static void ThreadRunInst(CtClass c, CtMethod m) throws CannotCompileException {
        if (m.getName().equals("run") && m.getSignature().equals("()V")) {
            int line = m.getMethodInfo().getLineNumber(0);

            //上だとThread thread = new Thread(new Task()); thread.start();との整合性が取れる
            //下だとExecutorServiceのexecuteとのHashで整合性が取れる
            String hash = "\"+Thread.currentThread().hashCode()+\"";
//            String hash = "\"+this.hashCode()+\"";

            m.insertBefore(LogCode.out("FORK_CH", hash, c.getName(), line));
            m.insertAfter(LogCode.out("JOIN_CH", hash, c.getName(), line));
            System.out.println(String.format("\t[OK]Trace: run() at %s%n", c.getName()));
        }
    }

    // Synchronized Method
    public static void SynMethodInst(CtClass c, CtMethod m) throws CannotCompileException {
        int line = m.getMethodInfo().getLineNumber(0);
        if (Modifier.isSynchronized(m.getModifiers())) {
            if (Modifier.isStatic(m.getModifiers())) return;
            String hash = "\"+this.hashCode()+\"";
            m.insertBefore(LogCode.out("LOCK", hash, c.getName(), line));
            m.insertAfter(LogCode.out("REL", hash, c.getName(), line));
            System.out.println(String.format("\t[OK]Trace: sync method %s", m.getName()));
        }
    }

}
