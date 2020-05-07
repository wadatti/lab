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
    int line;


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
//            longName = type.getName();
//            line = f.getLineNumber();
////            String hash = "\"+" + fieldName + ".hashCode()+\"";
//
//
//            if (!f.isStatic() && !type.isPrimitive() && !type.isEnum()) {
//                if (f.isWriter()) {
//                    String hash = "\"+$0.hashCode()+\"";
//                    f.replace(LogCode.out("WRITE", hash, className, fieldName, line) +
//                            "$proceed($$);"
//                    );
//                }
//                if (f.isReader()) {
//                    String hash = "\"+$0.hashCode()+\"";
//                    f.replace(LogCode.out("READ", hash, className, fieldName, line) +
//                            "$_ = $proceed();"
//                    );
//                }
//            }
//        } catch (NotFoundException | CannotCompileException e) {
//            e.printStackTrace();
//            System.out.println(f.getClassName());
//            System.out.println(f.getFieldName());
//            System.out.println(line);
//            System.out.println(longName);
//            System.exit(1);
//        }
//    }

    // parent fork join and Socket, Executor Service instrument
    @Override
    public void edit(MethodCall m) {
        String methodName = m.getMethodName();
        String methodClassName = m.getClassName();
        String className = currentCtClass.getName();
        line = m.getLineNumber();

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
                        LogCode.out("FORK_PA", hash_tmp, className, methodName, line) +
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
                        LogCode.out("JOIN_PA", hash_tmp, className, methodName, line) +
                                "$_ = $proceed();"
                );
                System.out.println("\t[OK]Trace: java.lang.Thread.join() at " + className);
                return;
            }

            // synchronized block
            if (m.getClassName().equals("wrapper.SyncBlock") && m.getMethodName().equals("begin")) {
                String hash_tmp = "\"+wrapper.TraceID.getObjectId($1)+\"";
                m.replace("wrapper.TraceID.objectRegist($1);" +
                        LogCode.out("LOCK", hash_tmp, className, methodName, line));
                System.out.println("\t[OK]Trace: synchronized Block start at " + className);
                return;
            }

            if (m.getClassName().equals("wrapper.SyncBlock") && m.getMethodName().equals("end")) {
                String hash_tmp = "\"+wrapper.TraceID.getObjectId($1)+\"";
                m.replace(LogCode.out("REL", hash_tmp, className, methodName, line));
                System.out.println("\t[OK]Trace: synchronized Block start at " + className);
                return;
            }

            // Socket
            // socket write
            if (methodName.equals("write") && currentCtClass.getName().contains("MapOutputServlet")) {
//                m.replace(
//                        "int omegaID = TraceID.getID();" +
//                                "response.setHeader(\"OmegaIDHeader\",String.valueOf(omegaID));" +
//                                LogCode.out("SEND_SO", "\"+omegaID+\"", className, methodName, line) +
//                                "$_ = $proceed($$);"
//                );
                m.replace(
                        "byte[] metaID = java.nio.ByteBuffer.allocate(4).putInt(wrapper.TraceID.getID()).array();" +
                                "byte[] metaLength = java.nio.ByteBuffer.allocate(4).putInt($3).array();" +
                                LogCode.out("SEND_SO", "\"+java.nio.ByteBuffer.wrap(metaID).getInt()+\"", className, methodName, line) +
                                "$_ = $proceed(metaID,0,(int)metaID.length);" +
                                "$_ = $proceed(metaLength,0,(int)metaLength.length);" +
                                "$_ = $proceed($$);"
                );
            }

            // socket read
            if (methodName.equals("read") && currentCtClass.getName().contains("MapOutputCopier")) {
                if (m.where().getName().contains("shuffleToDisk"))
                    m.replace(
                            "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] call read_Disk $1:\"+$1+\"  $2:\"+$2+\"  $3:\"+$3);" +
                                    "byte[] metaID = new byte[4];" +
                                    "byte[] metaLength = new byte[4];" +
                                    "int metaRem = 4;" +
                                    "while(metaRem > 0){$_ = $proceed(metaID,4 - metaRem,metaRem); metaRem -= $_; if($_==-1) break;}" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] metaID_Disk $_:\"+$_);" +
                                    "metaRem = 4;" +
                                    "while(metaRem > 0){$_ = $proceed(metaLength,4 - metaRem,metaRem); metaRem -= $_; if($_==-1) break;}" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] metaLength_Disk $_:\"+$_);" +
                                    "$_ = 0;" +
                                    "int omegaLen = java.lang.Math.min(java.nio.ByteBuffer.wrap(metaLength).getInt(),$3);" +
                                    "int tempLen = omegaLen;" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] omegaLen_Disk :\"+omegaLen+\"  $3:\"+$3);" +
                                    "while(omegaLen > 0){" +
                                    "$_ = $proceed($1,$2,omegaLen);" +
                                    "if($_==-1) break;" +
                                    "$2 += $_;" +
                                    "omegaLen -= $_;" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] call read_Disk $1:\"+$1+\"  $2:\"+$2+\"  omegaLen:\"+omegaLen);" +
                                    LogCode.out("RECV_SO", "\"+java.nio.ByteBuffer.wrap(metaID).getInt()+\"", className, methodName, line) +
                                    "} $_ = tempLen;"
                    );
            }

            if (currentCtClass.getName().contains("IFileInputStream")) {
                if (methodClassName.equals("java.io.InputStream") && methodName.equals("read")) {
                    m.replace(
                            "Throwable t = new Throwable();" +
                                    "java.io.StringWriter sw = new java.io.StringWriter();" +
                                    "java.io.PrintWriter pw = new java.io.PrintWriter(sw);" +
                                    "pw.flush();" +
                                    "t.printStackTrace(pw);" +
                                    "if(sw.toString().contains(\"shuffleInMemory\")){" + // if caller is shuffleInMemory...
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] call read $1:\"+$1+\"  $2:\"+$2+\"  $3:\"+$3);" +
                                    "int readNum = $3/wrapper.TraceID.MAX_BYTES_TO_READ;" +
                                    "if($3%wrapper.TraceID.MAX_BYTES_TO_READ == 0){readNum--;}" +
                                    "int tempLen = $3;" +
                                    "for(int i = 0; i <= readNum; i++){" +
                                    "byte[] metaID = new byte[4];" +
                                    "byte[] metaLength = new byte[4];" +
                                    "int metaRem = 4;" +
                                    "while(metaRem > 0){$_ = $proceed(metaID,4 - metaRem,metaRem); metaRem -= $_;}" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] metaID$_:\"+$_);" +
                                    "metaRem = 4;" +
                                    "while(metaRem > 0){$_ = $proceed(metaLength,4 - metaRem,metaRem); metaRem -= $_;}" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] metaLength$_:\"+$_);" +
                                    "$_ = 0;" +
                                    "int omegaLen = java.lang.Math.min(java.nio.ByteBuffer.wrap(metaLength).getInt(),$3);" +
                                    "$3 -= omegaLen;" +
                                    "wrapper.OmegaLogger.LogOutPutFile(\"[TemporaryTrace] omegaLen:\"+omegaLen+\"  $3:\"+$3);" +
                                    "while(omegaLen > 0){" +
                                    "$_ = $proceed($1,$2,omegaLen);" +
                                    "$2 += $_;" +
                                    "omegaLen -= $_;" +
                                    LogCode.out("RECV_SO", "\"+java.nio.ByteBuffer.wrap(metaID).getInt()+\"", className, methodName, line) +
                                    "}} $_ = tempLen; }else{$_ = $proceed($$);}"
                    );
                }
            }

            if (longName.contains("Stream.write")) {
                String hash_tmp = "\"+$1.TraceObjectID+\"";
                if (longName.contains("Object")) {
                    m.replace(
                            "$1.TraceObjectID = wrapper.TraceID.getID;" +
                                    LogCode.out("SEND_SO", hash_tmp, className, methodName, line) +
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
                                    LogCode.out("RECV_SO", hash_tmp, className, methodName, line)
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
                        (LogCode.out("FORK_PA", "\"+$1.hashCode()+\"", className, methodName, line) +
                                "$_ = $proceed($$);"
                        );

                System.out.println(String.format("\t[OK]Trace: ExecutorService at %s", className));
            }

            // lock / unlock
            if (longName.contains("java.util.concurrent.locks.ReentrantLock.lock()")) {
                m.replace
                        (
                                "$_ = $proceed($$);" +
                                        LogCode.out("LOCK", "\"+$0.hashCode()+\"", className, methodName, line)
                        );
                System.out.println(String.format("\t[OK]Trace: ReentrantLock.lock() at %s", className));
            }
            if (longName.contains("java.util.concurrent.locks.ReentrantLock.unlock()")) {
                m.replace
                        (LogCode.out("REL", "\"+$0.hashCode()+\"", className, methodName, line) +
                                "$_ = $proceed($$);"
                        );
                System.out.println(String.format("\t[OK]Trace: ReentrantLock.unlock() at %s", className));
            }

            // wait / notify
//            if (longName.contains("java.lang.Object.notify()")) {
//                String hash_tmp = "\"+$_.hashCode()+\"";
//                m.replace
//                        (
//                                LogCode.out("NOTIFY", "\"+$0.hashCode()+\"", className, methodName, line) +
//                                        "$_ = $proceed($$);"
//                        );
//            }
//            if (longName.contains("java.lang.Object.notifyAll()")) {
//                String hash_tmp = "\"+$_.hashCode()+\"";
//                m.replace
//                        (
//                                LogCode.out("NOTIFYALL", "\"+$0.hashCode()+\"", className, methodName, line) +
//                                        "$_ = $proceed($$);"
//                        );
//            }
//            if (longName.contains("java.lang.Object.wait()")) {
//                m.replace(
//                        "$_ = $proceed($$);" +
//                                LogCode.out("WAITEXIT", "\"+$0.hashCode()+\"", className, methodName, line)
//                );
//            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[ERROR]" + currentCtClass.getName() + ":" + currentMethod.getName());
            System.out.println(longName);
            System.exit(1);
        }


    }
}
