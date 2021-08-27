package org.jax.gotools.command;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "asum", aliases = {"A"},
        mixinStandardHelpOptions = true,
        description = "Annotation Summary")
public class AsumCommand extends GoToolsCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0")    String symbol;

    @Override
    public Integer call() {
        System.out.printf("[INFO] Extract annotations for %s\n", symbol);
        return null;
    }
}
