import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;

public class Instrument {
    private static void InstrumentMain() throws NotFoundException, CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass mainClass = classPool.get("Main");
        CtMethod mainMethod = mainClass.getDeclaredMethod("main");
        ExprEditor exprEditor = new ExprEditor();
        mainMethod.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                m.replace("{System.out.println(\"insert!!!\");\n $_ = $proceed($$);}");
            }
        });
        mainClass.writeFile();
    }

    private static void InstrumentParallelSum() throws NotFoundException, IOException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass parallelSumClass = classPool.get("ParallelSum");
        parallelSumClass.writeFile();

    }

    public static void main(String[] args) {
        try {
            InstrumentMain();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            InstrumentParallelSum();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
