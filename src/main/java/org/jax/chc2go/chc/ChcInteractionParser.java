package org.jax.chc2go.chc;

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


    public ChcInteractionParser(String path) {
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
