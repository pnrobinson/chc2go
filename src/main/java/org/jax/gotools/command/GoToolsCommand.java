package org.jax.gotools.command;


import com.beust.jcommander.Parameter;

import java.io.File;

public abstract class GoToolsCommand {


    @Parameter(names = {"-g", "--go"}, description = "path to go.obo file")
    protected String goOboPath = "data/go.obo";

    @Parameter(names = {"-a", "--gaf"},  description = "path to GAF file")
    protected String goGafPath = "data/goa_human.gaf";

    @Parameter(names = {"-d", "--data"}, description = "path to data download file")
    protected String dataDir = "data";

    @Parameter(names={"--outfile"}, description = "path to output file")
    protected String outfile = "gotools_results.txt";

    public abstract void run();

    protected void initGoPathsToDefault() {
        if (goOboPath == null) {
            goOboPath = String.format("%s%s%s", this.dataDir, File.separator, "go.obo");
        }
        if (goGafPath == null) {
            goGafPath = String.format("%s%s%s", this.dataDir, File.separator, "goa_human.gaf");
        }
    }

}
