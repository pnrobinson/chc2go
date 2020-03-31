package org.jax.gotools.go;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.jax.gotools.analysis.GoTermResult;
import org.jax.gotools.chc.ChcInteraction;
import org.monarchinitiative.phenol.annotations.formats.go.GoGaf21Annotation;
import org.monarchinitiative.phenol.annotations.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.InformationContentComputation;
import org.monarchinitiative.phenol.ontology.data.*;
import org.monarchinitiative.phenol.analysis.*;
import org.monarchinitiative.phenol.ontology.similarity.PairwiseResnikSimilarity;
import org.monarchinitiative.phenol.stats.*;
import org.monarchinitiative.phenol.stats.mtc.Bonferroni;
import org.monarchinitiative.phenol.stats.mtc.MultipleTestingCorrection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.jax.gotools.chc.ChcInteraction.InteractionType.UNDIRECTED_REF;

/**
 * Class to calculate the pairwise similarity of genes/proteins according to the Gene Ontology terms that annotate them.
 */
public class PairWiseGoSimilarity {
    private static final Logger logger = LoggerFactory.getLogger(PairWiseGoSimilarity.class);
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

    private Map<String,TermId> geneSymbol2IdMap;

    private List<TermAnnotation> goAnnots;


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
        this.goAnnots = GoGeneAnnotationParser.loadTermAnnotations(goGafFile);
        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
        associationContainer = new AssociationContainer(goAnnots);
        int n = associationContainer.getTotalNumberOfAnnotatedTerms();
        System.out.println("[INFO] parsed " + n + " annotated terms");

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
               // this is the baseline, just skip it -- we compare everything else to UNDIRECTED_REFxxx
               continue;
           }
           List<Double> range = means.get(itype); // sublist of similarity values that
           // correspond to itype for a given range, e.g., <100k
           DescriptiveStatistics rangeDS = new DescriptiveStatistics();
           range.forEach(d -> rangeDS.addValue(d));
           double rangeMean = rangeDS.getMean();
           double rangeMedian = rangeDS.getPercentile(50.0);
           double pVal;
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
                }
            } catch (PhenolException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("[INFO] We parsed %d gene symbol to TermId mappings", geneIdToTermIds.size());
    }


    /**
     * Get a list of all of the labeled genes in the population set.
     * @param annots List of annotations of genes/diseases to GO/HPO terms etc
     * @return an immutable set of TermIds representing the labeled genes/diseases
     */
    private Set<TermId> getPopulationSet(List<TermAnnotation> annots) {
        Set<TermId> st = new HashSet<>();
        for (TermAnnotation ann : annots) {
            TermId geneId = ann.getLabel();
            st.add(geneId);
        }
        return ImmutableSet.copyOf(st);
    }

    private StudySet getStudySet() {
        Set<TermId> studyGenes = new HashSet<>();
        for (ChcInteraction interact : chcInteractionList) {
            String geneA = interact.getMaxGeneA();
            String geneB = interact.getMaxGeneB();
            TermId tidA = this.geneSymbol2TermId.get(geneA);
            if (tidA == null) {
                System.err.printf("[ERROR] Could not find termid for %s, skipping.\n",geneA);
            } else {
                studyGenes.add(tidA);
            }
            TermId tidB = this.geneSymbol2TermId.get(geneB);
            if (tidB == null) {
                System.err.printf("[ERROR] Could not find termid for %s, skipping.\n",geneB);
            } else {
                studyGenes.add(tidB);
            }
        }
        Map<TermId, DirectAndIndirectTermAnnotations> studyAssocs = this.associationContainer.getAssociationMap(studyGenes,geneOntology);
        return new StudySet(studyGenes,"study set", studyAssocs);
    }



    public void performOverrepresentationAnalysis(Writer writer) throws IOException {
        if (this.geneSymbol2TermId == null || this.geneSymbol2TermId.isEmpty()) {
            prepareGeneSymbolMapping();
        }
        int n_terms = this.geneOntology.countNonObsoleteTerms();
        logger.trace("parsed {}} non-obsolete GO terms.",n_terms);
        int n_annoted_terms = this.associationContainer.getTotalNumberOfAnnotatedTerms();
        logger.trace("Of these, {} terms were annotated.",n_annoted_terms);
        logger.trace("Number of GO annotations: {}.", this.goAnnots.size());
        Set<TermId> populationGenes = getPopulationSet(this.goAnnots);
        Map<TermId, DirectAndIndirectTermAnnotations> popAssocs = this.associationContainer.getAssociationMap(populationGenes, geneOntology);
        StudySet populationSet = new PopulationSet(populationGenes, popAssocs);
        StudySet studySet = getStudySet();
        Hypergeometric hgeo = new Hypergeometric();
        MultipleTestingCorrection bonf = new Bonferroni();
        TermForTermPValueCalculation tftpvalcal = new TermForTermPValueCalculation(this.geneOntology,
            associationContainer,
            populationSet,
            studySet,
            hgeo,
            bonf);
        List<GoTerm2PValAndCounts> pvals = tftpvalcal.calculatePVals();
        logger.trace("Total number of retrieved p values: {}", pvals.size());
        int n_sig = 0;
        double ALPHA = 0.05;
        int studytotal = studySet.getAnnotatedItemCount();
        int poptotal = populationSet.getAnnotatedItemCount();
        List<GoTermResult> results = new ArrayList<>();
        logger.trace(String.format("Study set: %d genes. Population set: %d genes", studytotal,poptotal));
        writer.write(GoTermResult.header() + "\n");

        for (GoTerm2PValAndCounts item : pvals) {
            double pval = item.getRawPValue();
            double pval_adj = item.getAdjustedPValue();
            TermId tid = item.getItem();
            Term term = geneOntology.getTermMap().get(tid);
            if (term == null) {
                System.err.println("[ERROR] Could not retrieve term for " + tid.getValue());
                continue;
            }
            String label = term.getName();
            if (pval_adj > ALPHA) {
                continue;
            }
            n_sig++;
            GoTermResult result = new GoTermResult(label, tid, pval, pval_adj, item.getAnnotatedStudyGenes(), studytotal, item.getAnnotatedPopulationGenes(), poptotal);
            results.add(result);
        }
        Collections.sort(results);
        for (GoTermResult result : results) {
            writer.write(result.getRow() + "\n");
        }
        System.out.println(String.format("%d of %d terms were significant at alpha %.7f",
        n_sig,pvals.size(),ALPHA));
    }





    private void prepareResnik() throws PhenolException {
        // Compute list of annotations and mapping from OMIM ID to term IDs.
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
