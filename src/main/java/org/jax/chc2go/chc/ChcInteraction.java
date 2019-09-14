package org.jax.chc2go.chc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class represents one Capture Hi-C (CHC) interaction, i.e., two
 * restriction digests that have been found to have CHC interactions (readpairs).
 * We parse in a file that is created on the basis of Diachromatic data
 * and that lists the genes contained within the individual digests, as
 * well as the nature of the interactions (directed, undirected).
 */
public class ChcInteraction {

    private final File chcInteractionFile;


    public ChcInteraction(String path) {
        this.chcInteractionFile = new File(path);
        try {
            parse();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void parse() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(chcInteractionFile));
        String line;
        while ((line=br.readLine()) != null) {
            System.out.printf(line);
        }
    }
}
