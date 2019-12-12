import javassist.*;
import javassist.expr.*;

/**
 * Fork Join への計装コード挿入
 */
public class LogExprEditor extends ExprEditor {

    private ClassPool cpool;
    private CtClass currentCtClass;
    private CtMethod currentMethod;
    private String longName;
    private static int uid = 1;


    public LogExprEditor(CtClass c, ClassPool cpool) {
        currentCtClass = c;
        this.cpool = cpool;
    }

    // read write instrument
//    @Override
//    public void edit(FieldAccess f) {
//        try {
//            String className = f.getClassName();
//            String fieldName = f.getFieldName();
//            CtClass type = f.getField().getType();
//            int line = f.getLineNumber();
//
//
//            if (type.isPrimitive()) {
//                String access = f.isStatic() ? className + "." + fieldName : "\"+$0.hashCode()+\"" + fieldName;
//                String hash = "\"+(\"" + access + "\").hashCode()+\"";
//                if (f.isWriter()) {
//                    f.replace(LogCode.out("WRITE", hash, className + "." + fieldName, line) +
//                            "$proceed($$);"
//                    );
//                }
//
//                if (f.isReader()) {
//                    f.replace(LogCode.out("READ", hash, className + "." + fieldName, line) +
//                            "$_ = $proceed();"
//                    );
//                }
//            } else {
//                String access = f.isStatic() ? className + "." + fieldName : "$0." + fieldName;
//                String hash = "\"+java.util.Objects.hashCode(" + access + ")+\"";
//                if (f.isWriter()) {
//                    f.replace(
//                            LogCode.out("WRITE", hash, className + "." + fieldName, line) +
//                                    "$proceed($$);"
//                    );
//                }
//                if (f.isReader()) {
//                    f.replace(
//                            LogCode.out("READ", hash, className + "." + fieldName, line) +
//                                    "$_ = $proceed();"
//                    );
//                    }
//            }
//        } catch (NotFoundException | CannotCompileException e) {
//            e.printStackTrace();
//        }
//    }

    // parent fork join and Socket, Executor Service instrument
    @Override
    public void edit(MethodCall m) throws CannotCompileException {
        String methodName = m.getMethodName();
        String className = currentCtClass.getName();
        int line = m.getLineNumber();

        try {
            CtMethod mm = m.getMethod();
            longName = mm.getLongName();
            currentMethod = m.getMethod();

            // fork at parent
            if (longName.equals("java.lang.Thread.start()")) {
                //メソッドシグネチャを表す(詳しくはJavaByteCode参照)　()VはVoid型
                if (!m.getSignature().equals("()V")) return;
                String hash_tmp = "\"+((wrapper.WrapperThread)$0).getID()+\"";
                m.replace(
                        LogCode.out("FORK_PA", hash_tmp, className, line) +
                                "$_ = $proceed();"
                );
                System.out.println("\t[OK]Trace: start() at " + className);
                return;
            }

            // join at parent
            if (longName.equals("java.lang.Thread.join()")) {
                if (!m.getSignature().equals("()V")) return;
                String hash_tmp = "\"+((wrapper.WrapperThread)$0).getID()+\"";
                m.replace(
                        LogCode.out("JOIN_PA", hash_tmp, className, line) +
                                "$_ = $proceed();"
                );
                System.out.println("\t[OK]Trace: join() at " + className);
                return;
            }


            // RPC
//        if (methodName.startsWith("Rpc_")) {
//            String hash_send = "\"+$1.generateTraceLogUid()+\"";
//            String hash_recv = "\"+$_.getTraceLogUid()+\"";
//            m.replace
//                    (
//                            LogCode.out("SEND_RPC", hash_send, className, line) +
//                                    "$_ = $proceed($$);" +
//                                    LogCode.out("RECV_RPC", hash_recv, className, line)
//                    );
//            System.out.printf("\t[OK]Trace: %s at %s%n", methodName, className);
//            return;
//        }

            // Socket


            // socket send
            if (longName.contains("Stream.write")) {
                String hash_tmp = "\"+$1.TraceObjectID+\"";
                if (longName.contains("Object")) {
                    m.replace(
                            "$1.TraceObjectID = " + uid + ";" +
                                    LogCode.out("SEND_SO", hash_tmp, className, line) +
                                    "$proceed($$);"
                    );
                } else {
                    return;
                }
                uid++;
                System.out.printf("\t[OK]Trace: Socket %s at %s", methodName, className);

            }

            // socket recv
            if (longName.contains("Stream.read")) {
                String hash_tmp = "\"+$_.TraceObjectID+\"";
                if (longName.contains("Object")) {
                    m.replace(
                            "$_ = $proceed($$);" +
                                    LogCode.out("RECV_SO", hash_tmp, className, line)

                    );
                } else {
                    return;
                }
                System.out.println(String.format("\t[OK]Trace: Socket %s at %s", methodName, className));
            }
            // ExecutorService
            if (longName.contains("Executor.execute")) {
                if (m.getSignature().equals("()V"))
                    return;
				/*
				m.replace(
						"((benchmark.thread.EventHandler)$1).setEventhandlerid($0.hashCode());" +
                            "$_ = $proceed($$);"+
                            PocketLog.out("CREATE_EV",hash,className, line)
				);
				*/

                m.replace
                        (LogCode.out("FORK_PA", "\"+$1.hashCode()+\"", className, line) +
                                "$_ = $proceed($$);"
                        );

                System.out.println(String.format("\t[OK]Trace: ExecutorService at %s", className));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[ERROR]" + currentCtClass.getName() + ":" + currentMethod.getName());
            System.out.println(longName);
            System.exit(1);
        }


    }
}
