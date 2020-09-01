import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.viz.DotUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomSyncDetector {
    public static void main(String[] args) throws IOException, WalaException, CancelException {
        File instrumentInfo = new File("instrumentInfo.csv");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(instrumentInfo, false));) {
        }

        Properties p = CommandLine.parse(args);
        String inputJar = p.getProperty("appJar");

        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(inputJar, new File("Exclusions.txt"));
        ClassHierarchy cha = ClassHierarchyFactory.make(scope);

        Iterable<Entrypoint> entryPoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);

        CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneContainerCFABuilder(options, new AnalysisCacheImpl(), cha, scope);
//        CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);

        CallGraph cg = builder.makeCallGraph(options, null);

        System.out.println(CallGraphStats.getStats(cg));

        final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();

        Graph<CGNode> g = pruneGraph(cg, new ApplicationLoaderFilter());
        String pdfFile = "cg.pdf";
        String dotExe = "dot";
        DotUtil.dotify(g, null, PDFTypeHierarchy.DOT_FILE, pdfFile, dotExe);


        // Output CFG to PDF format
        for (CGNode node : g) {
            SSACFG cfg = node.getIR().getControlFlowGraph();
            PDFCFG pdfcfg = new PDFCFG();
            pdfcfg.create(node, cfg);
            RPCMethods rpcMethod = null;
            if ((rpcMethod = containsRPCMethods(node.getMethod().getName().toString())) != null)
                sliceRPCReturnValue(node, cg, cfg, pointerAnalysis, rpcMethod);
        }

        for (CGNode node : g) {
            SSACFG cfg = node.getIR().getControlFlowGraph();

            // Collecting LoopInfo in CGNode
            AllLoopInfo allLoopInfo = AllLoopInfo.getAllLoopInfo(cfg);
            if (!allLoopInfo.hasLoop()) continue;

            NonScalableLoopDetector detector = new NonScalableLoopDetector(cg, pointerAnalysis);
            Set<LoopInfo> nonScalableLoops = detector.detect(allLoopInfo, cfg, node);

            //  Set of slice instruction index
            HashMap<LoopInfo, HashSet<Integer>> sliceIndexes = new HashMap<>();

            // collect of condition instruction
            for (LoopInfo li : allLoopInfo.getLoops()) {
                for (int condBBNum : li.getConditionalBB()) {
                    for (SSAInstruction condBranchInst : cfg.getBasicBlock(condBBNum).getAllInstructions()) {
                        if (!condBranchInst.toString().contains("conditional branch")) continue;
                        IR ir = node.getIR();
                        DefUse defUse = new DefUse(ir);
                        System.out.println(condBranchInst.getUse(0));
                        System.out.println(defUse.getDef(condBranchInst.getUse(0)));
                        if (defUse.getDef(condBranchInst.getUse(0)).toString().contains("phi")) {   // check conditional instruction uses instruction
                            // Phi Instruction
                            SSAInstruction phiInst = defUse.getDef(condBranchInst.getUse(0));
                            for (int i = 0; i < phiInst.getNumberOfUses(); i++) {
                                if (sliceIndexes.containsKey(li)) {
                                    if (ir.getSymbolTable().isConstant(phiInst.getUse(i))) continue;
                                    sliceIndexes.get(li).add(defUse.getDef(phiInst.getUse(i)).iIndex());
                                } else {
                                    HashSet<Integer> indexes = new HashSet<>();
                                    indexes.add(defUse.getDef(phiInst.getUse(i)).iIndex());
                                    sliceIndexes.put(li, indexes);
                                }
                            }
                        } else {
                            // Other Instruction
                            if (sliceIndexes.containsKey(li)) {
                                sliceIndexes.get(li).add(defUse.getDef(condBranchInst.getUse(0)).iIndex());
                            } else {
                                HashSet<Integer> indexes = new HashSet<>();
                                indexes.add(defUse.getDef(condBranchInst.getUse(0)).iIndex());
                                sliceIndexes.put(li, indexes);
                            }
                        }
                    }
                }
            }

            // compute backward slice (conditional instruction)
            for (Map.Entry<LoopInfo, HashSet<Integer>> e : sliceIndexes.entrySet()) {
                LoopInfo li = e.getKey();
                HashSet<Integer> indexSet = e.getValue();
                for (int sliceIndex : indexSet) {
                    Statement s = new NormalStatement(node, sliceIndex);
                    if (s.toString().contains("invoke"))
                        s = new NormalReturnCaller(node, sliceIndex);
                    System.out.println("\nStatement: " + s);

                    Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, Slicer.DataDependenceOptions.FULL, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
                    RPCMethods rpcMethod = checkIncludeRPC(slice);
                    if (rpcMethod != null) {
                        for (int outBB : li.getOutNode()) {
                            for (SSAInstruction inst : cfg.getBasicBlock(outBB)) {
                                Statement outStatement = new NormalStatement(node, inst.iIndex());
                                if (outStatement.toString().contains("invoke")) {
                                    outputInstrumentPoint(rpcMethod, "PULL", outStatement);
                                }
                            }
                        }
                    }
//                    printSliceLine(slice, node);
                }
            }
        }
    }

    // debug method
    public static void printSliceLine(Collection<Statement> slice, CGNode node) {
        System.out.println("Slice result:");
        String className = null;
        String methodName = null;

        for (Statement statement : slice) {
            // extract class name
            Pattern patternClassName = Pattern.compile("L.*,");
            Matcher matcher = patternClassName.matcher(statement.getNode().toString());
            if (!matcher.find()) throw new InputMismatchException();
            String tempClassName = matcher.group(0).substring(1, matcher.group(0).indexOf(","));
            if (className == null || !className.equals(tempClassName)) {
                className = tempClassName;
                System.out.println("\nclass: " + className);
            }

            // extract method name
            Pattern patternMethodName = Pattern.compile(",+\\s[^,]*\\(");
            matcher = patternMethodName.matcher(statement.getNode().toString());
            if (!matcher.find()) throw new InputMismatchException();
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


    public static void outputInstrumentPoint(RPCMethods rpcMethod, String type, Statement s) {
        File instrumentInfo = new File("instrumentInfo.csv");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(instrumentInfo, true))) {
            // extract class name
            String className = getClassName(s);

            // extract method name
            String methodName = getMethodName(s);

            if (s.getKind() == Statement.Kind.NORMAL) {
                int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
                if (!((s.getNode().getMethod()) instanceof ShrikeBTMethod)) return;
                bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                int srcLineNumber = s.getNode().getMethod().getLineNumber(bcIndex);
                System.out.println(((NormalStatement) s).getInstruction());
                System.out.println("line: " + srcLineNumber);
                bw.write(rpcMethod + "," + type + "," + className + "," + methodName + "," + srcLineNumber);
                bw.newLine();
            }
        } catch (IOException | InvalidClassFileException e) {
            e.printStackTrace();
        }
    }

    public static void sliceRPCReturnValue(CGNode node, CallGraph cg, SSACFG cfg, PointerAnalysis<InstanceKey> pointerAnalysis, RPCMethods rpcMethod) throws CancelException {
        System.out.println("-----Slice RPC Return Value Start------");
        // collect of return instruction
        List<Integer> returnInstructionIndexes = new ArrayList<>();
        for (SSAInstruction inst : cfg.getInstructions()) {
            if (inst == null) continue;
            if (inst.toString().contains("return"))
                returnInstructionIndexes.add(inst.iIndex());
        }

        for (int returnInstructionIndex : returnInstructionIndexes) {
            Statement s = new NormalStatement(node, returnInstructionIndex);
            if (s.toString().contains("invoke"))
                s = new NormalReturnCaller(node, returnInstructionIndex);
            System.out.println("slice statement: " + s);

            Collection<Statement> slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, Slicer.DataDependenceOptions.FULL, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
            printSliceLine(slice, node);
            for (Statement sliceResult : slice) {
                if (getMethodName(sliceResult).contains(rpcMethod.getMethod())) {
                    if (sliceResult.toString().contains("invoke"))
                        outputInstrumentPoint(rpcMethod, "UPDATE", sliceResult);
                }
            }
        }
        System.out.println("-----Slice RPC Return Value End------");

//        SDG<InstanceKey> sdg = new SDG<>(cg, pointerAnalysis, ModRef.make(), Slicer.DataDependenceOptions.FULL, Slicer.ControlDependenceOptions.NONE);
//        PDG pdg = sdg.getPDG(node);
//        String pdfFile = "sdg.pdf";
//        String dotExe = "dot";
//        DotUtil.dotify(pdg, null, PDFTypeHierarchy.DOT_FILE, pdfFile, dotExe);
    }

    // slice in RPC method
    public static RPCMethods checkIncludeRPC(Collection<Statement> slice) {
        for (Statement s : slice) {
            EnumSet<RPCMethods> enumSet = EnumSet.allOf(RPCMethods.class);
            for (RPCMethods rpcMethod : enumSet) {
                if (s.toString().contains(rpcMethod.name())) return rpcMethod;
            }
        }
        return null;
    }

    public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
        Collection<T> slice = GraphSlicer.slice(g, f);
        return GraphSlicer.prune(g, new CollectionFilter<>(slice));
    }

    private static RPCMethods containsRPCMethods(String methodName) {
        EnumSet<RPCMethods> enumSet = EnumSet.allOf(RPCMethods.class);
        for (RPCMethods rpcMethod : enumSet) {
            if (rpcMethod.name().equals(methodName)) return rpcMethod;
        }
        return null;
    }

    private static String getMethodName(Statement statement) {
        Pattern patternMethodName = Pattern.compile(",+\\s[^,]*\\(");
        Matcher matcher = patternMethodName.matcher(statement.toString());
        if (!matcher.find()) throw new InputMismatchException();
        return matcher.group(0).substring(1, matcher.group(0).indexOf("(")).replace(" ", "").replace("/", ".");
    }

    private static String getClassName(Statement statement) {
        Pattern patternClassName = Pattern.compile("L.*,");
        Matcher matcher = patternClassName.matcher(statement.getNode().toString());
        if (!matcher.find()) throw new InputMismatchException();
        return matcher.group(0).substring(1, matcher.group(0).indexOf(","));
    }


    public static class ApplicationLoaderFilter implements Predicate<CGNode> {
        @Override
        public boolean test(CGNode cgNode) {
            if (cgNode == null) return false;
            return cgNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
        }
    }
}
