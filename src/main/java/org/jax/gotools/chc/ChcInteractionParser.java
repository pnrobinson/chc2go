package org.jax.gotools.chc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;
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
            if (path.endsWith("gz")) {
                parseGZ();
            } else {
                parse();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * A typical line has 7 fields:
     * [0] chr14:100952105-100959144;chr14:101555648-101573263 location of the two digests
     * [1] 596504 distance
     * [2] U (NA, U, URII, URAI, URA, S or T)
     * [3] SNORD114-4,SNORD114-6,SNORD114-3,SNORD114-5;DIO3OS,DIO3,MIR1247 genes on the two digests
     * [4]28:19 counts (twisted:simple)
     * [5] IA
     * [6] 2.11 log10(P-Wert)
     * @param line
     */
    private void processLine(String line) {
        String[] fields = line.split("\t");
       // System.out.println(line);
       // System.out.println(fields.length);

        if (fields.length != 7) {
            System.err.printf("[ERROR] Malformed line with %d fields: %s.\n", fields.length, line);
            return;
        }
        String[] pos = fields[0].split(";");
        int distance = Integer.parseInt(fields[1]);
        String category = fields[2];
        String[] genes = fields[3].split(";");
        String[] ratio = fields[4].split(":");
        String typus = fields[5];
        double logPval = Double.parseDouble(fields[6]);
        if (genes.length == 0) {
            n_interactions_with_no_genes++;
            return;
        } else if (genes.length == 1) {
            n_interactions_with_only_one_pair_with_genes++;
            return;
        }
        try {
            ChcInteraction chci = new ChcInteraction(pos, distance, category, genes, ratio, typus, logPval);
            interactionList.add(chci);
        } catch (Exception e) {
            System.out.println("Could not parse line\n" + line);
            e.printStackTrace();
        }
    }


    /** Parse a file such as JAV_ACD4_RALT_0.0019_interactions_with_genesymbols.tsv.gz
    */
     private void parseGZ() throws IOException {
         InputStream fileStream = new FileInputStream(this.chcInteractionFile);
         InputStream gzipStream = new GZIPInputStream(fileStream);
         Reader decoder = new InputStreamReader(gzipStream);
         BufferedReader br = new BufferedReader(decoder);
         String line;
         int i = 0;
         System.out.println();
         while ((line=br.readLine()) != null) {
             processLine(line);
             i++;
             if (i%50_000==0) {
                 System.out.printf("\rProcessed %d interactions.", i);
             }
         }
         System.out.println();
         logger.info("Parsed a total of {} interactions from {}.\n",
                 interactionList.size(),chcInteractionFile.getAbsolutePath());

    }

    private void parse() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(chcInteractionFile));
        String line;
        while ((line=br.readLine()) != null) {
            processLine(line);
        }
        logger.info("Parsed a total of {} interactions from {}.\n",
                interactionList.size(),chcInteractionFile.getAbsolutePath());
    }

    public List<ChcInteraction> getInteractions() {
        return this.interactionList;
    }
}
