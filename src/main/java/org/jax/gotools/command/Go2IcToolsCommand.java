package org.jax.gotools.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.monarchinitiative.phenol.annotations.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.InformationContentComputation;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;

import java.io.*;
import java.util.*;

@Parameters(commandDescription = "Calculate IC of GO terms in groups")
public class Go2IcToolsCommand extends GoToolsCommand {
    @Parameter(names={"-i", "--input"}, description = "path to input file", required = true)
    private String inputPath;

    private Map<TermId, Double> icMap;

    private Multimap<String, Double> category2icMap;


    @Override
    public void run() {
        calculateIcMap();
        evaluateTerms();
        printResults();
    }



    private void calculateIcMap() {
        String pathGoObo = String.format("%s%s%s", dataDir, File.separator, "go.obo" );
        System.out.println("[INFO] parsing  " + pathGoObo);
        Ontology gontology = OntologyLoader.loadOntology(new File(pathGoObo), "GO");
        int n_terms = gontology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        String pathGoGaf = String.format("%s%s%s", dataDir, File.separator, "goa_human.gaf");
        List<TermAnnotation> goAnnots = new ArrayList<>();
        System.out.println("[INFO] parsing  " + pathGoGaf);
        goAnnots = GoGeneAnnotationParser.loadTermAnnotations(pathGoGaf);
        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
        final Map<TermId, Collection<TermId>> termIdToGeneIds = new HashMap<>();
        for (TermAnnotation annot : goAnnots) {
            TermId geneId = annot.getLabel();
            TermId goId = annot.getTermId();
            termIdToGeneIds.putIfAbsent(goId, new HashSet<>());
            final Set<TermId> inclAncestorTermIds = TermIds.augmentWithAncestors(gontology, Sets.newHashSet(goId), true);
            for (TermId tid : inclAncestorTermIds) {
                termIdToGeneIds.putIfAbsent(tid, new HashSet<>());
                termIdToGeneIds.get(tid).add(geneId);
            }
        }



        // Compute information content of HPO terms, given the term-to-disease annotation.
        System.out.println("[INFO] Performing IC precomputation...");
        this.icMap =
                new InformationContentComputation(gontology)
                        .computeInformationContent(termIdToGeneIds);
        System.out.println("[INFO] DONE: Performing IC precomputation");
    }

    private void evaluateTerms() {
        category2icMap = ArrayListMultimap.create();
        try (BufferedReader br = new BufferedReader(new FileReader(this.inputPath))) {
            String line;
            int c = 0;
            while ((line = br.readLine()) != null) {
                String [] A = line.split("\t");
                if (A.length != 2) {
                    System.err.println("[ERROR] Bad input line: " + line);
                    continue;
                }
                TermId goId = TermId.of(A[0]);
                String category = A[1];
                c++;
                Double ic = this.icMap.getOrDefault(goId, -42.0);
                if (ic < -1.0) {
                    System.err.println("[ERROR] Could not get IC for : " + goId.getValue());
                    continue;
                }
                category2icMap.put(category, ic);
            }
            System.out.printf("[INFO] Parsed %d GO terms.\n", c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printResults() {
        String outfilename = "go2ic.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfilename))) {
            for (Map.Entry<String, Double> entry : category2icMap.entries()) {
                String cat = entry.getKey();
                Double d = entry.getValue();
                writer.write(cat + "\t" + d + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
