import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {

    public static void main(String[] args) {
        String targetFile = "input/" + args[0];
        String targetDir = "input/";

        File inputDir = new File(targetDir);

        ClassPool classPool = ClassPool.getDefault();
        List<CtClass> targetClass = new ArrayList<>();

        for (File file : FileListGet.getFiles(inputDir)) {
            try {
                classPool.appendClassPath(file.getPath());
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }

        try {
            JarFile jarFile = new JarFile(new File(targetFile));
            for (Enumeration e = jarFile.entries(); e.hasMoreElements(); ) {
                JarEntry entry = (JarEntry) e.nextElement();
                String fileName = entry.getName();
                if (fileName.endsWith(".class")) {
                    targetClass.add(classPool.get(fileName.replace("/", ".").replace(".class", "")));
                }
            }
        } catch (IOException | NotFoundException e) {
            e.printStackTrace();
        }

        try {
            for (CtClass c : targetClass) {
                if (c.getSuperclass().getName().contains("rmi")) {
                    System.out.println(c.getName());
                }
            }
        } catch (
                NotFoundException e) {
            e.printStackTrace();
        }


    }
}
