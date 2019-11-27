package org.jax.gotools.command;

import org.jax.gotools.chc.ChcInteraction;
import org.jax.gotools.chc.ChcInteractionParser;
import org.jax.gotools.go.PairWiseGoSimilarity;

import java.util.List;

public class StatsCommand extends Chc2GoCommand {

    @Override
    public void run() {
        ChcInteractionParser parser = new ChcInteractionParser(this.chcInteractionPath);
        List<ChcInteraction> chcInteractionList = parser.getInteractions();
        PairWiseGoSimilarity psim = new PairWiseGoSimilarity(chcInteractionList, goOboPath, goGafPath);
        psim.analyzePairwiseSimilarities();
    }
}
