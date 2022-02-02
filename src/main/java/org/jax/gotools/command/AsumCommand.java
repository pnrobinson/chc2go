package org.jax.gotools.command;

import org.monarchinitiative.phenol.annotations.formats.go.GoGaf21Annotation;
import org.monarchinitiative.phenol.annotations.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "asum", aliases = {"A"},
        mixinStandardHelpOptions = true,
        description = "Annotation Summary")
public class AsumCommand extends GoToolsCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0")    String symbol;

    @Override
    public Integer call() {
        System.out.printf("[INFO] Extract annotations for %s\n", symbol);
        File oboFile = new File(this.goOboPath);
        File gafFile = new File(this.goGafPath);
        if (!oboFile.isFile()) {
            System.out.printf("Could not find obo file %s\n", goOboPath);
            return 1;
        }
        if (! gafFile.isFile()){
            System.out.printf("Could not find GAF file %s\n", goGafPath);
            return 1;
        }
        Ontology gontology = OntologyLoader.loadOntology(oboFile, "GO");
        int n_terms = gontology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        List<GoGaf21Annotation> goAnnots = GoGeneAnnotationParser.loadAnnotations(gafFile);
        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
        final Set<TermId> seenGoIdSet = new HashSet<>();
        List<String> annotList = new ArrayList<>();
        TermId geneId = null;
        for (GoGaf21Annotation annot : goAnnots) {
            String dbSymbol = annot.getDbObjectSymbol();
            if (!dbSymbol.equals(this.symbol)) {
                continue;
            }
            geneId = annot.getLabel();
            TermId goId = annot.getTermId();
            if (seenGoIdSet.contains(goId)) {
                continue;
            } else {
                seenGoIdSet.add(goId);
            }

            Optional<String> opt = gontology.getTermLabel(goId);
            if (opt.isEmpty()) {
                System.err.println("[ERROR] Could not retrieve label for " + goId.getValue());
                continue;
            }
            String label = opt.get();
            annotList.add(String.format("%s (%s)",label, goId.getValue()));
        }
        Collections.sort(annotList);
        System.out.printf("### %s (%s) ###\n", symbol, geneId != null ? geneId.getValue() : "n/a");
        for (String s : annotList) {
            System.out.println(s);
        }
        return 0;
    }
}
