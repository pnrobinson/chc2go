package org.jax.chc2go;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.jax.chc2go.chc.ChcInteraction;
import org.jax.chc2go.chc.ChcInteractionParser;
import org.jax.chc2go.go.PairWiseGoSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Parameter(names = {"-h", "--help"}, help = true, description = "display this help message")
    private boolean usageHelpRequested;

    @Parameter(names = {"-c", "--chc"}, required = true, description = "path to CHC interaction file")
    private String chcInteractionPath;

    @Parameter(names = {"-g", "--go"}, required = true, description = "path to go.obo file")
    private String goOboPath;

    @Parameter(names = {"-a", "--gaf"}, required = true, description = "path to GAF file")
    private String goGafPath;

    public static void main(String [] args) {
        Main main = new Main();

        JCommander.newBuilder()
                .addObject(main)
                .build().
                parse(args);
        
      main.run();

    }

    public Main() {
    }

    public void run() {
        ChcInteractionParser parser = new ChcInteractionParser(this.chcInteractionPath);
        List<ChcInteraction> chcInteractionList = parser.getInteractions();
        PairWiseGoSimilarity psim = new PairWiseGoSimilarity(chcInteractionList,goOboPath,goGafPath);
    }


}
