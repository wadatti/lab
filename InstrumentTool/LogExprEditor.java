import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class LogExprEditor extends ExprEditor {

    private ClassPool cpool;
    private CtClass currentCtClass;


    public LogExprEditor(CtClass c, ClassPool cpool) {
        currentCtClass = c;
        this.cpool = cpool;
    }

    @Override
    public void edit(MethodCall m) throws CannotCompileException {
        String methodName = m.getMethodName();
        String className = currentCtClass.getName();
        int line = m.getLineNumber();

        // fork @ 親スレッド
        if (methodName.equals("start")) {
            //メソッドシグネチャを表す(詳しくはJavaByteCode参照)　()VはVoid型
            if (!m.getSignature().equals("()V")) return;
            m.replace
                    (LogCode.out("FORK_PA", "\"+$0.hashCode()+\"", className, line) +
                            "$_ = $proceed();"
                    );
            return;
        }

        // join @ 親スレッド
        if (methodName.equals("join")) {
            if (!m.getSignature().equals("()V")) return;
            m.replace
                    (LogCode.out("JOIN_PA", "\"+$0.hashCode()+\"", className, line) +
                            "$_ = $proceed();"
                    );
            System.out.println("\t[OK]Trace: join() at " + className);
            return;
        }


        // RPC
        if (methodName.startsWith("Rpc_")) {
            String hash_send = "\"+$1.generateTraceLogUid()+\"";
            String hash_recv = "\"+$_.getTraceLogUid()+\"";
            m.replace
                    (
                            LogCode.out("SEND_RPC", hash_send, className, line) +
                                    "$_ = $proceed($$);" +
                                    LogCode.out("RECV_RPC", hash_recv, className, line)
                    );
            System.out.printf("\t[OK]Trace: %s at %s%n", methodName, className);
            return;
        }

        // Socket
        try {
            CtMethod mm = m.getMethod();
            String longName = mm.getLongName();


            // socket send
            if (longName.contains("Stream.write")) {
                String hash_tmp = "\"+$1.hashCode()+\"";
                ;
                m.replace(
                        LogCode.out("SEND_SO", hash_tmp, className, line) +
                                "$proceed($$);"
                );
                System.out.printf("\t[OK]Trace: Socket %s at %s", methodName, className);

            }

            // socket recv
            if (longName.contains("Stream.read")) {
                String hash_tmp = "\"+$1.hashCode()+\"";
                m.replace(
                        "$_ = $proceed($$);" +
                                LogCode.out("RECV_SO", hash_tmp, className, line)
                );
                System.out.println(String.format("\t[OK]Trace: Socket %s at %s", methodName, className));
            }


            // EventHandler
            if (longName.contains("Executor.execute")) {
                String hash = "\"+$0.hashCode()+\"";
				/*
				m.replace(
						"((benchmark.thread.EventHandler)$1).setEventhandlerid($0.hashCode());" +
                            "$_ = $proceed($$);"+
                            PocketLog.out("CREATE_EV",hash,className, line)
				);
				*/

                System.out.println(String.format("\t[OK]Trace: ExecutorService at %s", className));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
