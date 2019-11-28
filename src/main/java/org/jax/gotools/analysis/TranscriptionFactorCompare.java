package org.jax.gotools.analysis;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jax.gotools.string.PPI;
import org.jax.gotools.string.StringParser;
import org.jax.gotools.tf.TranscriptionFactor;
import org.jax.gotools.tf.UniprotEntry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        ImmutableSet.Builder<Integer> builder = new ImmutableSet.Builder();
        for (TranscriptionFactor tf : tflstA) {
            String id = tf.getName();
            if (name2enspMap.containsKey(id)) {
                Integer i = name2enspMap.get(id);
                builder.add(i);
            } else {
                System.err.println("[WARNING] Could not find ENSP for " + id);
            }
        }
        this.targetsetA = builder.build();
        builder = new ImmutableSet.Builder();
        for (TranscriptionFactor tf : tflstB) {
            String id = tf.getName();
            if (name2enspMap.containsKey(id)) {
                Integer i = name2enspMap.get(id);
                builder.add(i);
            } else {
                System.err.println("[WARNING] Could not find ENSP for " + id);
            }
        }
        this.targetsetB = builder.build();
    }


    public void parseString(String gzipPath) {
       StringParser parser = new StringParser(gzipPath, targetsetA, targetsetB);
        this.ppisA = parser.getSetAppis();
        this.ppisB = parser.getSetBppis();
    }

    public void parseRBPs(String path) {
        ImmutableSet.Builder<Integer> builder = new ImmutableSet.Builder<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))){
            String line;

            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                String []F = line.split("\t");
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
        System.out.printf("[INFO] Got %d RBPs as ENSP\n",rbps.size());
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
            if (this.rbps.contains(ppi.getProtein1()) ||  this.rbps.contains(ppi.getProtein2())) {
                n_RBP_interactions_B++;
            }
        }
        System.out.printf("A: %d total interactions, B: %d total interactions\n", n_totalA, n_total_B);
        System.out.printf("A: %d RBP interactions; B: %d RBP interactions.\n", n_RBP_interactions_A, n_RBP_interactions_B);
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
