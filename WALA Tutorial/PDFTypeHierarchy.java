import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

public class PDFTypeHierarchy {

    public static void main(String[] args) {
        run(args);
    }

    //This method view Class and Method
    public static void test() {
        try {
            File exFile = new FileProvider().getFile("Exclusions.txt");
            AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("input/Hello.jar", exFile);
            IClassHierarchy cha = ClassHierarchyFactory.make(scope);
            for (IClass c : cha) {
                String cname = c.getName().toString();
                System.out.println("Class:" + cname);
                for (IMethod m : c.getAllMethods()) {
                    String mname = m.getName().toString();
                    System.out.println(" method:" + mname);
                }
                System.out.println();
            }
        } catch (IOException | ClassHierarchyException e) {
            e.printStackTrace();
        }
    }

    public static Process run(String[] args) {
        File exFile = null;
        try {
            exFile = new FileProvider().getFile("Exclusions.txt");
            AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("input/Hello.jar", exFile);

            ClassHierarchy cha = ClassHierarchyFactory.make(scope);

            Graph<IClass> g = typeHierarchy2Graph(cha);

            g = pruneForAppLoader(g);

            String dotFile = File.createTempFile("out", ".dt").getAbsolutePath();
            String pdfFile = File.createTempFile("out", ".pdf").getAbsolutePath();
            String dotExe = "dot";
            String gvExe = "open";
            DotUtil.dotify(g, null, dotFile, pdfFile, dotExe);
            return PDFViewUtil.launchPDFView(pdfFile, gvExe);
        } catch (IOException | WalaException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
        Collection<T> slice = GraphSlicer.slice(g, f);
        return GraphSlicer.prune(g, new CollectionFilter<>(slice));
    }

    public static Graph<IClass> pruneForAppLoader(Graph<IClass> g) {
        Predicate<IClass> f = new Predicate<IClass>() {
            @Override
            public boolean test(IClass iClass) {
                return (iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application));
            }
        };
        return pruneGraph(g, f);
    }

    public static Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) {
        Graph<IClass> result = SlowSparseNumberedGraph.make();
        for (IClass c : cha) {
            result.addNode(c);
        }
        for (IClass c : cha) {
            for (IClass x : cha.getImmediateSubclasses(c)) {
                result.addEdge(c, x);
            }
            if (c.isInterface()) {
                for (IClass x : cha.getImplementors(c.getReference())) {
                    result.addEdge(c, x);
                }
            }
        }
        return result;
    }
}
