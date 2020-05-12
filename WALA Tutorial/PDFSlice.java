import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFSDG;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.util.CallGraphSearchUtil;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import static com.ibm.wala.examples.drivers.PDFSlice.getReturnStatementForCall;

public class PDFSlice {
    public static void main(String[] args) {
        run(args);
    }

    public static Process run(String[] args) {
        Properties p = CommandLine.parse(args);
        validateCommandLine(p);
        return run(
                p.getProperty("appJar"),
                p.getProperty("mainClass"),
                p.getProperty("srcCaller"),
                p.getProperty("srcCallee"),
                goBackward(p),
                PDFSDG.getDataDependenceOptions(p),
                PDFSDG.getControlDependenceOptions(p));
    }

    public static boolean goBackward(Properties p) {
        return !p.getProperty("dir", "backward").equals("forward");
    }

    public static Process run(
            String appJar,
            String mainClass,
            String srcCaller,
            String srcCallee,
            boolean goBackward,
            Slicer.DataDependenceOptions dOptions,
            Slicer.ControlDependenceOptions cOptions) {
        try {
            File exFile = new FileProvider().getFile("Exclusions.txt");
            AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("input/Hello.jar", exFile);

            ClassHierarchy cha = ClassHierarchyFactory.make(scope);
            Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, mainClass);
            AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
            CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
            CallGraph cg = builder.makeCallGraph(options, null);

            SDG<InstanceKey> sdg = new SDG<>(cg, builder.getPointerAnalysis(), dOptions, cOptions);

            CGNode callerNode = CallGraphSearchUtil.findMethod(cg, srcCaller);
            Statement s = SlicerUtil.findCallTo(callerNode, srcCallee);
            System.err.println("Statement: " + s);

            Collection<Statement> slice = null;
            if (goBackward) {
                final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
                slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, dOptions, cOptions);
            } else {
                // for forward slices ... we actually slice from the return value of
                // calls.
                s = getReturnStatementForCall(s);
                final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
                slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis, dOptions, cOptions);
            }
            SlicerUtil.dumpSlice(slice);

            Graph<Statement> g = pruneSDG(sdg, slice);

            sanityCheck(slice, g);

            Properties p = WalaExamplesProperties.loadProperties();
            p.putAll(WalaProperties.loadProperties());

            String psFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + "slice.pdf";
            String dotExe = "dot";
            DotUtil.dotify(g, makeNodeDecorator(), PDFTypeHierarchy.DOT_FILE, psFile, dotExe);

            String gvExe = "open";
            return PDFViewUtil.launchPDFView(psFile, gvExe);


        } catch (IOException | CancelException | WalaException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void sanityCheck(Collection<Statement> slice, Graph<Statement> g) {
        try {
            GraphIntegrity.check(g);
        } catch (GraphIntegrity.UnsoundGraphException e1) {
            e1.printStackTrace();
            Assertions.UNREACHABLE();
        }
        Assertions.productionAssertion(
                g.getNumberOfNodes() == slice.size(), "panic " + g.getNumberOfNodes() + " " + slice.size());
    }

    public static Graph<Statement> pruneSDG(SDG<InstanceKey> sdg, final Collection<Statement> slice) {
        return GraphSlicer.prune(sdg, slice::contains);
    }

    public static NodeDecorator<Statement> makeNodeDecorator() {
        return s -> {
            switch (s.getKind()) {
                case HEAP_PARAM_CALLEE:
                case HEAP_PARAM_CALLER:
                case HEAP_RET_CALLEE:
                case HEAP_RET_CALLER:
                    HeapStatement h = (HeapStatement) s;
                    return s.getKind() + "\\n" + h.getNode() + "\\n" + h.getLocation();
                case NORMAL:
                    NormalStatement n = (NormalStatement) s;
                    return n.getInstruction() + "\\n" + n.getNode().getMethod().getSignature();
                case PARAM_CALLEE:
                    ParamCallee paramCallee = (ParamCallee) s;
                    return s.getKind()
                            + " "
                            + paramCallee.getValueNumber()
                            + "\\n"
                            + s.getNode().getMethod().getName();
                case PARAM_CALLER:
                    ParamCaller paramCaller = (ParamCaller) s;
                    return s.getKind()
                            + " "
                            + paramCaller.getValueNumber()
                            + "\\n"
                            + s.getNode().getMethod().getName()
                            + "\\n"
                            + paramCaller.getInstruction().getCallSite().getDeclaredTarget().getName();
                case EXC_RET_CALLEE:
                case EXC_RET_CALLER:
                case NORMAL_RET_CALLEE:
                case NORMAL_RET_CALLER:
                case PHI:
                default:
                    return s.toString();
            }
        };
    }

    static void validateCommandLine(Properties p) {
        if (p.get("appJar") == null) {
            throw new UnsupportedOperationException("expected command-line to include -appJar");
        }
        if (p.get("mainClass") == null) {
            throw new UnsupportedOperationException("expected command-line to include -mainClass");
        }
        if (p.get("srcCallee") == null) {
            throw new UnsupportedOperationException("expected command-line to include -srcCallee");
        }
        if (p.get("srcCaller") == null) {
            throw new UnsupportedOperationException("expected command-line to include -srcCaller");
        }
    }
}
