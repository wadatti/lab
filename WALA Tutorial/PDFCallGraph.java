import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

public class PDFCallGraph {
    public static void main(String[] args) {
        run(args);
    }

    public static Process run(String[] args) {
        Properties p = CommandLine.parse(args);
        return run(p.getProperty("appJar"), p.getProperty("exclusionFile", "Exclusions.txt"));
    }

    public static Process run(String appJar, String exclusionFile) {
        try {
            Graph<CGNode> g = buildPrunedCallGraph(appJar, (new FileProvider().getFile(exclusionFile)));

            Properties p = null;

            p = WalaExamplesProperties.loadProperties();
            p.putAll(WalaProperties.loadProperties());

            String pdfFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separator + "cg.pdf";

            String dotExe = "dot";
            DotUtil.dotify(g, null, PDFTypeHierarchy.DOT_FILE, pdfFile, dotExe);

            String gvExe = "open";
            return PDFViewUtil.launchPDFView(pdfFile, gvExe);
        } catch (WalaException | IOException | CallGraphBuilderCancelException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Graph<CGNode> buildPrunedCallGraph(String appJar, File exclusionFile) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, new File("Exclusions.txt"));

        ClassHierarchy cha = ClassHierarchyFactory.make(scope);

        Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);


        com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);

        System.err.println(CallGraphStats.getStats(cg));

        Graph<CGNode> g = pruneForAppLoader(cg);

        return g;
    }

    public static Graph<CGNode> pruneForAppLoader(CallGraph g) {
        return PDFTypeHierarchy.pruneGraph(g, new ApplicationLoaderFilter());
    }

    private static class ApplicationLoaderFilter implements Predicate<CGNode> {

        @Override
        public boolean test(CGNode cgNode) {
            if (cgNode == null) return false;
            return cgNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
        }
    }
}
