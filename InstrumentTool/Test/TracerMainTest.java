import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TracerMainTest {
    @ParameterizedTest
    @MethodSource("targetJar")
    void mainTest(String[] args) {
        TracerMain.main(args);
        defrostAll(args);
    }

    static Stream<Arguments> targetJar() {
        return Stream.of(
                Arguments.of((Object) new String[]{"TestInput", "LockSample.jar"})
//                Arguments.of((Object) new String[]{"input", "hadoop-0.20-mapreduce/hadoop-core-2.0.0-mr1-cdh4.0.0.jar"})
        );
    }

    //    @AfterEach
    void outputClean() {
        ProcessBuilder rm = new ProcessBuilder("sh", "./script/CleanOutput.sh");

        rm.redirectErrorStream(true);
        try {
            Process process = rm.start();
            int end = process.waitFor();

            String mes;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((mes = bufferedReader.readLine()) != null) {
                System.out.println(mes);
            }

            if (end != 0) {
                fail();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void defrostAll(String[] args) {
        String inputPath = args[0] + "/";
        String outputPath = "output/";
        String targetFileName = inputPath + args[1];
        File inputDir = new File(inputPath);
        JarFile targetFile;
        Set<CtClass> targetClass = new HashSet<>();

        ClassPool classPool = ClassPool.getDefault();


        classPool.importPackage("wrapper");
        try {
            File wrapperDir = new File("./src/wrapper/");
            for (File file : Objects.requireNonNull(wrapperDir.listFiles())) {
                classPool.get("wrapper." + file.getName().replace(".java", "")).writeFile(outputPath);
            }
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

        // I want to append the target jar file at the end.
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

        for (CtClass c : targetClass) {
            c.defrost();
        }
    }
}