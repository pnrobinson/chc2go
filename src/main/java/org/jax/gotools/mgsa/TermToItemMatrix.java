package org.jax.gotools.mgsa;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.analysis.ItemAssociations;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * This replaces the original CalculationUtils.java and uses Java8 collections.
 */

public class TermToItemMatrix {

    private final int n_genes;
    private final int n_annotated_terms;
    /** The genes (usually) that are annotated to one or more GO term. The index is used in the {@link #annotatedItemToIndexMap} */
    private final List<TermId> annotatedItemList;
    /** List of Gene Ontology terms. The index is used in the {@link #goTermToIndexMap} */
    private final List<TermId> goTermList;
    /** The genes (usually) and their indices in {@link #annotatedItemList} */
    private final Map<TermId,Integer> annotatedItemToIndexMap;
    /** The GO terms and their indices in {@link #goTermList}. */
    private final Map<TermId,Integer> goTermToIndexMap;

    private final int [][] termLinks;


    TermToItemMatrix(AssociationContainer assocs) throws PhenolException {
        Set<TermId> genes = assocs.getAllAnnotatedGenes();
        Set<TermId> goTerms = new HashSet<>();
        Multimap<TermId, TermId> goTermToAnnotatedGenesMap = ArrayListMultimap.create();
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        ImmutableMap.Builder<TermId,Integer> mapBuilder = new ImmutableMap.Builder<>();
        n_genes = genes.size();
        n_annotated_terms = assocs.getTotalNumberOfAnnotatedTerms();
        termLinks = new int[n_annotated_terms][];
        int i = 0;
        for (TermId tid : genes) {
            builder.add(tid);
            mapBuilder.put(tid,i);
            ItemAssociations itemAssocs = assocs.get(tid);
            List<TermId> assox = itemAssocs.getAssociations();
            for (TermId goId : assox) {
                goTermToAnnotatedGenesMap.put(goId,tid);
            }
            goTerms.addAll(assox);
            termLinks[i] = new int[assox.size()];
            i++;
        }
        annotatedItemList = builder.build();
        annotatedItemToIndexMap = mapBuilder.build();
        // reset index and builders
        i = 0;
        builder = new ImmutableList.Builder<>();
        mapBuilder = new ImmutableMap.Builder<>();
        for (TermId goTermId : goTerms) {
            builder.add(goTermId);
            mapBuilder.put(goTermId, i);
            i++;
        }
        goTermList = builder.build();
        goTermToIndexMap = mapBuilder.build();
        int gotIndex = 0;
        int missedIndex = 0;
        // Now create the array. Each row represents one GO term, with the columns representing the genes that
        // are annotated to the GO term. The matrix is not square, but instead we have an array of variable length
        // arrays with the fields of the array holding the index of the gene as per annotatedItemList
        for (int j = 0; j < goTermList.size(); j++) {
            TermId goId = goTermList.get(j);
            Collection<TermId> geneCollection = goTermToAnnotatedGenesMap.get(goId);
            int N = geneCollection.size();
            termLinks[j] = new int[N];
            int k = 0;
            for (TermId g : geneCollection) {
                Integer idx = this.annotatedItemToIndexMap.get(g);
                if (idx == null || idx < 0) {
                    missedIndex++;
                    System.err.printf("[ERROR] Could not get index for gene %s (total missed %d; total found %d).\n" ,
                            g.getValue(), missedIndex, gotIndex);
                    continue;
                } else {
                    gotIndex++;
                }
                termLinks[j][k] = idx;
                k++;
            }
        }
    }


    TermId getGoTermAtIndex(int i) {
        return this.goTermList.get(i);
    }

    public int getAnnotatedGeneCount(TermId goTermId) {
        Integer i = goTermToIndexMap.get(goTermId);
        if (i == null || i < 0) {
            System.err.println("[ERROR] Could not find index for GO Term " + goTermId);
            return 0;
        }
        int [] genes = this.termLinks[i];
        return genes.length;
    }

    public boolean []  getBooleanArrayobservedItems(Set<TermId> geneIds) {
        boolean [] observed = new boolean[this.n_genes];
        for (TermId gene : geneIds) {
            Integer i = this.annotatedItemToIndexMap.get(gene);
            if (i == null || i < 0) {
                System.err.println("[ERROR] Could not get index for gene " + gene.getValue());
                continue;
            }
            observed[i] = true;
        }
        return observed;
    }


    /**
     * Creates an array of term to item associations.
     * @return the array.
     */
    public int [][] getTermLinks()
    {
        return this.termLinks;
    }
}
