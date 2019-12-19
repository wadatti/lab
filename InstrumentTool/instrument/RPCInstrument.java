package instrument;

import instrument.tool.LogCode;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.*;

/**
 * instrument for:
 * RPC: implemented VerisonedProtocol interface and parameter has JvmContext object
 */
public class RPCInstrument {
    private Set<CtClass> targetClass;
    private Set<CtMethod> targetMethod;
    private Map<CtClass, HashSet<CtMethod>> rpcInterfaces = new HashMap<>();
    private ClassPool cp;

    public RPCInstrument(Set<CtClass> targetClass, ClassPool cp) {
        this.targetClass = targetClass;
        this.cp = cp;
    }

    private void addField() throws NotFoundException, CannotCompileException {
        CtClass jvmContext = cp.get("org.apache.hadoop.mapred.JvmContext");
        CtField iD = new CtField(CtClass.intType, "traceID", jvmContext);
        iD.setModifiers(Modifier.PUBLIC);
        jvmContext.addField(iD);
    }

    private void collect() throws NotFoundException {
        int numberOfRPCInterface = 0;

        while (true) {
            for (CtClass instrumentClass : targetClass) {
                for (CtClass interFace : instrumentClass.getInterfaces()) {
                    if (interFace.getName().contains("VersionedProtocol")) {
                        rpcInterfaces.put(instrumentClass, new HashSet<>(Arrays.asList(instrumentClass.getDeclaredMethods())));
                        System.out.println(instrumentClass.getName() + " is extended Versioned Protocol interface");
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

    public Set<CtClass> instrument() throws NotFoundException, CannotCompileException {
        Set<CtClass> result = new HashSet<>();

        collect();
        addField();

//        for (Map.Entry<CtClass, HashSet<CtMethod>> e : rpcInterfaces.entrySet()) {
//            System.out.println("-----------" + e.getKey().getName());
//            for (CtMethod m : e.getValue()) {
//                System.out.println(m.getName());
//            }
//        }

        for (CtClass instrumentClass : targetClass) {
            if (instrumentClass.isInterface()) continue;
            instrumentClass.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    try {
                        CtClass i = cp.get("org.apache.hadoop.mapred.TaskUmbilicalProtocol");
                        String methodName = m.getMethodName();
                        if (rpcInterfaces.get(i).contains(m.getMethod()) && m.getMethod().getLongName().contains("JvmContext")) {
                            int line = m.getLineNumber();
                            int jvmNum = 1;
                            for (String s : m.getMethod().getLongName().split(",")) {
                                if (s.contains("JvmContext")) {
                                    break;
                                }
                                jvmNum++;
                            }
                            m.replace("$" + jvmNum + ".traceID = wrapper.TraceID.getID();" +
                                    LogCode.out("RPC_SEND_PA", "\"+$" + jvmNum + ".traceID+\"", m.getClassName(), m.getMethod().getLongName(), line) +
                                    "$_ = $proceed($$);" +
                                    LogCode.out("RPC_RECV_PA", "\"+$" + jvmNum + ".traceID+\"", m.getClassName(), m.getMethod().getLongName(), line));
                            System.out.printf("\t[OK]Trace: RPC %s at %s %n", methodName, instrumentClass.getName());
                        }
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        try {
            for (CtClass instrumentClass : targetClass) {
                if (instrumentClass.isInterface()) continue;
                for (CtClass interFace : instrumentClass.getInterfaces()) {
                    if (!interFace.getName().equals("org.apache.hadoop.mapred.TaskUmbilicalProtocol")) continue;
                    for (CtMethod method : rpcInterfaces.get(interFace)) {
                        int line = method.getMethodInfo().getLineNumber(0);
                        String caller = "\"+java.util.Arrays.asList(java.lang.Thread.currentThread().getStackTrace())+\"";
                        int jvmNum = 1;
                        for (String s : method.getLongName().split(",")) {
                            if (s.contains("JvmContext")) {
                                break;
                            }
                            jvmNum++;
                        }
                        String hash = "\"+$" + jvmNum + ".traceID+\"";
                        if (method.getLongName().contains("JvmContext")) {
                            CtMethod instrumentMethod = instrumentClass.getDeclaredMethod(method.getName());
                            instrumentMethod.insertBefore(LogCode.out("RPC_RECV_CH", hash, instrumentClass.getName(), method.getName(), line));
                            instrumentMethod.insertAfter(LogCode.out("RPC_SEND_CH", hash, instrumentClass.getName(), method.getName(), line));
                            System.out.println("\t[OK]Trace: RPC method " + method.getName() + " at " + instrumentClass.getName());
                        }
                    }
                    result.add(instrumentClass);
                    break;
                }
            }
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }

        return result;
    }
}
