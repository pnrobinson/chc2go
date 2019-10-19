package org.jax.chc2go.command;


import com.beust.jcommander.Parameter;

public abstract class Chc2GoCommand {

    @Parameter(names = {"-c", "--chc"}, description = "path to CHC interaction file")
    protected String chcInteractionPath;

    @Parameter(names = {"-g", "--go"}, description = "path to go.obo file")
    protected String goOboPath;

    @Parameter(names = {"-a", "--gaf"},  description = "path to GAF file")
    protected String goGafPath;

    public abstract void run();

}
