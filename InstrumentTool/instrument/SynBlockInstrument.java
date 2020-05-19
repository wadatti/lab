package instrument;

import javassist.*;
import javassist.bytecode.*;

public class SynBlockInstrument {
    private CtClass c;

    public SynBlockInstrument(CtClass c) {
        this.c = c;
    }

    public void instrument() {
        ClassFile cf = c.getClassFile();

        try {
            for (MethodInfo minfo : cf.getMethods()) {
                if (minfo.isMethod()) {
                    CtMethod cm = c.getDeclaredMethod(minfo.getName());
                    if (Modifier.isAbstract(cm.getModifiers()))
                        return;
                }
                SynBlockLookup(minfo);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void SynBlockLookup(MethodInfo minfo) {
        CodeAttribute ca = minfo.getCodeAttribute();
        ca.setMaxStack(ca.getMaxStack() + 1);
        CodeIterator iterator = ca.iterator();

        try {
            for (iterator.begin(); iterator.hasNext(); iterator.next()) {
                if (iterator.byteAt(iterator.lookAhead()) == Opcode.MONITORENTER) {
                    iterator.insert(createByteCode(Opcode.DUP));
//                    iterator.next();
                    iterator.insert(createMethodCall("LOCK", minfo));
                } else if (iterator.byteAt(iterator.lookAhead()) == Opcode.MONITOREXIT) {
                    iterator.insert(createByteCode(Opcode.DUP));
                    iterator.insert(createMethodCall("REL", minfo));
                }
            }
        } catch (BadBytecode badBytecode) {
            badBytecode.printStackTrace();
            System.exit(1);
        }
    }

    private byte[] createByteCode(int opcode) {
        ConstPool cp = c.getClassFile().getConstPool();
        Bytecode instrumentCode = new Bytecode(cp);

        instrumentCode.add(opcode);

        return instrumentCode.get();
    }

    private byte[] createMethodCall(String op, MethodInfo minfo) {
        ConstPool cp = c.getClassFile().getConstPool();
        Bytecode instrumentCode = new Bytecode(cp);

        if (op.equals("LOCK")) {
            instrumentCode.addInvokestatic("wrapper.SyncBlock", "begin", "(Ljava/lang/Object;)V");
        } else if (op.equals("REL")) {
            instrumentCode.addInvokestatic("wrapper.SyncBlock", "end", "(Ljava/lang/Object;)V");
        }

        System.out.println("\t[OK]Trace: Synchronized Block " + minfo.getName() + ", " + c.getName());

        return instrumentCode.get();
    }

    public CtClass getC() {
        return c;
    }
}
