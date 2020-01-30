package instrument;

import instrument.tool.LogCode;
import instrument.tool.RPCMethodCreator;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;
import java.util.*;

/**
 * instrument for:
 * RPC: implemented VersionedProtocol interface and parameter has JvmContext object
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
        interTrackerProtocolInstrument();

        for (CtClass instrumentClass : targetClass) {
            if (instrumentClass.isInterface()) continue;
            instrumentClass.instrument(new InstrumentJobSubmissionProtocolCaller(instrumentClass));
            instrumentClass.instrument(new InstrumentTaskUmbilicalProtocolCaller(instrumentClass));
            instrumentClass.instrument(new InstrumentInterTrackerProtocolCaller(instrumentClass));
        }

        try {
//            instrumentRPCClassMethod();

            instrumentJobSubmissionCallee();
            instrumentTaskUmbilicalCallee();
            instrumentInterTrackerCallee();
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

    private void interTrackerProtocolInstrument() throws NotFoundException, CannotCompileException {
        CtClass instrumentInterface = cp.get("org.apache.hadoop.mapred.InterTrackerProtocol");
        instrumentInterface.addMethod(RPCMethodCreator.interfaceMethodCreate(instrumentInterface, "String", "getSystemDirWrapper"));
        instrumentInterface.addMethod(RPCMethodCreator.interfaceMethodCreate(instrumentInterface, "String", "getBuildVersionWrapper"));
        instrumentInterface.addMethod(RPCMethodCreator.interfaceMethodCreate(instrumentInterface, "String", "getVIVersionWrapper"));
    }

    private void jobSubmissionProtocolInstrument() throws NotFoundException, CannotCompileException {
        CtClass instrumentInterface = cp.get("org.apache.hadoop.mapred.JobSubmissionProtocol");
        instrumentInterface.addMethod(RPCMethodCreator.interfaceMethodCreate(instrumentInterface, "org.apache.hadoop.mapred.JobID", "getNewJobIdWrapper"));
        instrumentInterface.addMethod(RPCMethodCreator.interfaceMethodCreate(instrumentInterface, "String", "getStagingAreaDirWrapper"));
        instrumentInterface.addMethod(RPCMethodCreator.interfaceMethodCreate(instrumentInterface, "org.apache.hadoop.security.authorize.AccessControlList", "getQueueAdminsWrapper", "String"));
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
                            instrumentCallee(instrumentClass, method, "JobID", line);
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("TaskAttemptID")) {
                            instrumentCallee(instrumentClass, method, "TaskAttemptID", line);
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("Token")) {
                            instrumentCallee(instrumentClass, method, "Token", line);
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("Text")) {
                            instrumentCallee(instrumentClass, method, "Text", line);
                        }
                    }
                    CtMethod getNewJobIdWrapper = CtNewMethod.make("public org.apache.hadoop.mapred.JobID getNewJobIdWrapper(int TraceID) throws java.io.IOException {" +
                            LogCode.out("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getNewJobId", 0) +
                            "org.apache.hadoop.mapred.JobID var = getNewJobId(); " +
                            LogCode.out("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getNewJobId", 0) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(getNewJobIdWrapper);

                    CtMethod getStagingAreaDirWrapper = CtNewMethod.make("public String getStagingAreaDirWrapper(int TraceID) throws java.io.IOException {" +
                            LogCode.out("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getStagingAreaDir", 0) +
                            "String var = getStagingAreaDir(); " +
                            LogCode.out("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getStagingAreaDir", 0) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(getStagingAreaDirWrapper);
                    CtMethod getQueueAdminsWrapper = CtNewMethod.make("public org.apache.hadoop.security.authorize.AccessControlList getQueueAdminsWrapper(String arg,int TraceID) throws java.io.IOException {" +
                            LogCode.out("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getQueueAdmins", 0) +
                            "org.apache.hadoop.security.authorize.AccessControlList var = getQueueAdmins(arg); " +
                            LogCode.out("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getQueueAdmins", 0) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(getQueueAdminsWrapper);
                    break;
                } else if (interFace.getName().equals("org.apache.hadoop.mapred.JTProtocols") && interFace.getName().equals("org.apache.hadoop.mapred.JobSubmissionProtocol")) {
                    System.exit(1);
                }
            }
        }
    }

    private void instrumentCallee(CtClass instrumentClass, CtMethod method, String parameterName, int line) throws NotFoundException, CannotCompileException {
        int argNum = countArgumentPosition(method.getLongName(), parameterName);
        String hash = "\"+$" + argNum + ".traceID+\"";
        CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
        instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
        instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
        System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
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
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        } else if (method.getLongName().substring(method.getLongName().indexOf("(")).contains("JobID")) {
                            instrumentCallee(instrumentClass, method, "JobID", line);
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
                        instrumentCallee(instrumentClass, method, "TaskTrackerStatus", line);
                    }
                    CtMethod getSystemDirWrapper = CtNewMethod.make("public String getSystemDirWrapper(int TraceID) throws java.io.IOException {" +
                            LogCode.out("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getSystemDir", line) +
                            "String var = getSystemDir(); " +
                            LogCode.out("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getSystemDir", line) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(getSystemDirWrapper);
                    CtMethod getBuildVersionWrapper = CtNewMethod.make("public String getBuildVersionWrapper(int TraceID) throws java.io.IOException {" +
                            LogCode.out("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getBuildVersion", line) +
                            "String var = getBuildVersion(); " +
                            LogCode.out("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getBuildVersion", line) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(getBuildVersionWrapper);
                    CtMethod getVIVersionWrapper = CtNewMethod.make("public String getVIVersionWrapper(int TraceID) throws java.io.IOException {" +
                            LogCode.out("RPC_RECV_CH", "\"+TraceID+\"", instrumentClass.getName(), "getVIVersion", line) +
                            "String var = getVIVersion(); " +
                            LogCode.out("RPC_SEND_CH", "\"+TraceID+\"", instrumentClass.getName(), "getVIVersion", line) +
                            "return var;}", instrumentClass);
                    instrumentClass.addMethod(getVIVersionWrapper);
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
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("TaskAttemptID")) {
                    int argNum = countArgumentPosition(methodLongName, "TaskAttemptID");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("Token")) {
                    int argNum = countArgumentPosition(methodLongName, "Token");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(method.getLongName().indexOf("(")).contains("Text")) {
                    int argNum = countArgumentPosition(methodLongName, "Text");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.JobSubmissionProtocol.getNewJobId()")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $0.getNewJobIdWrapper(tempId);" +
                            LogCode.out("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.JobSubmissionProtocol.getStagingAreaDir()")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $0.getStagingAreaDirWrapper(tempId);" +
                            LogCode.out("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.JobSubmissionProtocol.getQueueAdmins")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $0.getQueueAdminsWrapper($1, tempId);" +
                            LogCode.out("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line));
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
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.substring(methodLongName.indexOf("(")).contains("JobID")) {
                    int argNum = countArgumentPosition(methodLongName, "JobID");
                    m.replace("$" + argNum + ".traceID = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
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
                            LogCode.out("RPC_SEND_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $proceed($$);" +
                            LogCode.out("RPC_RECV_PA", "\"+$" + argNum + ".traceID+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.InterTrackerProtocol.getSystemDir()")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $0.getSystemDirWrapper(tempId);" +
                            LogCode.out("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.InterTrackerProtocol.getBuildVersion()")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $0.getBuildVersionWrapper(tempId);" +
                            LogCode.out("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                } else if (rpcInterfaces.get(i).contains(method) && methodLongName.contains("org.apache.hadoop.mapred.InterTrackerProtocol.getVIVersion()")) {
                    m.replace("int tempId = wrapper.TraceID.getID();" +
                            LogCode.out("RPC_SEND_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line) +
                            "$_ = $0.getVIVersionWrapper(tempId);" +
                            LogCode.out("RPC_RECV_PA", "\"+tempId+\"", currentCtClass.getName(), methodName, line));
                    System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, currentCtClass.getName());
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
