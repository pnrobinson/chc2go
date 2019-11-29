package org.jax.gotools.analysis;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jax.gotools.stats.ChiSquared;
import org.jax.gotools.string.PPI;
import org.jax.gotools.string.StringParser;
import org.jax.gotools.tf.TranscriptionFactor;
import org.jax.gotools.tf.UniprotEntry;
import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Compare lists of transcription factors with repect to Gene Ontology annotations, PFAM modules, and STRING-encoded
 * protein bindings to a predefined set of proteins
 */
public class TranscriptionFactorCompare {

    private final List<TranscriptionFactor> tflstA;
    private final List<TranscriptionFactor> tflstB;
    private final List<UniprotEntry> upentrylist;

    private Map<Integer, UniprotEntry> spidToUniprotMap;
    private final Map<String, Integer> name2enspMap;
    private Set<Integer> targetsetA;
    private Set<Integer> targetsetB;
    private Set<Integer> rbps;
    private List<PPI> ppisA;
    private List<PPI> ppisB;

    private TranscriptionFactorCompare(Builder builder) {
        tflstA = builder.tflstA;
        tflstB = builder.tflstB;
        upentrylist = builder.upentrylist;
        spidToUniprotMap = new HashMap<>();
        ImmutableMap.Builder n2ebuilder = new ImmutableMap.Builder();
        for (UniprotEntry upe : builder.upentrylist) {
            for (String sp : upe.getStringIds()) {
                Integer ensp = ensp2int(sp);
                spidToUniprotMap.putIfAbsent(ensp, upe);
                String id = upe.getId();
                n2ebuilder.put(id, ensp);
            }
        }
        name2enspMap = n2ebuilder.build();
        initializeTargetList();
    }

    private Integer ensp2int(String ensp) {
        if (ensp.startsWith("9606.ENSP")) {
            return Integer.parseInt(ensp.substring(9));
        } else if (ensp.startsWith("ENSP")) {
            return Integer.parseInt(ensp.substring(4));
        } else {
            throw new RuntimeException("Could not parse ENSP " + ensp);
        }
    }

    private void initializeTargetList() {
        ImmutableSet.Builder<Integer> builderA = new ImmutableSet.Builder();
        for (TranscriptionFactor tf : tflstA) {
            String id = tf.getName();
            if (name2enspMap.containsKey(id)) {
                Integer i = name2enspMap.get(id);
                builderA.add(i);
            } else {
                System.err.println("[WARNING] Could not find ENSP for " + id);
            }
        }
        this.targetsetA = builderA.build();
        ImmutableSet.Builder<Integer> builderB = new ImmutableSet.Builder();
        for (TranscriptionFactor tf : tflstB) {
            String id = tf.getName();
            if (name2enspMap.containsKey(id)) {
                Integer i = name2enspMap.get(id);
                builderB.add(i);
            } else {
                System.err.println("[WARNING] Could not find ENSP for " + id);
            }
        }
        this.targetsetB = builderB.build();
    }


    public void parseString(String gzipPath) {
        StringParser parser = new StringParser(gzipPath, targetsetA, targetsetB);
        this.ppisA = parser.getSetAppis();
        this.ppisB = parser.getSetBppis();
    }

    public void parseRBPs(String path) {
        ImmutableSet.Builder<Integer> builder = new ImmutableSet.Builder<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                String[] F = line.split("\t");
                if (F.length != 3) {
                    throw new RuntimeException("Bad line for RBP file: " + line);
                }
                int ensp = ensp2int(F[2]);
                builder.add(ensp);
                //System.out.println(F[2] + ": " + ensp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        rbps = builder.build();
        System.out.printf("[INFO] Got %d RBPs as ENSP\n", rbps.size());
    }


    public void compareSetAandB() {
        int n_RBP_interactions_A = 0;
        int n_RBP_interactions_B = 0;
        int n_totalA = 0;
        int n_total_B = 0;
        for (PPI ppi : ppisA) {
            n_totalA++;
            //System.out.printf("pp1 : %s    %d\n", ppi.getEnsembl1(), ppi.getProtein1());
            if (this.rbps.contains(ppi.getProtein1()) || this.rbps.contains(ppi.getProtein2())) {
                n_RBP_interactions_A++;
            }
        }
        for (PPI ppi : ppisB) {
            n_total_B++;
            if (this.rbps.contains(ppi.getProtein1()) || this.rbps.contains(ppi.getProtein2())) {
                n_RBP_interactions_B++;
            }
        }
        System.out.printf("A: %d total interactions, B: %d total interactions\n", n_totalA, n_total_B);
        System.out.printf("A: %d RBP interactions (%.2f%% of total); B: %d RBP interactions %.2f%% of total).\n",
                n_RBP_interactions_A, 100.0 * (double) n_RBP_interactions_A / (double) n_totalA,
                n_RBP_interactions_B, 100.0 * (double) n_RBP_interactions_B / (double) n_total_B);
    }


    public void compareGO(String go) throws PhenolException {
        final Ontology geneOntology = OntologyLoader.loadOntology(new File(go));
        Map<TermId, Integer> mapA = new HashMap<>();
        Map<TermId, Integer> mapB = new HashMap<>();
        //final GoGeneAnnotationParser annotparser = new GoGeneAnnotationParser(gaf);
//        List<TermAnnotation> goAnnots = annotparser.getTermAnnotations();
//        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
//        AssociationContainer associationContainer = new AssociationContainer(goAnnots);
//        int n = associationContainer.getTotalNumberOfAnnotatedTerms();
        // associationContainer.
        int CC=0;
        int AA=0;
        for (TranscriptionFactor tf : tflstA) {
            String name = tf.getName();
            AA++;
            Integer i = name2enspMap.get(name);
            if (i == null) {
                System.err.println("[WARNING] Could not find ENSP for " + name);
                continue;
            }
            UniprotEntry e = spidToUniprotMap.get(i);
            if (e == null) {
                System.err.println("[WARNING] Could not find UniProt entry for " + name);
                continue;
            }
            List<TermId> goIds = e.getGoIds();
            Set<TermId> uniqueTerms = new HashSet<>();
            for (TermId t : goIds) {
                if (!geneOntology.getTermMap().containsKey(t)) {
                    System.err.println("[WARNING] go.obo did not contain " + t.getValue());
                    continue;
                }
                Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(geneOntology, t, true);
                uniqueTerms.addAll(ancs);
            }

        }
        for (TranscriptionFactor tf : tflstB) {
            String name = tf.getName();
            Integer i = name2enspMap.get(name);
            if (i == null) {
                System.err.println("[WARNING] Could not find ENSP for " + name);
                continue;
            }
            UniprotEntry e = spidToUniprotMap.get(i);
            if (e == null) {
                System.err.println("[WARNING] Could not find UniProt entry for " + name);
                continue;
            }
            List<TermId> goIds = e.getGoIds();
            Set<TermId> uniqueTerms = new HashSet<>();
            for (TermId t : goIds) {
                if (!geneOntology.getTermMap().containsKey(t)) {
                    System.err.println("[WARNING] go.obo did not contain " + t.getValue());
                    continue;
                }
                Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(geneOntology, t, true);
                uniqueTerms.addAll(ancs);
            }
            for (TermId w : uniqueTerms) {
                mapA.putIfAbsent(w, 0);
                mapA.merge(w, 1, Integer::sum);
                if (w.getValue().equals("GO:0050789")) CC++;
            }
        }
        // when we get here, mapA and mapB have term ids and counts.
        List<ChiSquared> c2list = ChiSquared.fromAnotationMaps(mapA, tflstA.size(), mapB, tflstB.size());
        for (ChiSquared c : c2list) {
            if (true ||     c.isSignificant()) {
                if (!geneOntology.getTermMap().containsKey(c.getTermId())) {
                    System.err.println("[WARNING] Could not retrieve " + c.getTermId().getValue());
                    continue;
                }
                String label = geneOntology.getTermMap().get(c.getTermId()).getName();
                System.out.println(label + ": " + c);
            }
        }
    }


    public static class Builder {
        List<TranscriptionFactor> tflstA;
        List<TranscriptionFactor> tflstB;
        List<UniprotEntry> upentrylist;

        public Builder transcriptionFactorListA(List<TranscriptionFactor> tflst) {
            this.tflstA = tflst;
            return this;
        }

        public Builder transcriptionFactorListB(List<TranscriptionFactor> tflst) {
            this.tflstB = tflst;
            return this;
        }

        public Builder uniprotEntries(List<UniprotEntry> upentries) {
            this.upentrylist = upentries;
            return this;
        }

        public TranscriptionFactorCompare build() {
            // todo -- qc data here
            return new TranscriptionFactorCompare(this);
        }
    }

}
