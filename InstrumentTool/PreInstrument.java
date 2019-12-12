import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.io.IOException;

public class PreInstrument extends ExprEditor {
    private CtClass currentCtClass;
    private ClassPool cpool;
    private CtMethod currentMethod;
    private String longName;


    public PreInstrument(CtClass currentCtClass, ClassPool cpool) {
        this.currentCtClass = currentCtClass;
        this.cpool = cpool;
    }

    @Override
    public void edit(NewExpr n) {
        try {
            String className = n.getClassName();
            int line = n.getLineNumber();
            longName = n.getFileName();
            CtClass clazz = cpool.get(n.getClassName());
            if (clazz.getName().equals("java.lang.Thread")) {
                n.replace("$_ = new WrapperThread($$);");
                System.out.println(String.format("\t[OK]preInstrument: java.lang.Thread at %s", className));
            }

        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
            System.out.println(longName);
            System.exit(1);
        }

    }

    @Override
    public void edit(MethodCall m) throws CannotCompileException {
        String methodName = m.getMethodName();
        String className = currentCtClass.getName();
        int line = m.getLineNumber();

        // fork at parent
        if (methodName.equals("start")) {
            //メソッドシグネチャを表す(詳しくはJavaByteCode参照)　()VはVoid型
            if (!m.getSignature().equals("()V")) return;

            System.out.println("\t[OK]Trace: start() at " + className);
            return;
        }

        // join at parent
        if (methodName.equals("join")) {
            if (!m.getSignature().equals("()V")) return;

            System.out.println("\t[OK]Trace: join() at " + className);
            return;
        }


        // Socket
        try {
            CtMethod mm = m.getMethod();
            longName = mm.getLongName();
            currentMethod = m.getMethod();


            // socket send
            if (longName.contains("Stream.write")) {
                System.out.println(mm.getLongName());
                CtClass[] insertClasses = mm.getParameterTypes();
                if (longName.contains("Object")) {
                    for (CtClass insertClass : mm.getParameterTypes()) {
                        for (CtField field : insertClass.getFields()) {
                            if (field.getName().equals("TraceObjectID"))
                                return;
                        }
                        if (insertClass.getName().contains("Object")) {
                            CtField f = new CtField(CtClass.intType, "TraceObjectID", insertClasses[0]);
                            f.setModifiers(Modifier.PUBLIC);
                            insertClass.addField(f);
                        }
                    }
                }
                System.out.printf("\t[OK]Trace: Socket %s at %s", methodName, className);

            }

            // socket recv
            if (longName.contains("Stream.read")) {
                System.out.println(mm.getLongName());
                CtClass insertClass = mm.getReturnType();
                if (longName.contains("Object")) {
                    for (CtField field : insertClass.getFields()) {
                        if (field.getName().equals("TraceObjectID"))
                            return;
                    }
                    if (insertClass.getName().contains("Object")) {
                        CtField f = new CtField(CtClass.intType, "TraceObjectID", insertClass);
                        f.setModifiers(Modifier.PUBLIC);
                        insertClass.addField(f);
                        insertClass.writeFile("output/");
                    }

                }
                System.out.println(String.format("\t[OK]preInstrument: Socket %s at %s", methodName, className));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[ERROR]" + currentCtClass.getName() + ":" + line);
            System.out.println(methodName);
            System.out.println(longName);
            System.exit(1);
        }


    }
}
