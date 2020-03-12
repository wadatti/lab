import instrument.*;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TracerMain {


    public static void main(String[] args) {
        String inputPath = "input/";
        String outputPath = "output/";
        String targetFileName = inputPath + args[0];
        File inputDir = new File(inputPath);
        JarFile targetFile;
        Set<CtClass> targetClass = new HashSet<>();

        ClassPool classPool = ClassPool.getDefault();


        classPool.importPackage("wrapper");
        try {
            classPool.get("wrapper.ThreadWrapper").writeFile(outputPath);
            classPool.get("wrapper.TraceID").writeFile(outputPath);
            classPool.get("wrapper.OmegaLogger").writeFile(outputPath);
            classPool.get("wrapper.OmegaObject").writeFile(outputPath);
        } catch (CannotCompileException | IOException | NotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }


        // classPool add
        RelationFileReader filePaths = new RelationFileReader(inputDir);
        for (String file : filePaths.getPaths()) {
            try {
                classPool.appendClassPath(file);
            } catch (NotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // I want to append target jar file to the end.
        try {
            classPool.appendClassPath(targetFileName);
        } catch (NotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }


        // targetClass add
        try {
            if (targetFileName.endsWith(".jar")) {
                targetFile = new JarFile(new File(targetFileName));
                for (Enumeration<JarEntry> e = targetFile.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = e.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        targetClass.add(classPool.get(entry.getName().replace(".class", "").replace("/", ".")));
                    }
                }
            } else if (targetFileName.endsWith(".class")) {
                targetClass.add(classPool.get(args[0].replace(".class", "")));
            } else {
                throw new IllegalArgumentException();
            }
        } catch (NotFoundException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }


        // start instrument
//        for (CtClass instrumentClass : targetClass) {
//            if (instrumentClass.isInterface() || Modifier.isAbstract(instrumentClass.getModifiers()) || instrumentClass.getName().contains("$")) {
//                continue;
//            }
//            boolean flag = false;
//            for (CtField field : instrumentClass.getDeclaredFields()) {
//                field.getName().contains("LOG");
//                flag = true;
//            }
//            if (flag) {
//                continue;
//            }
//
//            try {
//                CtClass logClass = classPool.get("org.apache.commons.logging.Log");
//                CtClass logFac = classPool.get("org.apache.commons.logging.LogFactory");
//                CtField logger = new CtField(logClass, "LOG", instrumentClass);
//                logger.setModifiers(Modifier.PUBLIC);
//                logger.setModifiers(Modifier.STATIC);
//                String[] init = {"this.class"};
//
//                instrumentClass.addField(logger, CtField.Initializer.byExpr("org.apache.commons.logging.LogFactory.getLog(\"this\")"));
//            } catch (CannotCompileException | NotFoundException e) {
//                e.printStackTrace();
//                System.out.println(instrumentClass.getName());
//                System.exit(1);
//            }
//        }


        try {
            CtClass wrapperThread = classPool.get("wrapper.ThreadWrapper");
            for (CtClass instrumentClass : targetClass) {
                AddMetaDataField addMetaDataField = new AddMetaDataField(instrumentClass);
                addMetaDataField.addField();
            }
            for (CtClass instrumentClass : targetClass) {
                if (instrumentClass.getSuperclass().getName().equals("java.lang.Thread")) {
                    instrumentClass.setSuperclass(wrapperThread);
                }
                instrumentClass.instrument(new PreInstrument(instrumentClass, classPool));
            }
            for (CtClass instrumentClass : targetClass) {
                SynBlockInstrument synBlockInstrument = new SynBlockInstrument(instrumentClass);
                synBlockInstrument.instrument();
                instrumentClass.instrument(new LogExprEditor(instrumentClass, classPool));
                MethodInstrument methodInstrument = new MethodInstrument(instrumentClass);
                methodInstrument.instrument();
            }
        } catch (CannotCompileException | NotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        RPCInstrument rpcInstrument = new RPCInstrument(targetClass, classPool);
        try {
            rpcInstrument.instrument();
        } catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // only method call instrument
//        for (CtClass instrumentClass : targetClass) {
//            NaiveInstrument naiveInstrument = new NaiveInstrument(instrumentClass, classPool);
//            naiveInstrument.instrumnet();
//        }

        for (CtClass instrumentClass : targetClass) {
            try {
                instrumentClass.writeFile(outputPath);
            } catch (CannotCompileException | IOException e) {
                e.printStackTrace();
                System.out.println(instrumentClass.getName());
                System.exit(1);
            }
        }

        // tentative instrument for RPC relation class
        try {
            CtClass server = classPool.get("org.apache.hadoop.ipc.Server$Handler");
            server.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) {
                    try {
                        if (m.getMethod().getName().contains("take")) {
                            m.replace("$_ = $proceed();" +
                                    "System.out.println(\"[RPC TRACE] \"+ $_.toString());");
                            System.out.println("deketa");
                        }
                    } catch (NotFoundException | CannotCompileException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            server.writeFile(outputPath);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}