package instrument;

import instrument.tool.LogCode;
import javassist.*;
import javassist.expr.*;

/**
 * instrument for:
 * WRITE/READ: FieldAccess
 * FORK_PA: java.lang.Thread.start(), Executor.execute(Runnable target)
 * JOIN_PA: java.lang.Thread.join()
 * LOCK/REL: synchronized block tag(SynBlockInstrument), Reentrant lock() unlock()
 * SOCKET(SEND/RECEIVE): Only Object
 */
public class LogExprEditor extends ExprEditor {

    private ClassPool cpool;
    private CtClass currentCtClass;
    private CtMethod currentMethod;
    private String longName;


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
//            String hash = "0";
//
//
//            if (f.isWriter()) {
//                f.replace(LogCode.out("WRITE", hash, className + "." + fieldName, line) +
//                        "$proceed($$);"
//                );
//            }
//            if (f.isReader()) {
//                f.replace(LogCode.out("READ", hash, className + "." + fieldName, line) +
//                        "$_ = $proceed();"
//                );
//            }
//        } catch (NotFoundException | CannotCompileException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }

    // parent fork join and Socket, Executor Service instrument
    @Override
    public void edit(MethodCall m) {
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
                String hash_tmp = "\"+((wrapper.ThreadWrapper)$0).getID()+\"";
                m.replace(
                        LogCode.out("FORK_PA", hash_tmp, className, line) +
                                "$_ = $proceed();"
                );
                System.out.println("\t[OK]Trace: java.lang.Thread.start() at " + className);
                return;
            }

            // join at parent
            if (longName.equals("java.lang.Thread.join()")) {
                if (!m.getSignature().equals("()V")) return;
                String hash_tmp = "\"+((wrapper.ThreadWrapper)$0).getID()+\"";
                m.replace(
                        LogCode.out("JOIN_PA", hash_tmp, className, line) +
                                "$_ = $proceed();"
                );
                System.out.println("\t[OK]Trace: java.lang.Thread.join() at " + className);
                return;
            }

            // synchronized block
            if (m.getClassName().equals("wrapper.SyncBlock") && m.getMethodName().equals("begin")) {
                String hash_tmp = "\"+$1.hashCode()+\"";
                m.replace(LogCode.out("LOCK", hash_tmp, className, line));
                System.out.println("\t[OK]Trace: synchronized Block start at " + className);
                return;
            }

            if (m.getClassName().equals("wrapper.SyncBlock") && m.getMethodName().equals("end")) {
                String hash_tmp = "\"+$1.hashCode()+\"";
                m.replace(LogCode.out("REL", hash_tmp, className, line));
                System.out.println("\t[OK]Trace: synchronized Block start at " + className);
                return;
            }

            // Socket
            // socket send
            if (longName.contains("Stream.write")) {
                String hash_tmp = "\"+$1.TraceObjectID+\"";
                if (longName.contains("Object")) {
                    m.replace(
                            "$1.TraceObjectID = wrapper.TraceID.getID;" +
                                    LogCode.out("SEND_SO", hash_tmp, className, line) +
                                    "$proceed($$);"
                    );
                } else {
                    return;
                }
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

            // lock / unlock
            if (longName.contains("java.util.concurrent.locks.ReentrantLock.lock()")) {
                m.replace
                        (LogCode.out("LOCK", "\"+$0.hashCode()+\"", className, line) +
                                "$_ = $proceed($$);"
                        );
                System.out.println(String.format("\t[OK]Trace: ReentrantLock.lock() at %s", className));
            }
            if (longName.contains("java.util.concurrent.locks.ReentrantLock.unlock()")) {
                m.replace
                        (LogCode.out("REL", "\"+$0.hashCode()+\"", className, line) +
                                "$_ = $proceed($$);"
                        );
                System.out.println(String.format("\t[OK]Trace: ReentrantLock.unlock() at %s", className));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[ERROR]" + currentCtClass.getName() + ":" + currentMethod.getName());
            System.out.println(longName);
            System.exit(1);
        }


    }
}
