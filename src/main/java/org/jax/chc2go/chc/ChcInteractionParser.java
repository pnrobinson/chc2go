package org.jax.chc2go.chc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChcInteractionParser {

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
            if (fields.length < 4) {
                System.err.printf("[ERROR] Malformed line with %d fields (expected 4)", fields.length);
            }
            String[] pos = fields[0].split(";");
            int distance = Integer.parseInt(fields[1]);
            String category = fields[2];
            String[] genes = fields[3].split(";");
            try {
                ChcInteraction chci = new ChcInteraction(pos, distance, category, genes);
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
