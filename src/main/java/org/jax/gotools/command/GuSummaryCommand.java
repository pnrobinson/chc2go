package org.jax.gotools.command;


import org.monarchinitiative.phenol.annotations.formats.go.GoGaf21Annotation;
import org.monarchinitiative.phenol.annotations.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import picocli.CommandLine;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "summary",
        mixinStandardHelpOptions = true,
        description = "Create a summary of annotations for a gene")
public class GuSummaryCommand extends GoToolsCommand implements Callable<Integer> {

    @CommandLine.Option(names={"--gene"}, description = "a gene symbol")
    private String gene;


    @Override
    public Integer call( ) {
        initGoPathsToDefault(this.dataDir);
        Ontology gontology = OntologyLoader.loadOntology(new File(this.goOboPath), "GO");
        int n_terms = gontology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        String pathGoGaf = String.format("%s%s%s", dataDir, File.separator, "goa_human.gaf");
        System.out.println("[INFO] parsing  " + pathGoGaf);
        List<GoGaf21Annotation> goAnnots = GoGeneAnnotationParser.loadAnnotations(pathGoGaf);
        List<GoGaf21Annotation> geneAnnots =
                goAnnots.stream().filter(
                        a -> a.getDbObjectSymbol().equals(this.gene)
                ).collect(Collectors.toList());
        Set<MyAnnot> myAnnotSet = new HashSet<>();
        for (var a : geneAnnots) {
            Optional<String> opt = gontology.getTermLabel(a.getTermId());
            if (opt.isEmpty()) {
                System.err.println("Could not get label for " + a.getTermId().getValue());
                continue;
            }
            String label = opt.get();
            MyAnnot myAnnot = new MyAnnot(a.getGoId().getValue(), label, gene, a.getDbObjectId(), a.getQualifier());
            myAnnotSet.add(myAnnot)   ;
        }
        for (MyAnnot m : myAnnotSet) {
            System.out.println(m);
        }
        return 0;
    }


    static class MyAnnot {

        private final String id;
        private final String label;
        private final String gene;
        private final String dbObjectId;
        private final String qualifier;


        MyAnnot(String id, String label, String gene, String dbObjectId, String qualifier) {
            this.id = id;
            this.label = label;
            this.gene = gene;
            this.dbObjectId = dbObjectId;
            this.qualifier = qualifier;
        }

        @Override
        public String toString() {
            String q = qualifier.isEmpty() ? "" : "(" + qualifier + ")";
            return String.format("%s (%s)\t%s\t%s\t%s",
                    gene, dbObjectId, id, label, q);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyAnnot myAnnot = (MyAnnot) o;
            return Objects.equals(id, myAnnot.id) && Objects.equals(label, myAnnot.label) && Objects.equals(gene, myAnnot.gene) && Objects.equals(dbObjectId, myAnnot.dbObjectId) && Objects.equals(qualifier, myAnnot.qualifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, label, gene, dbObjectId, qualifier);
        }
    }


}
