package org.jax.chc2go.io;

import com.beust.jcommander.Parameter;

public class Settings {
    @Parameter(names = {"-h", "--help"}, help = true, description = "display this help message")
    private boolean usageHelpRequested;

    @Parameter(names = {"-c", "--chc"}, required = true, description = "path to CHC interaction file")
    private String chcInteractionPath;
}
