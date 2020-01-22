package org.jax.gotools;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.jax.gotools.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Parameter(names = {"-h", "--help"}, help = true, description = "display this help message")
    private boolean usageHelpRequested;



    public static void main(String[] args) {
        Main main = new Main();
        GoToolsCommand download = new DownloadCommand();
        GoToolsCommand chc2go = new Chc2GoPairwiseCommand();
        GoToolsCommand overrep = new Chc2GoOverrepCommand();
        GoToolsCommand tf = new TfCommand();
        GoTableCommand table = new GoTableCommand();

        JCommander jc = JCommander.newBuilder()
                .addObject(main)
                .addCommand("download", download)
                .addCommand("chc2go", chc2go)
                .addCommand("tf", tf)
                .addCommand("overrep", overrep)
                .addCommand("table", table)
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
        GoToolsCommand goToolsCommand = null;
        switch (command) {
            case "download":
                goToolsCommand= download;
                break;
            case "overrep":
                goToolsCommand = overrep;
                break;
            case "tf":
                goToolsCommand = tf;
                break;
            case "chc2go":
                goToolsCommand = chc2go;
                break;
            case "table":
                goToolsCommand = table;
                break;
            default:
                System.err.println("[ERROR] Did not recognize command: "+ command);
                jc.usage();
                System.exit(0);
        }
        goToolsCommand.run();

    }

    private Main() {
    }

    /**
     * Parse the interaction file, the two Gene Ontology files, and analyze the data.
     */
    private void run() {

    }


}
