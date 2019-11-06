import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;

public class LogInstrument {

    // fork @ 子スレッド
    public static void ThreadRunInst(CtClass c, CtMethod m) throws CannotCompileException {
        if (m.getName().equals("run") && m.getSignature().equals("()V")) {
            int line = m.getMethodInfo().getLineNumber(0);
            String hash = "\"+Thread.currentThread().hashCode()+\"";
            m.insertBefore(LogCode.out("FORK_CH", hash, c.getName(), line));
            m.insertAfter(LogCode.out("JOIN_CH", hash, c.getName(), line));
            System.out.println(String.format("\t[OK]Trace: run() at %s%n", c.getName()));
        }
    }

    // Synchronized Method
    public static void SynMethodInst(CtClass c, CtMethod m) throws CannotCompileException {
        int line = m.getMethodInfo().getLineNumber(0);
        if (Modifier.isSynchronized(m.getModifiers())) {
            if (Modifier.isStatic(m.getModifiers())) return; //TODO staticだったらタグどうする?
            String hash = "\"+this.hashCode()+\"";
            m.insertBefore(LogCode.out("LOCK", hash, c.getName(), line));
            m.insertAfter(LogCode.out("REL", hash, c.getName(), line));
            System.out.println(String.format("\t[OK]Trace: sync method %s", m.getName()));
        }
    }

    // RPC
    public static void RpcMethodInst(CtClass c, CtMethod m) throws CannotCompileException {
        int line = m.getMethodInfo().getLineNumber(0);
        if (!c.getName().equals("benchmark.rpc.Rpc")) return;
        if (m.getName().startsWith("Rpc_")) {
            String hash_send = "\"+$_.generateTraceLogUid()+\"";
            String hash_recv = "\"+$1.getTraceLogUid()+\"";
            m.insertBefore(
                    LogCode.out("BEGIN_RPC", hash_recv, c.getName(), line)
            );

            m.insertAfter(
                    LogCode.out("END_RPC", hash_send, c.getName(), line));
            System.out.println(String.format("\t[OK]Trace: Rpc method %s@%s%n", m.getName(), c.getName()));
        }
    }
}
