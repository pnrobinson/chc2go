package org.jax.gotools.command;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.monarchinitiative.phenol.annotations.formats.go.GoGaf22Annotation;
import org.monarchinitiative.phenol.annotations.io.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.InformationContentComputation;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "IC", aliases = {"I"},
        mixinStandardHelpOptions = true,
        description = "Calculate IC of GO terms in groups")
public class Go2IcToolsCommand extends GoToolsCommand implements Callable<Integer> {
    Logger LOGGER = LoggerFactory.getLogger(Go2IcToolsCommand.class);
    @CommandLine.Option(names={"-i", "--input"}, description = "path to input file", required = true)
    private String inputPath;
    private Map<TermId, Double> icMap;

    private Map<TermId, Double> category2icMap;
    private Map<TermId, Double> category2weightedIcMap;

    @Override
    public Integer call() {
        calculateIcMap();
        evaluateTerms();
        return 0;
    }



    private void calculateIcMap() {
        String pathGoJson = String.format("%s%s%s", dataDir, File.separator, "go.json" );
        System.out.println("[INFO] parsing  " + pathGoJson);
        Ontology gontology = OntologyLoader.loadOntology(new File(pathGoJson), "GO");
        int n_terms = gontology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        String pathGoGaf = String.format("%s%s%s", dataDir, File.separator, "goa_human.gaf");
        System.out.println("[INFO] parsing  " + pathGoGaf);
        List<GoGaf22Annotation> goAnnots = GoGeneAnnotationParser.loadAnnotations(Path.of(pathGoGaf));
        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
        final Map<TermId, Collection<TermId>> termIdToGeneIds = new HashMap<>();
        for (TermAnnotation annot : goAnnots) {
            TermId geneId = annot.getItemId();
            TermId goId = annot.id();
            termIdToGeneIds.putIfAbsent(goId, new HashSet<>());
            final Set<TermId> inclAncestorTermIds = TermIds.augmentWithAncestors(gontology, Sets.newHashSet(goId), true);
            for (TermId tid : inclAncestorTermIds) {
                termIdToGeneIds.putIfAbsent(tid, new HashSet<>());
                termIdToGeneIds.get(tid).add(geneId);
            }
        }



        // Compute information content of HPO terms, given the term-to-disease annotation.
        LOGGER.info("Performing IC precomputation...");
        this.icMap = new InformationContentComputation(gontology)
                        .computeInformationContent(termIdToGeneIds);
        LOGGER.info("DONE: Performing IC precomputation");
    }

    private void evaluateTerms() {
        category2icMap = new HashMap();
        category2weightedIcMap = new HashMap();
        int no_ic = 0;
        int term_count = 0;
        int weighted_term_count = 0;
        double term_ic = 0d;
        double wewighted_term_ic = 0d;
        try (BufferedReader br = new BufferedReader(new FileReader(this.inputPath))) {
            String line;
            int c = 0;
            while ((line = br.readLine()) != null) {
                String [] A = line.split("\t");
                if (A.length != 2) {
                    LOGGER.warn(" Bad input line: " + line);
                    continue;
                }
                TermId goId = TermId.of(A[0]);
                try {
                    Integer cnt = Integer.parseInt(A[1]);
                    Double ic = this.icMap.getOrDefault(goId, -42.0);
                    if (ic < -1.0) {
                        no_ic++;
                        continue;
                    }
                    term_ic += ic;
                    wewighted_term_ic += ic*cnt;
                    term_count++;
                    weighted_term_count += cnt;
                } catch (NumberFormatException nfe) {
                    LOGGER.error(nfe.getMessage());
                }
                c++;
            }
            double mean_ic = term_ic/term_count;
            double mean_weighted_ic = wewighted_term_ic/weighted_term_count;
            System.out.printf("[INFO] Parsed %d GO terms.\n", c);
            if (no_ic > 0) {
                LOGGER.error("Could not get IC for {} terms", no_ic);
            }
            System.out.printf("Mean IC %f, mean weighted ic: %f\n", mean_ic, mean_weighted_ic);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
