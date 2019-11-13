import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.MethodCall;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TracerMain {

    public static void main(String[] args) {
        String targetFileName = "input/" + args[0];
        String inputPath = "input/";
        String outputPath = "output/";
        File inputDir = new File(inputPath);
        JarFile targetFile = null;
        Set<CtClass> targetClass = new HashSet<>();
        try {
            targetFile = new JarFile(new File(targetFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }


        ClassPool classPool = ClassPool.getDefault();

        // classPool add
        RelationFileReader filePaths = new RelationFileReader(inputDir);
        for (String file : filePaths.getPaths()) {
            try {
                classPool.appendClassPath(file);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }

        // targetClass add
        try {
            for (Enumeration<JarEntry> e = targetFile.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                if (entry.getName().endsWith(".class")) {
                    targetClass.add(classPool.get(entry.getName().replace(".class", "").replace("/", ".")));
                    System.out.println(entry.getName());
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }


        try {
            for (CtClass instrumentClass : targetClass) {
                if (instrumentClass.isInterface()) {
                    instrumentClass.writeFile(outputPath);
                    continue;
                }

                instrumentClass.instrument(new LogExprEditor(instrumentClass, classPool));
                MethodInstrument methodInstrument = new MethodInstrument(instrumentClass);
                methodInstrument.instrumnet();
                methodInstrument.getC().writeFile(outputPath);
            }
        } catch (CannotCompileException | IOException e) {
            e.printStackTrace();
        }


    }
}