import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.properties.WalaProperties;
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
import java.util.Properties;
import java.util.function.Predicate;

public class PDFSDG {
    public static void main(String[] args) {
        run(args);
    }

    public static Process run(String[] args) {
        Properties p = CommandLine.parse(args);
        return run(p.getProperty("appJar"), p.getProperty("mainClass"), getDataDependenceOptions(p), getControlDependenceOptions(p));
    }

    public static Slicer.DataDependenceOptions getDataDependenceOptions(Properties p) {
        String d = p.getProperty("dd", "full");
        for (Slicer.DataDependenceOptions result : Slicer.DataDependenceOptions.values()) {
            if (d.equals(result.getName())) {
                return result;
            }
        }
        Assertions.UNREACHABLE("unknown data datapendence option: " + d);
        return null;
    }

    public static Slicer.ControlDependenceOptions getControlDependenceOptions(Properties p) {
        String d = p.getProperty("cd", "full");
        for (Slicer.ControlDependenceOptions result : Slicer.ControlDependenceOptions.values()) {
            if (d.equals(result.getName())) {
                return result;
            }
        }
        Assertions.UNREACHABLE("unknown control datapendence option: " + d);
        return null;
    }

    public static Process run(String appJar, String mainClass, Slicer.DataDependenceOptions dOptions, Slicer.ControlDependenceOptions cOptions) {
        try {
            AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("input/Hello.jar", new File("Exclusions.txt"));

            ClassHierarchy cha = ClassHierarchyFactory.make(scope);
            Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, mainClass);
            AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

            CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
            CallGraph cg = builder.makeCallGraph(options, null);
            final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
            SDG<?> sdg = new SDG<>(cg, pointerAnalysis, dOptions, cOptions);
            GraphIntegrity.check(sdg);

            System.err.println(sdg);

            Properties p = WalaExamplesProperties.loadProperties();
            p.putAll(WalaProperties.loadProperties());

            String pdfFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + "sdg.pdf";

            String dotExe = "dot";
            Graph<Statement> g = pruneSDG(sdg);
            DotUtil.dotify(g, makeNodeDecorator(), PDFTypeHierarchy.DOT_FILE, pdfFile, dotExe);

            String gvExe = "open";
            return PDFViewUtil.launchPDFView(pdfFile, gvExe);
        } catch (IOException | CallGraphBuilderCancelException | GraphIntegrity.UnsoundGraphException | WalaException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Graph<Statement> pruneSDG(final SDG<?> sdg) {
        Predicate<Statement> f =
                statement -> {
                    if (statement.getNode().equals(sdg.getCallGraph().getFakeRootNode())) {
                        return false;
                    } else if (statement instanceof MethodExitStatement || statement instanceof MethodEntryStatement) {
                        return false;
                    } else {
                        return true;
                    }
                };
        return GraphSlicer.prune(sdg, f);
    }

    private static NodeDecorator<Statement> makeNodeDecorator() {
        return s -> {
            switch (s.getKind()) {
                case HEAP_PARAM_CALLEE:
                case HEAP_PARAM_CALLER:
                case HEAP_RET_CALLEE:
                case HEAP_RET_CALLER:
                    HeapStatement h = (HeapStatement) s;
                    return s.getKind() + "\\n" + h.getNode() + "\\n" + h.getLocation();
                case EXC_RET_CALLEE:
                case EXC_RET_CALLER:
                case NORMAL:
                case NORMAL_RET_CALLEE:
                case NORMAL_RET_CALLER:
                case PARAM_CALLEE:
                case PARAM_CALLER:
                case PHI:
                default:
                    return s.toString();
            }
        };
    }
}
