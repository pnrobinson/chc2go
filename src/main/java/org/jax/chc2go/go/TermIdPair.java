package org.jax.chc2go.go;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

/**
 * This class is used to represent a pair of terms and its GO similarity to avoid repeatedly
 * calculating the similarity. It is intended to be used in a Map with
 * Map<TermIdPair, Double> with the Double being the similarity we calculated. If a similarity is
 * already in the map then we do not need to recalculate.
 * Note that we will always sort the ids so that the order of the arguments in the constructor
 * should not matter
 */
class TermIdPair {

    private final String  a;
    private final String  b;


    TermIdPair(TermId termA, TermId termB) {
        String vala = termA.getId();
        String valb = termB.getId();
        if (vala.compareTo(valb)>0) {
            a = vala;
            b=valb;
        } else {
            b = vala;
            a = valb;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TermIdPair that = (TermIdPair) o;
        return Objects.equals(a, that.a) &&
                Objects.equals(b, that.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
