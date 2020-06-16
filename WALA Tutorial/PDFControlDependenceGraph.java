import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * This simple example application builds a WALA CDG and fires off ghostview to viz a DOT
 * representation.
 *
 * @author sfink
 */
public class PDFControlDependenceGraph {

    public static final boolean SANITIZE_CFG = false;

    public static final String PDF_FILE = "cdg.pdf";

    /**
     * Usage: GVControlDependenceGraph -appJar [jar file name] -sig [method signature] The "jar file
     * name" should be something like "c:/temp/testdata/java_cup.jar" The signature should be
     * something like "java_cup.lexer.advance()V"
     */
    public static void main(String[] args) throws IOException {

        run(args);
    }

    /**
     * @param args -appJar [jar file name] -sig [method signature] The "jar file name" should be
     *             something like "c:/temp/testdata/java_cup.jar" The signature should be something like
     *             "java_cup.lexer.advance()V"
     */
    public static Process run(String[] args) throws IOException {
        validateCommandLine(args);
        return run(args[1], args[3]);
    }

    /**
     * @param appJar    should be something like "c:/temp/testdata/java_cup.jar"
     * @param methodSig should be something like "java_cup.lexer.advance()V"
     */
    public static Process run(String appJar, String methodSig) throws IOException {
        try {
            if (PDFCallGraph.isDirectory(appJar)) {
                appJar = PDFCallGraph.findJarFiles(new String[]{appJar});
            }
            AnalysisScope scope =
                    AnalysisScopeReader.makeJavaBinaryAnalysisScope(
                            appJar, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

            ClassHierarchy cha = ClassHierarchyFactory.make(scope);

            MethodReference mr = StringStuff.makeMethodReference(methodSig);

            IMethod m = cha.resolveMethod(mr);
            if (m == null) {
                System.err.println("could not resolve " + mr);
                throw new RuntimeException();
            }
            AnalysisOptions options = new AnalysisOptions();
            options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
            IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());
            IR ir = cache.getIR(m, Everywhere.EVERYWHERE);

            if (ir == null) {
                Assertions.UNREACHABLE("Null IR for " + m);
            }

            System.err.println(ir.toString());
            ControlDependenceGraph<ISSABasicBlock> cdg =
                    new ControlDependenceGraph<>(ir.getControlFlowGraph());

            Properties wp = null;
            try {
                wp = WalaProperties.loadProperties();
                wp.putAll(WalaExamplesProperties.loadProperties());
            } catch (WalaException e) {
                e.printStackTrace();
                Assertions.UNREACHABLE();
            }
            String psFile = "./out/cfg.pdf";
            String dotFile = "./out/temp.dt";
            String dotExe = "dot";
            String gvExe = "open";

            DotUtil.<ISSABasicBlock>dotify(cdg, PDFViewUtil.makeIRDecorator(ir), dotFile, psFile, dotExe);

            return PDFViewUtil.launchPDFView(psFile, gvExe);

        } catch (WalaException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validate that the command-line arguments obey the expected usage.
     *
     * <p>Usage:
     *
     * <ul>
     *   <li>args[0] : "-appJar"
     *   <li>args[1] : something like "c:/temp/testdata/java_cup.jar"
     *   <li>args[2] : "-sig"
     *   <li>args[3] : a method signature like "java_cup.lexer.advance()V"
     * </ul>
     *
     * @throws UnsupportedOperationException if command-line is malformed.
     */
    static void validateCommandLine(String[] args) {
        if (args.length != 4) {
            throw new UnsupportedOperationException("must have at exactly 4 command-line arguments");
        }
        if (!args[0].equals("-appJar")) {
            throw new UnsupportedOperationException(
                    "invalid command-line, args[0] should be -appJar, but is " + args[0]);
        }
        if (!args[2].equals("-sig")) {
            throw new UnsupportedOperationException(
                    "invalid command-line, args[2] should be -sig, but is " + args[0]);
        }
    }
}