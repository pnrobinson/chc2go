package org.jax.chc2go.command;

import org.jax.chc2go.chc.ChcInteraction;
import org.jax.chc2go.chc.ChcInteractionParser;
import org.jax.chc2go.go.PairWiseGoSimilarity;

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
