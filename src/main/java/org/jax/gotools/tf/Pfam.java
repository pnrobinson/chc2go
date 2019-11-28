package org.jax.gotools.tf;

import java.util.Objects;

public class Pfam {
    private final String id;
    private final String name;


    public Pfam(String []F) {
        id = F[1].trim();
        name = F[2].trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pfam pfam = (Pfam) o;
        return Objects.equals(id, pfam.id) &&
                Objects.equals(name, pfam.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, name);
    }
}
