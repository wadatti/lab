package instrument;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;

public class AddMetaDataField {
    CtClass c;

    public AddMetaDataField(CtClass c) {
        this.c = c;
    }

    public void addField() {
        String className = c.getName();

        try {
            if (className.contains("IFileInputStream")) {
                CtField readFlag = new CtField(CtClass.booleanType, "readFlag", c);
                readFlag.setModifiers(Modifier.PRIVATE);
                c.addField(readFlag, "true");
                System.out.println("\t[OK]add meta field at " + className);
            }
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }
}