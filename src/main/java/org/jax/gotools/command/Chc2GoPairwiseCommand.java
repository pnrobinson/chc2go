package org.jax.gotools.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jax.gotools.chc.ChcInteraction;
import org.jax.gotools.chc.ChcInteractionParser;
import org.jax.gotools.go.PairWiseGoSimilarity;

import java.util.List;
@Parameters(commandDescription = "Run pairwise analysis of diachromatic output files")
public class Chc2GoPairwiseCommand extends GoToolsCommand {

    @Parameter(names = {"-c", "--chc"}, description = "path to CHC interaction file", required = true)
    protected String chcInteractionPath = null;

    @Override
    public void run() {
        initGoPathsToDefault();
        ChcInteractionParser parser = new ChcInteractionParser(this.chcInteractionPath);
        List<ChcInteraction> chcInteractionList = parser.getInteractions();
        PairWiseGoSimilarity psim = new PairWiseGoSimilarity(chcInteractionList, goOboPath, goGafPath);
        psim.analyzePairwiseSimilarities();
    }
}
