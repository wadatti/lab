import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.io.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomSyncDetector {
    public static void main(String[] args) throws IOException, ClassHierarchyException, CancelException {
        Properties p = CommandLine.parse(args);
        String inputJar = p.getProperty("appJar");

        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(inputJar, new File("Exclusions.txt"));
        ClassHierarchy cha = ClassHierarchyFactory.make(scope);

        Iterable<Entrypoint> entryPoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);

        CallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);

        System.out.println(CallGraphStats.getStats(cg));

        final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();

        Graph<CGNode> g = pruneGraph(cg, new ApplicationLoaderFilter());

        for (CGNode node : g) {
            IR ir = node.getIR();
            DefUse defUse = new DefUse(ir);
            SSACFG cfg = node.getIR().getControlFlowGraph();

            // Output CFG to PDF format
            PDFCFG pdfcfg = new PDFCFG();
            pdfcfg.create(node, cfg);

            // Collecting LoopInfo in CGNode
            AllLoopInfo allLoopInfo = AllLoopInfo.getAllLoopInfo(cfg);
            if (!allLoopInfo.hasLoop()) continue;

            //  Set of slice instruction index
            HashSet<Integer> sliceIndexes = new HashSet<>();

            // collect of condition instruction
            for (LoopInfo li : allLoopInfo.getLoops()) {
                for (int condBBNum : li.getConditionalBB()) {
                    for (SSAInstruction condBranchInst : cfg.getBasicBlock(condBBNum).getAllInstructions()) {
                        if (!condBranchInst.toString().contains("conditional branch")) continue;
                        System.out.println(condBranchInst.getUse(0));
                        System.out.println(defUse.getDef(condBranchInst.getUse(0)));
                        if (defUse.getDef(condBranchInst.getUse(0)).toString().contains("phi")) {
                            SSAInstruction phiInst = defUse.getDef(condBranchInst.getUse(0));
                            for (int i = 0; i < phiInst.getNumberOfUses(); i++) {
                                sliceIndexes.add(defUse.getDef(phiInst.getUse(i)).iIndex());
                            }
                        } else {
                            sliceIndexes.add(defUse.getDef(condBranchInst.getUse(0)).iIndex());
                        }
                    }
                }
            }

            for (int sliceIndex : sliceIndexes) {
                Collection<Statement> slice = null;
                Statement s = new NormalStatement(node, sliceIndex);
                System.out.println("Statement: " + s);

                slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, Slicer.DataDependenceOptions.FULL, Slicer.ControlDependenceOptions.NONE);
                printSliceLine(slice);
            }
        }
    }

    public static void printSliceLine(Collection<Statement> slice) {
        System.out.println("Slice result:");
        String className = null;
        String methodName = null;

        for (Statement statement : slice) {
            // extract class name
            Pattern patternClassName = Pattern.compile("L.*,");
            Matcher matcher = patternClassName.matcher(statement.getNode().toString());
            if (matcher.find()) throw new InputMismatchException();
            String tempClassName = matcher.group(0).substring(1, matcher.group(0).indexOf(","));
            if (className == null || !className.equals(tempClassName)) {
                className = tempClassName;
                System.out.println("\nclass: " + className);
            }

            // extract method name
            Pattern patternMethodName = Pattern.compile(",+\\s[^,]*\\(");
            matcher = patternMethodName.matcher(statement.getNode().toString());
            if (matcher.find()) throw new InputMismatchException();
            String tempMethodName = matcher.group(0).substring(1, matcher.group(0).indexOf("("));
            if (methodName == null || !methodName.equals(tempMethodName)) {
                methodName = tempMethodName;
                System.out.println("Method: " + methodName);
            }

            if (statement.getKind() == Statement.Kind.NORMAL) {
                int bcIndex, instructionIndex = ((NormalStatement) statement).getInstructionIndex();
                try {
                    if (!((statement.getNode().getMethod()) instanceof ShrikeBTMethod)) continue;
                    bcIndex = ((ShrikeBTMethod) statement.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                    int srcLineNumber = statement.getNode().getMethod().getLineNumber(bcIndex);
                    System.out.println(((NormalStatement) statement).getInstruction());
                    System.out.println("line: " + srcLineNumber);
                } catch (InvalidClassFileException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
        Collection<T> slice = GraphSlicer.slice(g, f);
        return GraphSlicer.prune(g, new CollectionFilter<>(slice));
    }

    private static class ApplicationLoaderFilter implements Predicate<CGNode> {
        @Override
        public boolean test(CGNode cgNode) {
            if (cgNode == null) return false;
            return cgNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
        }
    }
}
