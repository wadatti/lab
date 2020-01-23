package instrument.tool;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

public class RPCMethodCreator {
    public static CtMethod interfaceMethodCreate(CtClass instrumentInterface, String returnType, String methodName) {
        CtMethod method = null;
        try {
            method = CtNewMethod.make("public " + returnType + " " + methodName + "(int TraceID);", instrumentInterface);
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
        return method;
    }
}
