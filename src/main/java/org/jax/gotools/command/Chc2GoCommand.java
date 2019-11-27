package org.jax.gotools.command;


import com.beust.jcommander.Parameter;

public abstract class Chc2GoCommand {

    @Parameter(names = {"-c", "--chc"}, description = "path to CHC interaction file")
    protected String chcInteractionPath = null;

    @Parameter(names = {"-g", "--go"}, description = "path to go.obo file")
    protected String goOboPath = null;

    @Parameter(names = {"-a", "--gaf"},  description = "path to GAF file")
    protected String goGafPath = null;

    @Parameter(names = {"-d", "--data"}, description = "path to data download file")
    protected String dataDir = "data";

    public abstract void run();

}
