package org.jax.gotools.command;


import java.io.File;

public abstract class GoToolsCommand {


//    @Parameter(names = {"-g", "--go"}, description = "path to go.obo file")
    protected String goOboPath = "data/go.obo";
//
//    @Parameter(names = {"-a", "--gaf"},  description = "path to GAF file")
    protected String goGafPath = "data/goa_human.gaf";
//
//
//
//    @Parameter(names={"--outfile"}, description = "path to output file")
    protected String outfile = "gotools_results.txt";


    protected void initGoPathsToDefault(String dataDir) {
        if (goOboPath == null) {
            goOboPath = String.format("%s%s%s", dataDir, File.separator, "go.obo");
        }
        if (goGafPath == null) {
            goGafPath = String.format("%s%s%s", dataDir, File.separator, "goa_human.gaf");
        }
    }

}
