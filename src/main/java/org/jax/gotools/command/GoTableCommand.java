package org.jax.gotools.command;


import org.jax.gotools.analysis.GoTable;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "table", aliases = {"T"},
        mixinStandardHelpOptions = true,
        description = "Create a table of genes annotated to one or more GO terms")
public class GoTableCommand extends GoToolsCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-t", "--terms"}, description = "comma-separated list of GO Ids")
    private String goIdList;

    @CommandLine.Option(names={"--gene"}, description = "path to Homo_sapiens_gene_info.gz")
    private String pathToGeneInfo;

    @CommandLine.Option(names={"--mim"}, description = "mim2gene_medgen")
    private String pathToMim2gene;

    @CommandLine.Option(names = {"-d", "--data"}, description = "path to data download file")
    protected String dataDir = "data";

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
        goId2Label.put(mRNASplicing,"S");//"mRNA splicing, via spliceosome"
        TermId regulation  = TermId.of("GO:0048024");
        goId2Label.put(regulation,"R");//regulation of mRNA splicing, via spliceosome
        return goId2Label;
    }

    @Override
    public Integer call( ) {
        goId2Label = spliceMap();
        initGoPathsToDefault(this.dataDir);
        if (pathToGeneInfo == null) {
            pathToGeneInfo = String.format("%s%s%s", this.dataDir, File.separator, "Homo_sapiens_gene_info.gz");
        }
        if (pathToMim2gene == null) {
            pathToMim2gene = String.format("%s%s%s", this.dataDir, File.separator, "mim2gene_medgen");
        }
        print_params();
        GoTable table = new GoTable(this.goOboPath, this.goGafPath, this.pathToGeneInfo, this.pathToMim2gene, goId2Label);
        String outf = "splicing-relevant.tex";
        table.outputLatexLongTableToFile(outf);
        String outname = "splice-relevant-genes.txt";
        table.outputTSV(outname,goId2Label);
        return 0;
    }



    private void print_params() {
        System.out.printf("[INFO] -g: %s\n", this.goOboPath);
        System.out.printf("[INFO] -a: %s\n", this.goGafPath);
        System.out.printf("[INFO] -t: %s\n", this.goIdList);
    }

}
