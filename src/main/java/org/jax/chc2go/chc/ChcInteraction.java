package org.jax.chc2go.chc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents one Capture Hi-C (CHC) interaction, i.e., two
 * restriction digests that have been found to have CHC interactions (readpairs).
 * We parse in a file that is created on the basis of Diachromatic data
 * and that lists the genes contained within the individual digests, as
 * well as the nature of the interactions (directed, undirected).
 */
public class ChcInteraction {

    enum InteractionType {
        TWISTED,
        SIMPLE,
        UNDIRECTED,
        UNKNOWN,
        NO_GENE_IN_DIGEST
    }

    private final String posA;
    private final String posB;
    private final int distance;
    private final InteractionType itype;
    private final List<String> genelistA;
    private final List<String> genelistB;


    public String getPosA() {
        return posA;
    }

    public String getPosB() {
        return posB;
    }

    public int getDistance() {
        return distance;
    }

    public InteractionType getItype() {
        return itype;
    }

    public List<String> getGenelistA() {
        return genelistA;
    }

    public List<String> getGenelistB() {
        return genelistB;
    }

    public ChcInteraction(String[] pos, int distance, String cat, String[] genes) {
        if (pos.length != 2) {
            throw new RuntimeException("Malformed position String with " + pos.length + "fields");
        }
        posA = pos[0];
        posB = pos[1];
        this.distance = distance;
        genelistA = new ArrayList<>();
        genelistB = new ArrayList<>();
        if (genes.length == 2) {
            switch (cat) {
                case "U":
                    itype = InteractionType.UNDIRECTED;
                    break;
                case "T":
                    itype = InteractionType.TWISTED;
                    break;
                case "S":
                    itype = InteractionType.SIMPLE;
                    break;
                default:
                    itype = InteractionType.UNKNOWN;
            }
            String[] genelst = genes[0].split(",");
            for (String g : genelst) {
                genelistA.add(g.trim());
            }
            genelst = genes[1].split(",");
            for (String g : genelst) {
                genelistB.add(g.trim());
            }
        } else if (genes.length < 2) {
            itype = InteractionType.NO_GENE_IN_DIGEST;
        } else {
            // should really never happen!
            throw new RuntimeException("Malformed genes string with >2 fields");
        }

    }


}
