package org.jax.gotools.command;


import picocli.CommandLine;

import java.io.File;

public abstract class GoToolsCommand {

    @CommandLine.Option(names = {"-g", "--go"},
            description = "path to go.obo file",
            scope = CommandLine.ScopeType.INHERIT)
    protected String goOboPath = "data/go.obo";

    @CommandLine.Option(names = {"-d", "--data"},
            description = "path to data download file",
            scope = CommandLine.ScopeType.INHERIT)
    protected String dataDir = "data";


    @CommandLine.Option(names = {"-a", "--gaf"},
        description = "path to GAF file",
        scope = CommandLine.ScopeType.INHERIT)
    protected String goGafPath = "data/goa_human.gaf";

    @CommandLine.Option(names={"--outfile"},
        description = "path to output file",
        scope = CommandLine.ScopeType.INHERIT)
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
