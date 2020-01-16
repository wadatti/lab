package instrument;

import instrument.tool.LogCode;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;
import java.util.*;

/**
 * instrument for:
 * RPC: implemented VerisonedProtocol interface and parameter has JvmContext object
 */
public class RPCInstrument {
    private Set<CtClass> targetClass;
    private Map<CtClass, HashSet<CtMethod>> rpcInterfaces = new HashMap<>();
    private ClassPool cp;

    public RPCInstrument(Set<CtClass> targetClass, ClassPool cp) {
        this.targetClass = targetClass;
        this.cp = cp;
    }

    public void instrument() throws NotFoundException, CannotCompileException {
        collect();
        addField();
        jobSubmissionProtocolInstrument();

        for (CtClass instrumentClass : targetClass) {
            if (instrumentClass.isInterface()) continue;
            instrumentClass.instrument(new InstrumentJobSubmissionProtocolCaller(instrumentClass));
            instrumentClass.instrument(new InstrumentTaskUmbilicalProtocolCaller(instrumentClass));
//            instrumentClass.instrument(new InstrumentInterTrackerProtocolCaller(instrumentClass));
        }

        try {
//            instrumentRPCClassMethod();

            instrumentJobSubmissionCallee();
            instrumentTaskUmbilicalCallee();
//            instrumentInterTrackerCallee();
        } catch (CannotCompileException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void addField() throws NotFoundException, CannotCompileException {
        CtClass jvmContext = cp.get("org.apache.hadoop.mapred.JvmContext");
        addFieldAndWritable(jvmContext);

        CtClass taskTrackerStatus = cp.get("org.apache.hadoop.mapred.TaskTrackerStatus");
        addFieldAndWritable(taskTrackerStatus);

        CtClass jobID = cp.get("org.apache.hadoop.mapreduce.JobID");
        addFieldAndWritable(jobID);

        CtClass taskAttemptID = cp.get("org.apache.hadoop.mapreduce.TaskAttemptID");
        addFieldAndWritable(taskAttemptID);

        CtClass token = cp.get("org.apache.hadoop.security.token.Token");
        addFieldAndWritable(token);

        CtClass text = cp.get("org.apache.hadoop.io.Text");
        addFieldAndWritable(text);

        System.out.println("\t[OK]Trace: add field at JvmContext");
        System.out.println("\t[OK]Trace: add field at TaskTrackerStatus");
        System.out.println("\t[OK]Trace: add field at JobID");
        System.out.println("\t[OK]Trace: add field at TaskAttemptID");
        System.out.println("\t[OK]Trace: add field at org.apache.hadoop.security.token.Token");
        System.out.println("\t[OK]Trace: add field at org.apache.hadoop.io.Text");

    }

    private void addFieldAndWritable(CtClass clazz) throws CannotCompileException, NotFoundException {
        CtField iD = new CtField(CtClass.intType, "traceID", clazz);
        iD.setModifiers(Modifier.PUBLIC);
        clazz.addField(iD);
        CtMethod write = clazz.getDeclaredMethod("write");
        CtMethod readFields = clazz.getDeclaredMethod("readFields");
        write.insertBefore("$1.writeInt(this.traceID);");
        readFields.insertBefore("this.traceID=$1.readInt();");
    }

    private void collect() throws NotFoundException {
        int numberOfRPCInterface = 0;
        CtClass versionedProtocol = cp.get("org.apache.hadoop.ipc.VersionedProtocol");
        rpcInterfaces.put(versionedProtocol, new HashSet<>(Arrays.asList(versionedProtocol.getDeclaredMethods())));
        while (true) {
            for (CtClass instrumentClass : targetClass) {
                for (CtClass interFace : instrumentClass.getInterfaces()) {
                    boolean addFlag = false;
                    for (CtClass rpcRelationInterface : rpcInterfaces.keySet()) {
                        if (interFace.equals(rpcRelationInterface) && instrumentClass.isInterface()) {
                            addFlag = true;
                            break;
                        }
                    }
                    if (addFlag) {
                        rpcInterfaces.put(instrumentClass, new HashSet<>(Arrays.asList(instrumentClass.getDeclaredMethods())));
                    }
                }
            }
            if (numberOfRPCInterface == rpcInterfaces.size()) {
                break;
            } else {
                numberOfRPCInterface = rpcInterfaces.size();
            }
        }
    }

    private int countArgumentPosition(String methodName, String className) {
        int argPos = 1;
        for (String s : methodName.substring(methodName.indexOf("(")).split(",")) {
            if (s.contains(className)) {
                return argPos;
            }
            argPos++;
        }
        throw new IllegalArgumentException();
    }

    private void jobSubmissionProtocolInstrument() throws NotFoundException, CannotCompileException {
        CtClass instrumentInterface = cp.get("org.apache.hadoop.mapred.JobSubmissionProtocol");
        CtMethod m = CtNewMethod.make("public org.apache.hadoop.mapred.JobID getNewJobIdWrapper(int TraceID);", instrumentInterface);
        instrumentInterface.addMethod(m);
    }

    private void instrumentJobSubmissionCallee() throws NotFoundException, CannotCompileException {
        for (CtClass instrumentClass : targetClass) {
            if (instrumentClass.isInterface()) continue;
            for (CtClass interFace : instrumentClass.getInterfaces()) {
                if (interFace.getName().equals("org.apache.hadoop.mapred.JTProtocols") || interFace.getName().equals("org.apache.hadoop.mapred.JobSubmissionProtocol")) {
                    for (CtMethod method : rpcInterfaces.get(cp.getCtClass("org.apache.hadoop.mapred.JobSubmissionProtocol"))) {
                        int line = method.getMethodInfo().getLineNumber(0);
                        String caller = "\"+java.util.Arrays.asList(java.lang.Thread.currentThread().getStackTrace())+\"";
                        if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("JobID")) {
                            int argNum = countArgumentPosition(method.getLongName(), "JobID");
                            String hash = "\"+$" + argNum + ".traceID+\"";
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("TaskAttemptID")) {
                            int argNum = countArgumentPosition(method.getLongName(), "TaskAttemptID");
                            String hash = "\"+$" + argNum + ".traceID+\"";
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("Token")) {
                            int argNum = countArgumentPosition(method.getLongName(), "Token");
                            String hash = "\"+$" + argNum + ".traceID+\"";
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("Text")) {
                            int argNum = countArgumentPosition(method.getLongName(), "Text");
                            String hash = "\"+$" + argNum + ".traceID+\"";
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        }
                    }
                    CtMethod m = CtNewMethod.make("public org.apache.hadoop.mapred.JobID getNewJobIdWrapper(int TraceID) throws java.io.IOException {" +
                            LogCode.omegaOut("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getNewJobIdWrapper", 0) +
                            "org.apache.hadoop.mapred.JobID var = getNewJobId(); " +
                            LogCode.omegaOut("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getNewJobIdWrapper", 0) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(m);
                    break;
                } else if (interFace.getName().equals("org.apache.hadoop.mapred.JTProtocols") && interFace.getName().equals("org.apache.hadoop.mapred.JobSubmissionProtocol")) {
                    System.exit(1);
                }
            }
        }
    }

    private void instrumentTaskUmbilicalCallee() throws NotFoundException, CannotCompileException {
        for (CtClass instrumentClass : targetClass) {
            if (instrumentClass.isInterface()) continue;
            for (CtClass interFace : instrumentClass.getInterfaces()) {
                if (interFace.getName().equals("org.apache.hadoop.mapred.TaskUmbilicalProtocol")) {
                    for (CtMethod method : rpcInterfaces.get(interFace)) {
                        int line = method.getMethodInfo().getLineNumber(0);
                        String caller = "\"+java.util.Arrays.asList(java.lang.Thread.currentThread().getStackTrace())+\"";
                        if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("JvmContext")) {
                            int argNum = countArgumentPosition(method.getLongName(), "JvmContext");
                            String hash = "\"+$" + argNum + ".traceID+\"";
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertBefore(LogCode.omegaOut("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.omegaOut("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("JobID")) {
                            int argNum = countArgumentPosition(method.getLongName(), "JobID");
                            String hash = "\"+$" + argNum + ".traceID+\"";
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        }
                    }
                    break;
                }
            }
        }
    }

    private void instrumentInterTrackerCallee() throws NotFoundException, CannotCompileException {
        for (CtClass instrumentClass : targetClass) {
            if (instrumentClass.isInterface()) continue;
            for (CtClass interFace : instrumentClass.getInterfaces()) {
                if (interFace.getName().equals("org.apache.hadoop.mapred.JTProtocols") || interFace.getName().equals("org.apache.hadoop.mapred.InterTrackerProtocol")) {
                    CtMethod method = instrumentClass.getDeclaredMethod("heartbeat");
                    int line = method.getMethodInfo().getLineNumber(0);
                    String caller = "\"+java.util.Arrays.asList(java.lang.Thread.currentThread().getStackTrace())+\"";
                    if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("TaskTrackerStatus")) {
                        int argNum = countArgumentPosition(method.getLongName(), "TaskTrackerStatus");
                        String hash = "\"+$" + argNum + ".traceID+\"";
                        CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                        instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                        instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                        System.out.println("\t[OK]Trace: RPC Method " + method.getName() + " at " + instrumentClass.getName());
                        break;
                    }
                } else if (interFace.getName().equals("org.apache.hadoop.mapred.JTProtocols") && interFace.getName().equals("org.apache.hadoop.mapred.InterTrackerProtocol")) {
                    System.exit(1);
                }
            }
        }
    }

    private void instrumentRPCClassMethod() throws NotFoundException, CannotCompileException, IOException {
        CtClass rpcClass = cp.get("org.apache.hadoop.ipc.RPC");
        for (CtMethod m : rpcClass.getDeclaredMethods()) {
            m.insertBefore(LogCode.out("RPC Method Call Begin", "-", rpcClass.getName(), m.getName(), m.getMethodInfo().getLineNumber(0)));
            m.insertAfter(LogCode.out("RPC Method Call End", "-", rpcClass.getName(), m.getName(), m.getMethodInfo().getLineNumber(0)));
            System.out.println("\t[OK]Trace: RPC Method Call " + m.getName() + " at " + rpcClass.getName());
        }
        rpcClass.writeFile("output/");
    }

    private class InstrumentJobSubmissionProtocolCaller extends ExprEditor {
        private CtClass currentCtClass;

        public InstrumentJobSubmissionProtocolCaller(CtClass currentCtClass) {
            this.currentCtClass = currentCtClass;
        }

        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            try {
                CtClass i = cp.get("org.apache.hadoop.mapred.JobSubmissionProtocol");
                CtMethod method = m.getMethod();
                String methodName = m.getMethodName();
                String methodLongName = method.getLongName();
                int line = m.getLineNumber();

                if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("JobID")) {
                    int argNum = countArgumentPosition(methodLongName, "JobID");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("TaskAttemptID")) {
                    int argNum = countArgumentPosition(methodLongName, "TaskAttemptID");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("Token")) {
                    int argNum = countArgumentPosition(methodLongName, "Token");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("Text")) {
                    int argNum = countArgumentPosition(methodLongName, "Text");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.JobSubmissionProtocol.getNewJobId()")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.omegaOut("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $0.getNewJobIdWrapper(tempId);" +
                            LogCode.omegaOut("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }


    private class InstrumentTaskUmbilicalProtocolCaller extends ExprEditor {
        private CtClass currentCtClass;

        public InstrumentTaskUmbilicalProtocolCaller(CtClass currentCtClass) {
            this.currentCtClass = currentCtClass;
        }

        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            try {
                CtClass i = cp.get("org.apache.hadoop.mapred.TaskUmbilicalProtocol");
                CtMethod method = m.getMethod();
                String methodName = m.getMethodName();
                String methodLongName = method.getLongName();
                int line = m.getLineNumber();

                if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(methodLongName.indexOf("(")).contains("JvmContext")) {
                    int argNum = countArgumentPosition(methodLongName, "JvmContext");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            LogCode.omegaOut("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            LogCode.omegaOut("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(methodLongName.indexOf("(")).contains("JobID")) {
                    int argNum = countArgumentPosition(methodLongName, "JobID");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            LogCode.omegaOut("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            LogCode.omegaOut("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private class InstrumentInterTrackerProtocolCaller extends ExprEditor {
        private CtClass currentCtClass;

        public InstrumentInterTrackerProtocolCaller(CtClass currentCtClass) {
            this.currentCtClass = currentCtClass;
        }

        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            try {
                CtClass i = cp.get("org.apache.hadoop.mapred.InterTrackerProtocol");
                CtMethod method = m.getMethod();
                String methodName = m.getMethodName();
                String methodLongName = method.getLongName();
                int line = m.getLineNumber();

                if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("TaskTrackerStatus")) {
                    int argNum = countArgumentPosition(methodLongName, "TaskTrackerStatus");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodLongName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
