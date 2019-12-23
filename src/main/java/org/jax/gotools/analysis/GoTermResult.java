package org.jax.gotools.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class GoTermResult implements  Comparable<GoTermResult> {
    private final String GoTermLabel;
    private final TermId GoTermId;
    private final double pval;
    private final double adj_pval;
    private final int studyCount;
    private final int studyTotal;
    private final int popCount;
    private final int popTotal;



    public GoTermResult(String label, TermId id, double pval, double adj_pval, int studycount, int studytotal, int popcount, int poptotal) {
        this.GoTermLabel = label;
        this.GoTermId = id;
        this.pval = pval;
        this.adj_pval = adj_pval;
        this.studyCount = studycount;
        this.studyTotal = studytotal;
        this.popCount = popcount;
        this.popTotal = poptotal;
    }

    private String formatPval(double p) {
        if (p > 0.1) {
            return String.format("%.2f", p);
        } else if (p > 0.01) {
            return String.format("%.3f", p);
        }else if (p > 0.001) {
            return String.format("%.4f", p);
        } else {
            return String.format("%.2e", p);
        }
    }

    private String getPerc(int x, int y) {
        double p = 100.0 * (double)x/(double)y;
        return String.format("%.1f%%",p );
    }

    public static String header() {
        String []fields = {"GO.term", "GO.id", "pval", "pval.adj", "n.study", "perce.study", "n.population", "perc.population"};
        return String.join("\t", fields);
    }

    public String getRow() {
        String [] fields = {
                this.GoTermLabel,
                this.GoTermId.getValue(),
                formatPval(this.pval),
                formatPval(this.adj_pval),
                String.valueOf(this.studyCount),
                getPerc(this.studyCount, this.studyTotal),
                String.valueOf(this.popCount),
                getPerc(this.popCount, this.popTotal) };
        return String.join("\t", fields);
    }


    @Override
    public int compareTo(GoTermResult other) {
        return Double.compare(pval, other.pval);
    }
}
