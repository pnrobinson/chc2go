package org.jax.chc2go;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.jax.chc2go.chc.ChcInteraction;
import org.jax.chc2go.chc.ChcInteractionParser;
import org.jax.chc2go.command.Chc2GoCommand;
import org.jax.chc2go.command.DownloadCommand;
import org.jax.chc2go.command.StatsCommand;
import org.jax.chc2go.go.PairWiseGoSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Parameter(names = {"-h", "--help"}, help = true, description = "display this help message")
    private boolean usageHelpRequested;



    public static void main(String[] args) {
        Main main = new Main();
        StatsCommand stats = new StatsCommand();
        DownloadCommand download = new DownloadCommand();

        JCommander jc = JCommander.newBuilder()
                .addObject(main)
                .addCommand("download", download)
                .addCommand("stats", stats)
                .build();

        try {
            jc.parse(args);
        } catch (ParameterException pe) {
            System.err.printf("[ERROR] Could not start chc2go: %s\n", pe.getMessage());
            System.exit(1);
        }
        if (main.usageHelpRequested) {
            jc.usage();
            System.exit(0);
        }
        String command = jc.getParsedCommand();
        if (command == null) {
            System.err.println("[ERROR] no command passed");
            System.err.println(jc.toString());
            System.exit(1);
        }
        Chc2GoCommand chc2goCommand = null;
        switch (command) {
            case "download":
                chc2goCommand= download;
                break;
            case "stats":
                chc2goCommand = stats;
                break;
        }
        chc2goCommand.run();

    }

    private Main() {
    }

    /**
     * Parse the interaction file, the two Gene Ontology files, and analyze the data.
     */
    private void run() {

    }


}
