package instrument;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.util.HashSet;
import java.util.Set;

public class PreInstrument extends ExprEditor {
    private CtClass currentCtClass;
    private ClassPool cpool;
    private CtMethod currentMethod;
    private String longName;
    public static Set<String> search = new HashSet<>();


    public PreInstrument(CtClass currentCtClass, ClassPool cpool) {
        this.currentCtClass = currentCtClass;
        this.cpool = cpool;
    }

    /**
     * replacing "new Thread" -> "new ThreadWrapper"
     *
     * @param n new operator object
     */
    @Override
    public void edit(NewExpr n) {
        try {
            String className = n.getClassName();
            int line = n.getLineNumber();
            longName = n.getFileName();
            CtClass clazz = cpool.get(n.getClassName());
            if (clazz.getName().equals("java.lang.Thread")) {
                n.replace("$_ = new ThreadWrapper($$);");
                System.out.println(String.format("\t[OK]preInstrument: java.lang.Thread at %s", className));
            }



        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
            System.out.println(longName);
            System.exit(1);
        }

    }

    @Override
    public void edit(MethodCall m) {
        String methodName = m.getMethodName();
        String className = currentCtClass.getName();
        int line = m.getLineNumber();



        // Socket
        try {
            CtMethod mm = m.getMethod();
            longName = mm.getLongName();
            currentMethod = m.getMethod();



            // socket send
            if (longName.contains("Stream.write")) {
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
                    System.out.printf("\t[OK]Trace: Socket %s at %s", methodName, className);
                }
            }

            // socket recv
            if (longName.contains("Stream.read")) {
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
                    System.out.println(String.format("\t[OK]preInstrument: Socket %s at %s", methodName, className));
                }
            }

            if (longName.contains("java.util.concurrent")) {
                search.add(longName);
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
