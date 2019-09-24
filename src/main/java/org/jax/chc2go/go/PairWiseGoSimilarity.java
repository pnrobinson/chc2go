package org.jax.chc2go.go;

import com.google.common.collect.Sets;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
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

import static org.jax.chc2go.chc.ChcInteraction.InteractionType.TWISTED;
import static org.jax.chc2go.chc.ChcInteraction.InteractionType.UNDIRECTED_REF;

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


    /**
     *
     * @param interactions List of Capture Hi-C interaction objects
     * @param goObo path to the go.obo file
     * @param goGaf path to human_goa.gaf file
     */
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

    }

    public void analyzePairwiseSimilarities() {
        prepareGeneSymbolMapping();
        testAllInteractions();
        compareMeans();
        System.out.println("saved="+  saved_lookup );
    }


    /**
     *
     * @param means One of the hash maps shown in {@link #compareMeans{}
     * @param title The type of test (e.g., 100k-250k)
     */
    private void performTTest( Map<ChcInteraction.InteractionType,List<Double>> means, String title) {
       System.out.println("\n\n######################" + title + "######################\n\n");
       if (! means.containsKey(UNDIRECTED_REF)) {
           System.err.println("[ERROR] Could not find UNDIRECTED_REF data for " + title);
           return; // should never happen if the input file is valid, but check nonetheless!
       }
       DescriptiveStatistics undirectedDS = new DescriptiveStatistics();
       List<Double> undirected = means.get(UNDIRECTED_REF);
       undirected.forEach(d -> undirectedDS.addValue(d));
       double undirectedMean = undirectedDS.getMean();
       double undirectedMedian = undirectedDS.getPercentile(50.0);

       for (ChcInteraction.InteractionType itype : means.keySet()) {
           if (itype == UNDIRECTED_REF) {
               // this is the baseline, just skip it -- we compare everything else to UNDIRECTED_REF
               continue;
           }
           List<Double> range = means.get(itype); // sublist of similarity values that
           // correspond to itype for a given range, e.g., <100k
           DescriptiveStatistics rangeDS = new DescriptiveStatistics();
           range.forEach(d -> rangeDS.addValue(d));
           double rangeMean = rangeDS.getMean();
           double rangeMedian = rangeDS.getPercentile(50.0);
           double pVal = -1.0;
           System.out.printf("Number of observations: undirected: %d %s: %d\n", undirectedDS.getN(), itype, rangeDS.getN());
           if (undirectedDS.getN()>1 && rangeDS.getN() > 1) {
               // need at least two values to do a t-test
               pVal = TestUtils.tTest(undirectedDS, rangeDS);
               System.out.printf("Undirected reference mean: %.2f, median %.2f, %s mean: %.2f, %s median: %.2f, T-test p-value %e\n",
                       undirectedMean,undirectedMedian,itype,rangeMean,itype,rangeMedian,pVal);
           } else {
               System.out.printf("Undirected reference mean: %.2f, median %.2f, %s mean: %.2f, %s median: %.2f\n",
                       undirectedMean,undirectedMedian,itype,rangeMean,itype,rangeMedian);
           }
           System.out.println();


       }
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

        performTTest(means, "overall");
        performTTest(meansUnder100k, "under 100k");
        performTTest(meansUnder250k, "over 100k and under 250k");
        performTTest(meansUnder500k, "over 250k and under 500k");
        performTTest(meansUnder1m, "over 500k and under 1m");
        performTTest(meansOver1m, "over 1m");

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
            //if (c>5000) break; //uncomment for testing
        }
    }

}
