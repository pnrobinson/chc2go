package org.jax.gotools.stats;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.Writer;
import java.util.*;

import static org.apache.commons.math3.stat.inference.TestUtils.chiSquare;
import static org.apache.commons.math3.stat.inference.TestUtils.chiSquareTest;
import static org.apache.commons.math3.stat.inference.TestUtils.t;

/**
 * Represents a chi-squared statistic, its p-value, and the p-value after correction for multiple comparisons.
 * Based on ChiSquared.java created by robinp on 6/14/17.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 09 Apr 2018
 */
public class ChiSquared implements Comparable<ChiSquared> {
    /** chi-squared statistic. */
    private double chiSquare;
    /** p-value associated with the chi-squared statistic. */
    private double chiSquareP;
    /** p-value after correction for multiple comparisons. */
    private double correctedP;
    /** Counts of observed terms */
    private long[][] observed;

    private final static double UNINITIALIZED = -1.0;

    private TermId termId;
    /** True if the assumptions of chi-1 testing are satisfied. */
    private boolean valid;

    ChiSquared(long[][] observed, TermId t) {
        valid=checkValues(observed);
        if (!valid) return;
        this.observed = observed;
        chiSquare = chiSquare(observed);
        chiSquareP = chiSquareTest(observed);
        correctedP = UNINITIALIZED;
        this.termId = t;
        this.valid = checkValues(observed);
    }


    private boolean checkValues(long[][] obs){
        if (obs[0][0]<5) return false;
        if (obs[0][1]<5) return false;
        if (obs[1][0]<5) return false;
        if (obs[1][0]<5) return false;
        return true; // todo move checking code here.
    }

    public boolean isValid(){ return valid; }

    public boolean isSignificant() {
        return correctedP < 0.05;
    }


    public static List<ChiSquared> fromAnotationMaps(Map<TermId, Integer> mapA, int totalCountA,
                                                     Map<TermId, Integer> mapB, int totalCountB) {
        ImmutableList.Builder builder = new ImmutableList.Builder();
        // get set of terms to test
        Set<TermId> termids = new HashSet<>();
        termids.addAll(mapA.keySet());
        termids.addAll(mapB.keySet());
        for (TermId tid : termids) {
            long[][] csq = new long[2][2];
            csq[0][0] = mapA.getOrDefault(tid, 0);
            csq[0][1] = totalCountA - csq[0][0];
            csq[1][0] = mapB.getOrDefault(tid,0);
            csq[1][1] = totalCountB - csq[1][0];
            ChiSquared c2 = new ChiSquared(csq, tid);
            if (c2.isValid()) {
                builder.add(c2);
            } else {
                System.err.println("Skipping " + tid.getValue());
            }
        }
        List<ChiSquared> c2list = builder.build();
        long N = c2list.stream().filter(ChiSquared::isValid).count();
        c2list.stream().forEach(c -> c.performBonferroni(N));
        return ImmutableList.sortedCopyOf(builder.build());
    }




    /**
     * Compares the argument to this ChiSquared object. Orders first by chiSquare value, then by
     * p-value. (p-value could differ even when chiSquared value is equal, if degrees of
     * freedom are different for the two objects.)
     * @param other   the ChiSquared object to which this object is compared
     * @return int    0 if they are equal; negative if this precedes (is less than) other,
     *                positive if other precedes (is less than) this
     * @throws NullPointerException if argument is null
     */
    public int compareTo(ChiSquared other) {
        Objects.requireNonNull(other, "[ChiSquared.compareTo] Cannot compare to null object");
        int cmp = Double.compare(this.chiSquare, other.chiSquare);
        return cmp != 0 ? cmp : Double.compare(this.chiSquareP, other.chiSquareP);
    }

    /**
     * Two ChiSquared objects are considered equal if they have the same chiSquare value and
     * the same p-value. (p-value could differ even when chiSquared value is equal, if degrees of
     * freedom are different for the two objects.)
     * @param o    object to which this ChiSquared is compared
     * @return     true if this ChiSquared and the object o are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChiSquared that = (ChiSquared) o;
        return  Double.compare(chiSquare, that.chiSquare) == 0 &&
                Double.compare(chiSquareP, that.chiSquareP) == 0;
    }

    public double getChiSquare() { return chiSquare; }

    public double getChiSquareP() { return chiSquareP; }

    public double getCorrectedP() { return correctedP; }

    public TermId getTermId() {
        return termId;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(chiSquare);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(chiSquareP);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }


    @Override
    public String toString() {
        long Acount = observed[0][0];
        long Atotal = (observed[0][0]+observed[0][1]);
        double Apercent = 100.0 * (double)Acount/(double)Atotal;
        long Bcount = observed[1][0];
        long Btotal = observed[1][0] + observed[1][1];
        double Bpercent = 100.0 * (double)Bcount/(double)Btotal;
        return String.format("%s chi2: %f p:%.2e, p.adj %.2e. Group A: %d/%d (%.1f%%). Group B: %d/%d (%.1f%%)",
                this.termId.getValue(), this.chiSquare, this.chiSquareP, this.correctedP,
                Acount, Atotal, Apercent,
                Bcount, Btotal, Bpercent);
    }



    /**
     * Sets the p-value with Bonferroni correction by multiplying the original p-value by the
     * number of comparisons performed, with max at 1.0.
     * @param numComparisons   total number of comparisons performed
     * @return double          the p-value after Bonferroni correction
     */
    public double performBonferroni(long numComparisons) {
        correctedP = Math.min(chiSquareP * numComparisons, 1.0);
        return correctedP;
    }
}
