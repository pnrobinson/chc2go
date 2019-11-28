package org.jax.gotools.tf;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;

public class UniprotEntry {
    private final static String EMPTY = "";
    private final String id;
    private final String recName;
    private final String geneName;
    private final List<String> acList;
    private final List<TermId> goIds;
    private final List<Pfam> pfams;
    private final List<String> stringIds;


    public UniprotEntry(List<String> idLines,
                        List<String> deLines,
                        List<String> gnLines,
                        List<String> acLines,
                        List<String> ccLines,
                        List<String> drLines,
                        List<String> peLines,
                        List<String> kwLines,
                        List<String> ftLines) {
        if (idLines.isEmpty()) {
            throw new RuntimeException("ID empty");
        } if (idLines.size() > 1) {
            System.err.println("Warning: ID lines size=" + idLines.size());
        }
        String L = idLines.get(0);
        String []F = L.split("\\s+");
        if (F.length < 3) {
            throw new RuntimeException("ID length < 3: " +L);
        }
        id = F[0];
        //RecName: Full=
        String tmp = EMPTY;
        for (String ln : deLines) {
            if (ln.contains("RecName: Full=")) {
                tmp = getSubstringBetweenDelims(ln,'=',';');
            }
        }
        recName = tmp;
        tmp = EMPTY;
        for (String ln : gnLines) {
            if (ln.contains("Name=")) {
                tmp = getSubstringBetweenDelims(ln,'=',';');
            }
        }
        geneName = tmp;
        // the accessions
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        for (String ln : acLines) {
            String []A = ln.split(";");
            for (String a : A) {
                builder.add(a.trim());
            }
        }
        acList = builder.build();
        ImmutableList.Builder<TermId> gobuilder = new ImmutableList.Builder<>();
        ImmutableList.Builder<Pfam> pfamBuilder = new ImmutableList.Builder<>();
        ImmutableList.Builder<String> stringbuilder = new ImmutableList.Builder<>();
        for (String ln : drLines) {
            if (ln.startsWith("GO; GO:")) {
                String gostring = ln.substring(4,14);
                //System.out.println(gostring);
                TermId t = TermId.of(gostring);
                gobuilder.add(t);
            } else if (ln.startsWith("Pfam")) {
                String []A = ln.split(";");
                if (A.length >=3) {
                    Pfam pfam = new Pfam(A);
                    pfamBuilder.add(pfam);
                }
            } else if (ln.startsWith("STRING;")) {
                String []A = ln.split(";");
                if (A.length > 1) {
                    for (int i=1; i<A.length;i++) {
                        if (A[i].contains("ENSP")) {
                            stringbuilder.add(A[i].trim());
                        }
                    }
                }
            }
        }
        goIds = gobuilder.build();
        pfams = pfamBuilder.build();
        stringIds = stringbuilder.build();
    }

    private String getSubstringBetweenDelims(String line, char d1, char d2) {
        int i = line.indexOf(d1);
        if (i<0 || line.length() < (i+1)) return line;
        line = line.substring(i+1);
        i = line.indexOf(d2);
        if (i<0) return line;
        return line.substring(0,i);

    }


    public String toString() {
        return String.format("%s [%s/%s] GO Annotations: %d, Pfams: %d, String xrefs: %d",
                id, recName, geneName, goIds.size(), pfams.size(), stringIds.size());
    }


    public String getId() {
        return id;
    }

    public String getRecName() {
        return recName;
    }

    public String getGeneName() {
        return geneName;
    }

    public List<String> getAcList() {
        return acList;
    }

    public List<TermId> getGoIds() {
        return goIds;
    }

    public List<Pfam> getPfams() {
        return pfams;
    }

    public List<String> getStringIds() {
        return stringIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniprotEntry that = (UniprotEntry) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(recName, that.recName) &&
                Objects.equals(geneName, that.geneName) &&
                Objects.equals(acList, that.acList) &&
                Objects.equals(goIds, that.goIds) &&
                Objects.equals(pfams, that.pfams) &&
                Objects.equals(stringIds, that.stringIds);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, recName, geneName, acList, goIds, pfams, stringIds);
    }
}
