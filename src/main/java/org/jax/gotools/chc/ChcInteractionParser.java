package org.jax.gotools.chc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChcInteractionParser {
    private static final Logger logger = LoggerFactory.getLogger(ChcInteractionParser.class);
    private final File chcInteractionFile;
    private final List<ChcInteraction> interactionList;

    private int n_interactions_with_no_genes = 0;
    /** Some interactions are like this: Hic1,Mir212,Mir132;  i.e., only the first member of the pair has genes.
     * We cannot use this for the functional analysis. This variable counts how many times this happens.
     */
    private int n_interactions_with_only_one_pair_with_genes = 0;


    public ChcInteractionParser(String path) {
        if (path == null || path.isEmpty()) {
            System.err.println("[ERROR] Need to pass valid pass to CHC interaction file (diachromatic).");
            throw new RuntimeException("Need to pass valid pass to CHC interaction file (diachromatic).");
        }
        this.chcInteractionFile = new File(path);
        interactionList = new ArrayList<>();
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
            String[] fields = line.split("\t");
            //System.out.println(line);
            //System.out.println(fields.length);
            if (fields.length != 6) {
                System.err.printf("[ERROR] Malformed line with %d fields: %s.\n", fields.length, line);
                continue;
            }
            String[] pos = fields[0].split(";");
            int distance = Integer.parseInt(fields[1]);
            String category = fields[2];
            String[] genes = fields[3].split(";");
            String[] ratio = fields[4].split(":");
            String typus = fields[5];
            if (genes.length == 0) {
                n_interactions_with_no_genes++;
                continue;
            } else if (genes.length == 1) {
                n_interactions_with_only_one_pair_with_genes++;
                continue;
            }
            try {
                ChcInteraction chci = new ChcInteraction(pos, distance, category, genes, ratio, typus);
                interactionList.add(chci);
            } catch (Exception e) {
                System.out.println("Could not parse line\n" + line);
                e.printStackTrace();
            }
        }
        System.out.printf("[INFO] Parsed a total of %d interactions from %s.\n",
                interactionList.size(),chcInteractionFile.getAbsolutePath());
    }

    public List<ChcInteraction> getInteractions() {
        return this.interactionList;
    }
}
