import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.util.Properties;


/**
 * Output CFG to PDF format class
 */
public class PDFCFG {

    /**
     * create PDF file
     *
     * @param node CGNode
     * @param cfg  ControlFlowGraph
     */
    void create(CGNode node, ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg) {
        try {
            Properties p = WalaProperties.loadProperties();
            p.putAll(WalaExamplesProperties.loadProperties());
            String dotExe = "dot";
            String psFile = "cfg_" + node.getMethod().getName() + ".pdf";
            String dotFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
            DotUtil.<ISSABasicBlock>dotify(cfg, PDFViewUtil.makeIRDecorator(node.getIR()), dotFile, psFile, dotExe);
        } catch (WalaException e) {
            e.printStackTrace();
        }
    }
}
