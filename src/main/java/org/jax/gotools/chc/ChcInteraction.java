package org.jax.gotools.chc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>This class represents one Capture Hi-C (CHC) interaction, i.e., two
 * restriction digests that have been found to have CHC interactions (readpairs).
 * We parse in a file that is created on the basis of Diachromatic data
 * and that lists the genes contained within the individual digests, as
 * well as the nature of the interactions (directed, undirected).
 * </p>
 * <p>
 * There are six columns
 * </p>
 * <ol>
 * <li>Position strings: chr8:11861759-11870747;chr8:11995989-12003281</li>
 * <li>Distance: 125242</li>
 * <li>Category: UR</li>
 * <li>Genes CTSB;DEFB134</li>
 * <li>simple:twisted 4:5</li>
 * <li>typus AA</li>
 * </ol>
 * <p>
 *     The categories are: S: simple; T: twisted; U: undirected (all); UR: undirected reference; NA: indefinable. The
 *     typus column currently includes AA (active/active) interactions.
 * </p>
 *
 *
 */
public class ChcInteraction {
    private static final Logger logger = LoggerFactory.getLogger(ChcInteraction.class);

    public enum InteractionType {
        TWISTED,
        SIMPLE,
        UNDIRECTED,
        UNDIRECTED_REF,
        INDEFINABLE
    }

    private final String posA;
    private final String posB;
    private final int distance;
    private final InteractionType itype;
    private final List<String> genelistA;
    private final List<String> genelistB;
    private double similarity;
    private String maxGeneA;
    private String maxGeneB;
    private final int simple;
    private final int twisted;

    private final double logPval;


    public String getMaxGeneA() {
        return maxGeneA;
    }

    public String getMaxGeneB() {
        return maxGeneB;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double s, String maxGeneA, String maxGeneB) {
        this.similarity = s;
        this.maxGeneA = maxGeneA;
        this.maxGeneB = maxGeneB;
    }

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

    public ChcInteraction(String[] pos, int distance, String cat, String[] genes, String[] ratio, String typus, double logpval) {
        if (pos.length != 2) {
            throw new RuntimeException("Malformed position String with " + pos.length + "fields");
        }
        posA = pos[0];
        posB = pos[1];
        this.distance = distance;
        genelistA = new ArrayList<>();
        genelistB = new ArrayList<>();
        // The parser enforces that genes is an array with length 2
        if (genes.length == 2) {
            switch (cat) {
                case "U":
                    itype = InteractionType.UNDIRECTED;
                    break;
                case "UR":
                    itype = InteractionType.UNDIRECTED_REF;
                    break;
                case "T":
                    itype = InteractionType.TWISTED;
                    break;
                case "S":
                    itype = InteractionType.SIMPLE;
                    break;
                case "NA":
                    itype = InteractionType.INDEFINABLE;
                    break;
                default:
                    logger.error("Unexpected category abbreviation: {}", cat);
                    itype = InteractionType.INDEFINABLE;
            }
            String[] genelst = genes[0].split(",");
            for (String g : genelst) {
                genelistA.add(g.trim());
            }
            genelst = genes[1].split(",");
            for (String g : genelst) {
                genelistB.add(g.trim());
            }
        } else {
            // should really never happen!
            throw new RuntimeException("Malformed genes string with >2 fields");
        }
        if (ratio.length != 2) {
            throw new RuntimeException("Malformed ratio field n="+ratio.length);
        }
        this.simple = Integer.parseInt(ratio[0]);
        this.twisted = Integer.parseInt(ratio[1]);
        this.logPval = logpval;
    }

    public int getSimple() {
        return simple;
    }

    public int getTwisted() {
        return twisted;
    }
}
