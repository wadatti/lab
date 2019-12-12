import javassist.*;
import javassist.bytecode.*;

import java.util.List;

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
        }
    }

    private void SynBlockLookup(MethodInfo minfo) {
        CodeAttribute ca = minfo.getCodeAttribute();
        ca.setMaxStack(ca.getMaxStack() + 4);
        CodeIterator iterator = ca.iterator();

        try {
            for (iterator.begin(); iterator.hasNext(); iterator.next()) {
                if (iterator.byteAt(iterator.lookAhead()) == Opcode.MONITORENTER) {
                    iterator.next();
                    iterator.insert(createMethodCall("LOCK", minfo));
                } else if (iterator.byteAt(iterator.lookAhead()) == Opcode.MONITOREXIT) {
                    iterator.insert(createMethodCall("REL", minfo));
                }
            }
        } catch (BadBytecode badBytecode) {
            badBytecode.printStackTrace();
        }
    }

    private byte[] createMethodCall(String op, MethodInfo minfo) {
        ConstPool cp = c.getClassFile().getConstPool();
        Bytecode instrumentCode = new Bytecode(cp);

        instrumentCode.addGetstatic("java.lang.System", "out", "Ljava/io/PrintStream;"); // out フィールド呼び出し
        instrumentCode.addNew("java.lang.StringBuilder"); // StringBuilder使うよ
        instrumentCode.add(Opcode.DUP); // 複製, リアルコードがやってたから1回やっとくといいっぽい
        instrumentCode.addInvokespecial("java.lang.StringBuilder", "<init>", "()V");
        instrumentCode.addLdc("[TraceLog] LOCK, hashcode, ");
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        instrumentCode.addInvokestatic("java.lang.Thread", "currentThread", "()Ljava/lang/Thread;");
        instrumentCode.addInvokevirtual("java.lang.Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;");
        instrumentCode.add(Opcode.ICONST_1);
        instrumentCode.add(Opcode.AALOAD);
        instrumentCode.addInvokevirtual("java.lang.StackTraceElement", "getClassName", "()Ljava/lang/String;");
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        instrumentCode.addLdc(", ");
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        instrumentCode.addInvokestatic("java.lang.Thread", "currentThread", "()Ljava/lang/Thread;");
        instrumentCode.addInvokevirtual("java.lang.Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;");
        instrumentCode.add(Opcode.ICONST_1);
        instrumentCode.add(Opcode.AALOAD);
        instrumentCode.addInvokevirtual("java.lang.StackTraceElement", "getLineNumber", "()I");
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "toString", "()Ljava/lang/String;");
        instrumentCode.addInvokevirtual("java.io.PrintStream", "println", "(Ljava/lang/String;)V");

        System.out.println("[OK]Trace: Synchronized Block " + minfo.getName());

        return instrumentCode.get();
    }

    public CtClass getC() {
        return c;
    }
}
