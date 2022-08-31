package org.jax.gotools;

import org.jax.gotools.command.*;

import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(name = "GOtools", mixinStandardHelpOptions = true,
        description = "Gene Ontology tools")
public class Main implements Callable<Integer> {

    public static void main(String [] args){
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }
        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("IC", new GoIcCompareCommand())
                .addSubcommand("asum", new AsumCommand())
                .addSubcommand("summary", new GuSummaryCommand())
                .addSubcommand("tf", new TfCommand())
                .addSubcommand("table", new GoTableCommand());
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // work done in subcommands
        return 0;
    }
}
