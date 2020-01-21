package org.jax.gotools.analysis;

import com.google.common.collect.ImmutableSet;
import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.analysis.ItemAssociations;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.util.*;

public class GoTable {

    private final Ontology geneOntology;
    /** Use by the GO framework to store annotations. */
    private AssociationContainer associationContainer;
    private List<TermAnnotation> goAnnots;
    private Set<TermId> targetGoTermSet;
    private List<TermId> targetGoTermList;
    private List<Protein> proteins;
    private String goLabels [];



    public GoTable(String goObo, String goGaf, String inputFile, List<TermId> goIdList, String goLabels []) {
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
        this.geneOntology = OntologyLoader.loadOntology(goOboFile, "GO");
        int n_terms = geneOntology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        System.out.println("[INFO] parsing  " + goGafFile.getAbsolutePath());
        try {
            final GoGeneAnnotationParser annotparser = new GoGeneAnnotationParser(goGafFile.getAbsolutePath());
            this.goAnnots = annotparser.getTermAnnotations();
            System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
            associationContainer = new AssociationContainer(goAnnots);
            int n = associationContainer.getTotalNumberOfAnnotatedTerms();
            System.out.println("[INFO] parsed " + n + " annotated terms");
        } catch (PhenolException e) {
            e.printStackTrace();
            System.out.println("[NOTE] The GAF file needs to be unzipped");
            return;
        }
        // read the target terms
        this.targetGoTermList = goIdList;
        this.targetGoTermSet = new HashSet<>(goIdList);
        this.goLabels = goLabels;
        parseInputFile(inputFile);
        checkAnnotations();
    }


    private void checkAnnotations() {
        for (Protein p : this.proteins) {
            TermId uprotId = p.getUniprotTermID();
            List<Boolean> annotated = new ArrayList<>();
            try {
                ItemAssociations itmemassoc = this.associationContainer.get(uprotId);
                for (TermId goId : this.targetGoTermSet) {
                    if (itmemassoc.containsID(goId)) {
                        annotated.add(Boolean.TRUE);
                    } else {
                        annotated.add(Boolean.FALSE);
                    }
                }
                p.setAnnotations(annotated);
            } catch (PhenolException e) {
                System.err.println("[ERROR] Could not get annotations for " + p);
            }
        }
    }


    private void parseInputFile(String path) {
        proteins = new ArrayList<>();
        File file = new File(path);
        if (! file.exists()) {
            throw new RuntimeException("Could not find input file at " + path);
        }
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String fields [] = line.split("\t");
                if (fields.length < 4) {
                    continue; // skip bad lines
                }
                String sym = fields[0];
                int geneID;
                try {
                    geneID = Integer.parseInt(fields[1]);
                } catch (NumberFormatException e) {
                    System.err.println("COuld not parse gene id: " + fields[1]);
                    continue;
                }
                String uprot = fields[2];
                String name = fields[3];
                proteins.add(new Protein(sym, geneID, uprot, name));
                System.out.println(proteins.get(proteins.size()-1));
            }
        } catch (IOException e ){
            e.printStackTrace();
            throw new RuntimeException("Could not find input file");
        }
    }


    /**
     * This class represents a line from the input file, whichh we expect to have
     */
    public static class Protein {
        public final String geneSymbol;
        public final int geneID;
        public final String uniprotID;
        public final String name;
        public List<Boolean> annotated;


        public Protein(String sym, int id, String uprot, String name) {
            this.geneSymbol = sym;
            this.geneID = id;
            this.uniprotID = uprot;
            this.name = name;

        }

        public void setAnnotations(List<Boolean> annot) {
            this.annotated = annot;
        }

        public TermId getUniprotTermID() {
            return TermId.of("UniProtKB", this.uniprotID);
        }

        @Override
        public String toString() {
            return String.format("%s (GeneID:%d; %s): %s", geneSymbol, geneID, uniprotID, name);
        }
    }

    private String getHeader() {
        List<String> header = new ArrayList<>();
        header.add("Gene");
        header.add("Gene ID");
        header.add("Uniprot");
        header.add("name");
        for (String s : this.goLabels) {
            String h = String.format("\\rot{%s}",s);
            header.add(h);
        }


        return String.join(" &", header);
    }




    public void outputLatexTableToFile(String filename) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("\\documentclass{standalone} \n");
            writer.write("\\usepackage[table]{xcolor} \n");
            writer.write("\\usepackage{array,graphicx} \n");
            writer.write("\\newcommand*\\rot{\\rotatebox{90}}\n");
            writer.write("\\newcommand*\\OK{\\ding{51}}");
            writer.write("\\begin{document}");

            String fields = "{lllp{4cm}";
            int n = targetGoTermSet.size();
            for (int i = 0; i<n; i++) {
                fields += "l";
            }
            fields += "}";
            writer.write("\\begin{tabular}" + fields + "\n");
            writer.write(getHeader() + "\\\\ \n");
            for (Protein p : this.proteins) {
                List<String> arr = new ArrayList<>();
                arr.add(p.geneSymbol);
                arr.add(String.valueOf(p.geneID));
                arr.add(p.uniprotID);
                arr.add(p.name);
                for (Boolean b : p.annotated) {
                    if (b) {
                        arr.add("\\cellcolor{black!75}x");
                    } else {
                        arr.add(" ");
                    }
                }
                String row = String.join("&", arr);
                writer.write(row + "\\\\ \n");
            }
            writer.write("\\end{tabular}\n");
            writer.write("\\end{document}\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
