package org.jax.chc2go.command;

import org.jax.chc2go.chc.ChcInteraction;
import org.jax.chc2go.chc.ChcInteractionParser;
import org.jax.chc2go.go.PairWiseGoSimilarity;

import java.io.File;
import java.util.List;

public class OverrepresentationCommand extends Chc2GoCommand {
    @Override
    public void run() {
        if (this.chcInteractionPath == null) {
            System.err.println("[ERROR] CHC Interaction file path is not initialized.\n");
            System.exit(1);
        }
        ChcInteractionParser parser = new ChcInteractionParser(this.chcInteractionPath);
        List<ChcInteraction> chcInteractionList = parser.getInteractions();
        PairWiseGoSimilarity psim = new PairWiseGoSimilarity(chcInteractionList, goOboPath, goGafPath);
        psim.performOverrepresentationAnalysis();
    }




    private void setPaths() {
        if (this.goOboPath == null && this.goGafPath == null) {
            // not initialized. We use the data directory
            this.goOboPath = String.format("%s%s%s",this.dataDir, File.separator, "go.obo");
            if (! (new File(goOboPath)).exists()) {
                throw new RuntimeException("Could not find go obo file at " + goOboPath);
            }
            this.goGafPath = String.format("%s%s%s",this.dataDir, File.separator, "goa_human.gaf");
            if (! (new File(goGafPath)).exists()) {
                throw new RuntimeException("Could not find go annotation file at " + goGafPath);
            }
        }
    }
}
