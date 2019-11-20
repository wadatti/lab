import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
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
        CodeIterator iterator = minfo.getCodeAttribute().iterator();
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
        instrumentCode.addLdc("[TraceLog] " + op + ", 888, "); // Stringのロード
        instrumentCode.addInvokespecial("java.lang.StringBuilder", "<init>", "(Ljava/lang/String;)V"); // sb = new StringBuilder("[hoge]")
        instrumentCode.addInvokestatic("java.lang.Thread", "currentThread", "()Ljava/lang/Thread;"); // t = Thread.currentThread();
        instrumentCode.addInvokevirtual("java.lang.Thread", "getId", "()J"); // tid = t.getId();
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "append", "(J)Ljava/lang/StringBuilder;"); // sb.append(tid);
        instrumentCode.addLdc(", 0" + LogCode.LogOutTail(c.getName(), 0)); // str = "fugafuga" (出力用情報)
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"); // sb.append(str);
        instrumentCode.addInvokevirtual("java.lang.StringBuilder", "toString", "()Ljava/lang/String;"); // output = sb.toString();
        instrumentCode.addInvokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V"); // System.out.println(output);

        return instrumentCode.get();
    }

    public CtClass getC() {
        return c;
    }
}
