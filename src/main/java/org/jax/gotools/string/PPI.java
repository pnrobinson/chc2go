package org.jax.gotools.string;

import java.util.Objects;

/**
 * A class representing protein - protein interactions
 */
public class PPI {
    private final int protein1;
    private final int protein2;


    public PPI(Integer p1, Integer p2) {
        this.protein1 = p1;
        this.protein2 = p2;
    }

    public int getProtein1() {
        return protein1;
    }

    public int getProtein2() {
        return protein2;
    }

    public String getEnsembl1() {
        return String.format("ENSP%013d", protein1);
    }

    public String getEnsembl2() {
        return String.format("ENSP%013d", protein2);
    }

    @Override
    public String toString() {
        return String.format("%s <-> %S", getEnsembl1(), getEnsembl2());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PPI ppi = (PPI) o;
        return protein1 == ppi.protein1 &&
                protein2 == ppi.protein2;
    }

    @Override
    public int hashCode() {

        return Objects.hash(protein1, protein2);
    }
}
