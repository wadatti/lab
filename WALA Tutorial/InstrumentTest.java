import com.ibm.wala.shrikeBT.*;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;

public class InstrumentTest {
    private final static boolean disasm = true;

    private final static boolean verify = true;

    private static OfflineInstrumenter instrumenter = new OfflineInstrumenter();

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1; i++) {

            Writer w = new BufferedWriter(new FileWriter("report", false));

            args = instrumenter.parseStandardArgs(args);

            instrumenter.setPassUnmodifiedClasses(true);
            instrumenter.beginTraversal();
            ClassInstrumenter ci;
            while ((ci = instrumenter.nextClass()) != null) {
                doClass(ci, w);
            }
            instrumenter.close();
        }
    }

    

    static final String fieldName = "_Bench_enable_trace";

    // Keep these commonly used instructions around
    static final Instruction getSysOut = Util.makeGet(System.class, "out");

    static final Instruction callPrintln = Util.makeInvoke(PrintStream.class, "println", new Class[]{String.class});

    private static void doClass(final ClassInstrumenter ci, Writer w) throws Exception {
        final String className = ci.getReader().getName();
        System.out.println("Class name : " + className);
        w.write("Class: " + className + "\n");
        w.flush();

        for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
            MethodData d = ci.visitMethod(m);
            System.out.println(d.getName());
            // d could be null, e.g., if the method is abstract or native
            if (d != null) {
                w.write("Instrumenting " + ci.getReader().getMethodName(m) + " " + ci.getReader().getMethodType(m) + ":\n");
                w.flush();

                if (disasm) {
                    w.write("Initial ShrikeBT code:\n");
                    (new Disassembler(d)).disassembleTo(w);
                    w.flush();
                }

                if (verify) {
                    Verifier v = new Verifier(d);
                    v.verify();
                }

                MethodEditor methodEditor = new MethodEditor(d);
                methodEditor.beginPass();

                final int noTraceLabel = methodEditor.allocateLabel();

                IInstruction[] instr = methodEditor.getInstructions();
                final String msg0 = "Loop called at " + Util.makeClass("L" + ci.getReader().getName() + ";") + "."
                        + ci.getReader().getMethodName(m);
                int i = 0;

                for (IInstruction in : instr) {
                    if (in instanceof ConditionalBranchInstruction) {

                        int b = i;
                        methodEditor.insertBefore(i, new MethodEditor.Patch() {
                            @Override
                            public void emitTo(MethodEditor.Output w) {
                                w.emit(getSysOut);
                                w.emit(ConstantInstruction.makeString(msg0));
                                w.emit(callPrintln);
                                w.emitLabel(noTraceLabel);
                            }
                        });
                    }

                    i++;
                    System.out.println(in.toString());
                }
                methodEditor.applyPatches();
                if (disasm) {
                    w.write("Final ShrikeBT code:\n");
                    (new Disassembler(d)).disassembleTo(w);
                    w.flush();
                }
            }
        }
        ClassWriter cw = ci.emitClass();
        instrumenter.outputModifiedClass(ci, cw);
    }
}
