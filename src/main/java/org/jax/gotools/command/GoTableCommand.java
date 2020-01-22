package org.jax.gotools.command;

import com.beust.jcommander.Parameter;
import org.jax.gotools.analysis.GoTable;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;

public class GoTableCommand extends GoToolsCommand {

    @Parameter(names = {"-t", "--terms"}, description = "comma-separated list of GO Ids", required = false)
    protected String goIdList;

    @Parameter(names={"--infile"}, description = "path to input file", required = true)
    protected String infile;


    String goIds [] = {"GO:0043484", //regulation of RNA splicing
        "GO:0008380", //RNA splicing
            "GO:0003729", //mRNA binding
            "GO:0000395", //mRNA 5'-splice site recognition
            "GO:0006376", //mRNA splice site selection
            "GO:0006405", // RNA export from nucleus
    };

    String goLabels [] = {
            "regulation of RNA splicing",
            "RNA splicing",
            "mRNA binding",
            "mRNA 5'-splice site recognition",
            "mRNA splice site selection",
            "RNA export from nucleus"
    };

    public GoTableCommand(){

    }

    public void run( ) {
        //this.goIdList = String.join(",", goIds);
        print_params();

        List<TermId> lst = new ArrayList<>();
        for (String s : goIds) {
            lst.add(TermId.of(s));
        }


        GoTable table = new GoTable(this.goOboPath, this.goGafPath, this.infile, lst, goLabels);
        String outf = "text.tex";
        table.outputLatexTableToFile(outf);
    }



    private void print_params() {
        System.out.printf("[INFO] -g: %s\n", this.goOboPath);
        System.out.printf("[INFO] -a: %s\n", this.goGafPath);
        System.out.printf("[INFO] -t: %s\n", this.goIdList);
        System.out.printf("[INFO] ---infile: %s\n", this.infile);
    }

}
