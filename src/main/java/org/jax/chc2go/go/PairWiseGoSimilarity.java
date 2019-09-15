package org.jax.chc2go.go;

import com.google.common.collect.Sets;
import org.jax.chc2go.chc.ChcInteraction;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.go.GoGaf21Annotation;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.ontology.algo.InformationContentComputation;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.analysis.*;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.monarchinitiative.phenol.ontology.similarity.PairwiseResnikSimilarity;

import java.io.File;
import java.util.*;

/**
 * Class to calculate the pairwise similarity of genes/proteins according to the Gene Ontology terms that annotate them.
 */
public class PairWiseGoSimilarity {
    /** Each entry corresponds to a Capture Hi C interaction. */
    private final List<ChcInteraction> chcInteractionList;

    private Ontology geneOntology;
    /** Use by the GO framework to store annotations. */
    private AssociationContainer associationContainer;
    /** Pairwise similarity of GO terms. */
    private PairwiseResnikSimilarity pairwiseSimilarity;
    /** Key. TermId of a Gene Ontology term. Value: The information content (IC) of the term. */
    private Map<TermId, Double> icMap;
    /** Key -- symbol of a gene; Value: Corresponding Uniprot TermId. */
    private Map<String,TermId> geneSymbol2TermId;
    private static final String NOT_INITIALIZED = "n/a";
    /** count how many times we use the memo (for debug) */
    private int saved_lookup = 0;
    /** Key: Gene Ontology TermId. Value: Collection of genes/proteins that the GO term annotates. */
    private Map<TermId, Collection<TermId>> geneIdToTermIds;
    /** Key -- TermId of a human gene/protein. Value: Collection of Gene Ontology terms that annotate the gene. */
    private Map<TermId, Collection<TermId>> termIdToGeneIds;
    /** Stores GO similarities for pairs of TermIds to avoid double calculations */
    private Map<TermIdPair,Double> memoizedSimilarities;



    public PairWiseGoSimilarity(List<ChcInteraction> interactions, String goObo, String goGaf) {
        chcInteractionList = interactions;
        //  go.obo file
        File goOboFile = new File(goObo);
        // GoGaf file.
        File goGafFile = new File(goGaf);
        if (! goOboFile.exists()) {
            throw new RuntimeException("Could not find go.obo file");
        }
        if (! goGafFile.exists()) {
            throw new RuntimeException("Could not find go.gaf file");
        }
        System.out.println("[INFO] Parsing GO data.....");
        geneOntology = OntologyLoader.loadOntology(goOboFile, "GO");
        int n_terms = geneOntology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        System.out.println("[INFO] parsing  " + goGafFile.getAbsolutePath());
        try {
            final GoGeneAnnotationParser annotparser = new GoGeneAnnotationParser(goGafFile.getAbsolutePath());
            List<TermAnnotation> goAnnots = annotparser.getTermAnnotations();
            System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
            associationContainer = new AssociationContainer(goAnnots);
            int n = associationContainer.getTotalNumberOfAnnotatedTerms();
            System.out.println("[INFO] parsed " + n + " annotated terms");
        } catch (PhenolException e) {
            e.printStackTrace();
            System.out.println("[NOTE] The GAF file needs to be unzipped");
            return;
        }
        try {
            prepareResnik();
        } catch (PhenolException e) {
            e.printStackTrace();
        }
        prepareGeneSymbolMapping();
        testAllInteractions();
        compareMeans();
        compareMedians();
        System.out.println("saved="+  saved_lookup );
    }

    /**
     * Compare mean similarity for the different interaction classes.
     */
    private void compareMeans() {
        Map<ChcInteraction.InteractionType,List<Double>> means = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> meansUnder100k = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> meansUnder250k = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> meansUnder500k = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> meansUnder1m = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> meansOver1m = new HashMap<>();
        for (ChcInteraction chci :  chcInteractionList) {
            ChcInteraction.InteractionType itype = chci.getItype();
            means.putIfAbsent(itype,new ArrayList<>());
            double sim = chci.getSimilarity();
            int dist = chci.getDistance();
            means.get(itype).add(sim);
            if (dist < 100_000) {
                meansUnder100k.putIfAbsent(itype,new ArrayList<>());
                meansUnder100k.get(itype).add(sim);
            }
            else if (dist < 250_000) {
                meansUnder250k.putIfAbsent(itype,new ArrayList<>());
                meansUnder250k.get(itype).add(sim);
            }
            else if (dist < 500_000) {
                meansUnder500k.putIfAbsent(itype,new ArrayList<>());
                meansUnder500k.get(itype).add(sim);
            }
            else if (dist < 1_000_000) {
                meansUnder1m.putIfAbsent(itype,new ArrayList<>());
                meansUnder1m.get(itype).add(sim);
            } else {
                meansOver1m.putIfAbsent(itype, new ArrayList<>());
                meansOver1m.get(itype).add(sim);
            }

        }
        for (ChcInteraction.InteractionType it : means.keySet()) {
            List<Double> simvals = means.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " mean similarity (overall)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : meansUnder100k.keySet()) {
            List<Double> simvals = meansUnder100k.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " mean similarity (<100k)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : meansUnder250k.keySet()) {
            List<Double> simvals = meansUnder250k.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " mean similarity (<250k)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : meansUnder500k.keySet()) {
            List<Double> simvals = meansUnder500k.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " mean similarity (<500k)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : meansUnder1m.keySet()) {
            List<Double> simvals = meansUnder1m.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " mean similarity (<1m)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : meansOver1m.keySet()) {
            List<Double> simvals = meansOver1m.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " mean similarity (>1m)=" + mean);
        }
    }

    /**
     * Calculate medians according to distance and dump to shell
     */
    private void compareMedians() {
        Map<ChcInteraction.InteractionType,List<Double>> medians = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> mediansUnder100k = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> mediansUnder250k = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> mediansUnder500k = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> mediansUnder1m = new HashMap<>();
        Map<ChcInteraction.InteractionType,List<Double>> mediansOver1m = new HashMap<>();
        int count_of_zero_similarity_interactions = 0;
        for (ChcInteraction chci :  chcInteractionList) {
            ChcInteraction.InteractionType itype = chci.getItype();
            medians.putIfAbsent(itype,new ArrayList<>());
            double sim = chci.getSimilarity();
            if (sim == 0.0) {
                count_of_zero_similarity_interactions++;
            }
            int dist = chci.getDistance();
            medians.get(itype).add(sim);
            if (dist < 100_000) {
                mediansUnder100k.putIfAbsent(itype,new ArrayList<>());
                mediansUnder100k.get(itype).add(sim);
            }
            else if (dist < 250_000) {
                mediansUnder250k.putIfAbsent(itype,new ArrayList<>());
                mediansUnder250k.get(itype).add(sim);
            }
            else if (dist < 500_000) {
                mediansUnder500k.putIfAbsent(itype,new ArrayList<>());
                mediansUnder500k.get(itype).add(sim);
            }
            else if (dist < 1_000_000) {
                mediansUnder1m.putIfAbsent(itype,new ArrayList<>());
                mediansUnder1m.get(itype).add(sim);
            } else {
                mediansOver1m.putIfAbsent(itype,new ArrayList<>());
                mediansOver1m.get(itype).add(sim);
            }
            System.out.println("[INFO] count_of_zero_similarity_interactions = " + count_of_zero_similarity_interactions);
        }

        for (ChcInteraction.InteractionType it : medians.keySet()) {
            List<Double> simvals = medians.get(it);
            Collections.sort(simvals);
            double middle = (simvals.get(simvals.size() / 2) + simvals.get(simvals.size() / 2 - 1)) / 2;
            System.out.println(it + " median similarity=" + middle);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : mediansUnder100k.keySet()) {
            List<Double> simvals = mediansUnder100k.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " median similarity (<100k)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : mediansUnder250k.keySet()) {
            List<Double> simvals = mediansUnder250k.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " median similarity (<250k)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : mediansUnder500k.keySet()) {
            List<Double> simvals = mediansUnder500k.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " median similarity (<500k)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : mediansUnder1m.keySet()) {
            List<Double> simvals = mediansUnder1m.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " median similarity (<1m)=" + mean);
        }
        System.out.println();
        for (ChcInteraction.InteractionType it : mediansOver1m.keySet()) {
            List<Double> simvals = mediansOver1m.get(it);
            OptionalDouble average = simvals
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            double mean = average.isPresent() ? average.getAsDouble() : 0;
            System.out.println(it + " median similarity (>1m)=" + mean);
        }
    }



    /**
     * e.g., map KMT2B to UniProtKB:Q9BV73
     */
    private void prepareGeneSymbolMapping() {
        geneSymbol2TermId = new HashMap<>();
        for (TermId geneId : associationContainer.getAllAnnotatedGenes()) {
            try {
                ItemAssociations iassoc = associationContainer.get(geneId);
                Iterator it = iassoc.iterator();
                if (it.hasNext()) {
                    GoGaf21Annotation gaf = (GoGaf21Annotation) iassoc.iterator().next();
                    String dbObjectSymbol = gaf.getDbObjectSymbol();
                    TermId dbObjectTermId = gaf.getDbObjectTermId();
                    //System.out.println(dbObjectSymbol +": " + dbObjectTermId.getValue());
                    geneSymbol2TermId.put(dbObjectSymbol,dbObjectTermId);
                    continue;
                }
            } catch (PhenolException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("[INFO] We parsed %d gene symbol to TermId mappings", geneIdToTermIds.size());
    }




    private void prepareResnik() throws PhenolException {
        // Compute list of annoations and mapping from OMIM ID to term IDs.
        this.geneIdToTermIds = new HashMap<>();
        this.termIdToGeneIds = new HashMap<>();

        for (TermId geneId : this.associationContainer.getAllAnnotatedGenes()) {
            //System.out.println(geneId);
            ItemAssociations iassocs = this.associationContainer.get(geneId);
            List<TermId> goTerms = iassocs.getAssociations();
            geneIdToTermIds.putIfAbsent(geneId, new HashSet<>());
            final Set<TermId> inclAncestorTermIds = TermIds.augmentWithAncestors(geneOntology, Sets.newHashSet(goTerms), true);
            for (TermId goId : inclAncestorTermIds) {
                termIdToGeneIds.putIfAbsent(goId, new HashSet<>());
                termIdToGeneIds.get(goId).add(geneId);
                geneIdToTermIds.get(geneId).add(goId);
            }
        }
        System.out.printf("[INFO] termIdToGeneIds n=%d\n",termIdToGeneIds.size());
        System.out.printf("[INFO] geneIdToTermIds n=%d\n",geneIdToTermIds.size());
        // Compute information content of HPO terms, given the term-to-disease annotation.
        System.out.println("[INFO] Performing IC precomputation...");
        this.icMap =
                new InformationContentComputation(geneOntology)
                        .computeInformationContent(termIdToGeneIds);
        System.out.println("[INFO] DONE: Performing IC precomputation. Size of IC map is: " + icMap.size());
        // Setup PairwiseResnikSimilarity to use for computing scores.
        this.pairwiseSimilarity =
                new PairwiseResnikSimilarity(geneOntology, icMap);
    }


    private double getSimilarity(String geneA, String geneB) {
        this.memoizedSimilarities = new HashMap<>();
        TermId tidA = this.geneSymbol2TermId.get(geneA);
        TermId tidB = this.geneSymbol2TermId.get(geneB);

        if (tidA == null || tidB == null) {
            return 0.0;
        }
        Collection<TermId> goTidA = this.geneIdToTermIds.get(tidA);
        Collection<TermId> goTidB = this.geneIdToTermIds.get(tidB);
        if (goTidA==null || goTidB==null) return 0.0;
        double sumAtoB = 0.0;
        int c = 0;
        for (TermId a : goTidA) {
            double max_a_to_b = 0.0;
            for (TermId b : goTidB) {
                double sim;
                TermIdPair tip = new TermIdPair(a,b);
                if (this.memoizedSimilarities.containsKey(tip)) {
                    sim = this.memoizedSimilarities.get(tip);
                    saved_lookup++;
                } else {
                    sim = this.pairwiseSimilarity.computeScore(a, b);
                    this.memoizedSimilarities.put(tip,sim);
                }
                if (sim > max_a_to_b) max_a_to_b = sim;
            }
            c++;
            sumAtoB += max_a_to_b;
        }
        double similarityAtoB = c==0 ? 0.0 : sumAtoB/(double)c;
        // symmetrical -- this looks in the other direction
        c = 0;
        double sumBtoA = 0.0;
        for (TermId b : goTidB) {
            double max_b_to_a = 0.0;
            for (TermId a : goTidA) {
                double sim;
                TermIdPair tip = new TermIdPair(a,b);
                if (this.memoizedSimilarities.containsKey(tip)) {
                    sim = this.memoizedSimilarities.get(tip);
                    saved_lookup++;
                } else {
                    sim = this.pairwiseSimilarity.computeScore(a, b);
                    this.memoizedSimilarities.put(tip,sim);
                }
                if (sim > max_b_to_a) max_b_to_a = sim;
            }
            c++;
            sumBtoA += max_b_to_a;
        }
        double similarityBtoA = c==0 ? 0.0 : sumBtoA/(double)c;
        return 0.5 * (similarityAtoB + similarityBtoA);
    }


    private void testInteraction(ChcInteraction chci) {
        List<String> genelistA = chci.getGenelistA();
        List<String> genelistB = chci.getGenelistB();
        String maxGeneA = NOT_INITIALIZED;
        String maxGeneB = NOT_INITIALIZED;
        double maxSim = -1.0;
        for (String a : genelistA) {
            for (String b : genelistB) {
                double d = getSimilarity(a,b);
               // System.out.printf("%s <-> %s: %.2f\n",a,b,d);
                if (d>maxSim) {
                    maxSim = d;
                    maxGeneA = a;
                    maxGeneB = b;
                }
            }
        }
        chci.setSimilarity(maxSim,maxGeneA,maxGeneB);
    }

    private void testAllInteractions() {
        int total = this.chcInteractionList.size();
        int c = 0;
        System.out.println();
        for (ChcInteraction chci : this.chcInteractionList) {
            testInteraction(chci);
            c++;
            if (c%100==0) {
                System.out.printf("[INFO] Processed %d/%d interactions (%.1f%%).\r", c, total, 100.0* (double)c/total);
            }
           // if (c>2000) break; uncomment for testing
        }
    }

}
