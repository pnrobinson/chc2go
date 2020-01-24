package org.jax.gotools.command;

import com.beust.jcommander.Parameter;
import org.jax.gotools.analysis.GoTable;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoTableCommand extends GoToolsCommand {

    @Parameter(names = {"-t", "--terms"}, description = "comma-separated list of GO Ids", required = false)
    protected String goIdList;

    private Map<TermId, String> goId2Label;




    public GoTableCommand(){

    }

    /**
     * GO terms related to mRNA splicing and regulation of RNA splicing
     * @return
     */
    private Map<TermId, String> spliceMap() {
        goId2Label = new LinkedHashMap<>();
        TermId mRNASplicing = TermId.of("GO:0000398");
        goId2Label.put(mRNASplicing,"mRNA splicing");//"mRNA splicing, via spliceosome"
        TermId regulation  = TermId.of("GO:0048024");
        goId2Label.put(regulation,"regulation");//regulation of mRNA splicing, via spliceosome
        return goId2Label;
    }

    public void run( ) {
        goId2Label = spliceMap();
        print_params();
        GoTable table = new GoTable(this.goOboPath, this.goGafPath,  goId2Label);
        String outf = "splicing-relevant.tex";
        table.outputLatexLongTableToFile(outf);
    }



    private void print_params() {
        System.out.printf("[INFO] -g: %s\n", this.goOboPath);
        System.out.printf("[INFO] -a: %s\n", this.goGafPath);
        System.out.printf("[INFO] -t: %s\n", this.goIdList);
    }

}
