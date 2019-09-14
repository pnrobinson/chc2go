package org.jax.chc2go;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.jax.chc2go.chc.ChcInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Parameter(names = {"-h", "--help"}, help = true, description = "display this help message")
    private boolean usageHelpRequested;

    @Parameter(names = {"-c", "--chc"}, required = true, description = "path to CHC interaction file")
    private String chcInteractionPath;



    public static void main(String [] args) {
        Main main = new Main();

        JCommander.newBuilder()
                .addObject(main)
                .build().
                parse(args);
        
        Main mn = new Main();

    }

    public Main() {
        System.out.printf("path="+chcInteractionPath);

        ChcInteraction chc = new ChcInteraction(this.chcInteractionPath);
    }


}
