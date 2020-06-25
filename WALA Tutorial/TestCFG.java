import com.ibm.wala.analysis.exceptionanalysis.ExceptionAnalysis;
import com.ibm.wala.analysis.exceptionanalysis.ExceptionAnalysis2EdgeFilter;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter2EdgeFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.IgnoreExceptionsFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.CombinedInterproceduralExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.IgnoreExceptionsInterFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

public class TestCFG {

    private static CombinedInterproceduralExceptionFilter<SSAInstruction> filter = new CombinedInterproceduralExceptionFilter<>();


    public static void main(String[] args) throws IOException, WalaException, CallGraphBuilderCancelException, InvalidClassFileException {
        Properties p = CommandLine.parse(args);
        buildPrunedCallGraph(p.getProperty("appJar"), (new FileProvider().getFile(p.getProperty("exclusionFile", "Exclusions.txt"))));
    }

    public static void buildPrunedCallGraph(String appJar, File exclusionFile) throws IOException, WalaException, CallGraphBuilderCancelException, InvalidClassFileException {
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, new File("Exclusions.txt"));

        ClassHierarchy cha = ClassHierarchyFactory.make(scope);

        Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);


        com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);

        System.err.println(CallGraphStats.getStats(cg));

        final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();

        filter.add(
                new IgnoreExceptionsInterFilter<>(
                        new IgnoreExceptionsFilter(TypeReference.JavaLangOutOfMemoryError)));
        filter.add(
                new IgnoreExceptionsInterFilter<>(
                        new IgnoreExceptionsFilter(TypeReference.JavaLangNullPointerException)));
        filter.add(
                new IgnoreExceptionsInterFilter<>(
                        new IgnoreExceptionsFilter(TypeReference.JavaLangExceptionInInitializerError)));
        filter.add(
                new IgnoreExceptionsInterFilter<>(
                        new IgnoreExceptionsFilter(TypeReference.JavaLangNegativeArraySizeException)));

        ExceptionAnalysis analysis = new ExceptionAnalysis(cg, pointerAnalysis, cha, filter);
        analysis.solve();

        Graph<CGNode> g = pruneForAppLoader(cg);


        int num = 0;

        for (CGNode node : g) {
            IR ir = node.getIR();
            num++;
            if (node.getIR() != null && !node.getIR().isEmptyIR()) {
                EdgeFilter<ISSABasicBlock> exceptionAnalysedEdgeFilter = new ExceptionAnalysis2EdgeFilter(analysis, node);
                SSACFG cfg_orig = node.getIR().getControlFlowGraph();
                ExceptionFilter2EdgeFilter<ISSABasicBlock> filterOnlyEdgeFilter = new ExceptionFilter2EdgeFilter<>(filter.getFilter(node), cha, cfg_orig);
                ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = PrunedCFG.make(cfg_orig, filterOnlyEdgeFilter);
                ControlFlowGraph<SSAInstruction, ISSABasicBlock> exceptionPruned = PrunedCFG.make(cfg_orig, exceptionAnalysedEdgeFilter);


                Graph domiTree = null;

                AllLoopInfo loopInfo = AllLoopInfo.getAllLoopInfo(cfg_orig);

                for (int headNum : loopInfo.getHeads()) {
                    System.out.println("headBB: " + headNum);
                    for (SSAInstruction headInst : cfg_orig.getBasicBlock(headNum)) {
                        IBytecodeMethod method = (IBytecodeMethod) ir.getMethod();
                        int bytecodeIndex = method.getBytecodeIndex(headInst.iIndex());
                        int sourceLineNum = method.getLineNumber(bytecodeIndex);
                        System.out.println(headInst.toString());
                        System.out.println("line: " + sourceLineNum);
                    }
                    System.out.println();
                }

                if (loopInfo.hasLoop()) {
                    Dominators<ISSABasicBlock> dominators = Dominators.make(exceptionPruned, cfg_orig.getBasicBlock(1));
                    domiTree = dominators.dominatorTree();
                    String dotExe = "dot";
                    String psFile = "dominatorTree.pdf";
                    Properties wp = null;
                    wp = WalaProperties.loadProperties();
                    wp.putAll(WalaExamplesProperties.loadProperties());
                    String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
                    DotUtil.<ISSABasicBlock>dotify(domiTree, PDFViewUtil.makeIRDecorator(node.getIR()), dotFile, psFile, dotExe);
                }

                Properties wp = null;
                wp = WalaProperties.loadProperties();
                wp.putAll(WalaExamplesProperties.loadProperties());
                String dotExe = "dot";
                String psFile = "cfg" + num + ".pdf";
                String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;

                DotUtil.<ISSABasicBlock>dotify(exceptionPruned, PDFViewUtil.makeIRDecorator(node.getIR()), dotFile, psFile, dotExe);
            }
        }
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
