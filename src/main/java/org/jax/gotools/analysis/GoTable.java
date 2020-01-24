package org.jax.gotools.analysis;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.analysis.ItemAssociations;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.util.*;

public class GoTable {

    private final Ontology geneOntology;
    /** Use by the GO framework to store annotations. */
    private List<Protein> proteins;

    private Map<TermId, String> goTermMap;

    private Map<String,Protein> uprotMap;



    public GoTable(String goObo, String goGaf, Map<TermId, String> goTermMap) {
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
        this.goTermMap = goTermMap;
        this.uprotMap = new HashMap<>();
        findAnnotatedProteins(goGaf);
        makeSortedListOfProteins();
    }



    private void findAnnotatedProteins(String goGaf) {
        try (BufferedReader br = new BufferedReader(new FileReader(goGaf))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("!"))
                    continue;
                //System.out.println(line);
                String F[] = line.split("\t");
                String uprotID = F[1]; // e.g., O43633
                String symbol = F[2]; // e.g. CHMP2A
                TermId goTerm = TermId.of(F[4]);
                String label = F[9]; // e.g., Charged multivesicular body protein 2a
                String category = F[11]; // e.g. protein
                if (! category.equals("protein"))
                    continue;
                Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(this.geneOntology, goTerm, true);
                for (TermId targetID : this.goTermMap.keySet()) {
                    if (ancs.contains(targetID)) {
                        // we have a match!
                        int fakeGeneId = 42;
                        this.uprotMap.putIfAbsent(uprotID, new Protein(symbol,fakeGeneId,uprotID, label));
                        Protein p = this.uprotMap.get(uprotID);
                        p.addAnnotation(targetID);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not read go gaf file");
            System.exit(1);
        }
        System.out.printf("[INFO] Added %d proteins.\n", this.uprotMap.size());
    }


    private void makeSortedListOfProteins() {
        this.proteins = new ArrayList<>();
        this.proteins.addAll(this.uprotMap.values());
        Collections.sort(this.proteins);
    }




    /**
     * This class represents a line from the input file, whichh we expect to have
     */
    public static class Protein implements Comparable<Protein> {
        public final String geneSymbol;
        public final int geneID;
        public final String uniprotID;
        public final String name;
        public Set<TermId> annotatedGoTermSet;


        public Protein(String sym, int id, String uprot, String name) {
            this.geneSymbol = sym;
            this.geneID = id;
            this.uniprotID = uprot;
            this.name = name;
            annotatedGoTermSet = new HashSet<>();

        }

        @Override
        public int compareTo(Protein other) {
            return uniprotID.compareTo(other.uniprotID);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 31).
                            append(geneSymbol).
                            append(geneID).
                            append(uniprotID).
                            append(name).
                            toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Protein)) return false;
            Protein other = (Protein) obj;
            return geneSymbol.equals(other.geneSymbol) &&
                    geneID == other.geneID &&
                    uniprotID.equals(other.uniprotID) &&
                    name.equals(other.name);
        }

        public void addAnnotation(TermId tid) {
            annotatedGoTermSet.add(tid);
        }

        public boolean isAnnotatedTo(TermId tid) {
            return this.annotatedGoTermSet.contains(tid);
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
        for (String s : this.goTermMap.values()) {
            String h = String.format("\\rot{%s}",s);
            header.add(h);
        }


        return String.join(" &", header);
    }

    private String headerField(String s) {
        return String.format("\\multicolumn{1}{|c|}{\\textbf{%s}}", s);
    }


    private String getHeaderForLongTable() {
        List<String> header = new ArrayList<>();
        header.add(headerField("Gene"));
        header.add(headerField("Gene ID"));
        header.add(headerField("Uniprot"));
        header.add(headerField("name"));
        for (String s : this.goTermMap.values()) {
            String h = String.format("\\rot{%s}",s);
            header.add(headerField(h));
        }
        return "\\hline + " + String.join(" &", header);
    }


    public void outputLatexLongTableToFile(String filename) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("\\documentclass{standalone} \n");
            writer.write("\\usepackage[table]{xcolor} \n");
            writer.write("\\usepackage{longtable} \n");
            writer.write("\\usepackage{array,graphicx} \n");
            writer.write("\\usepackage{pifont} \n");
            writer.write("\\newcommand*\\rot{\\rotatebox{90}}\n");
            writer.write("\\newcommand*\\OK{\\ding{51}}");
            writer.write("\\begin{document}");

            String fields = "{lllp{8cm}|";
            int n = this.goTermMap.size();
            for (int i = 0; i<n; i++) {
                fields += "l";
            }
            fields += "}";


            writer.write("\\begin{longtable}" + fields + "\n" +
                    "\\caption[GO Table Label.} \\label{tab:go} \\\\\n" +
                    "\n" +
                    "\\hline" +
                    getHeaderForLongTable() +
                    " \\\\ \\hline \n" +
                    "\\endfirsthead\n" +
                    "\n" +
                    "\\multicolumn{3}{c}%\n" +
                    "{{\\bfseries \\tablename\\ \\thetable{} -- continued from previous page}} \\\\\n" +
                    "\\hline " +
                    getHeaderForLongTable() +
                    " \\\\ \\hline \n" +
                    "\\endhead\n" +
                    "\n" +
                    "\\hline \\multicolumn{3}{|r|}{{Continued on next page}} \\\\ \\hline\n" +
                    "\\endfoot\n" +
                    "\n" +
                    "\\hline \\hline\n" +
                    "\\endlastfoot\n");
            for (Protein p : this.proteins) {
                List<String> arr = new ArrayList<>();
                arr.add(p.geneSymbol);
                arr.add(String.valueOf(p.geneID));
                arr.add(p.uniprotID);
                arr.add(p.name);
                for (TermId tid : this.goTermMap.keySet()) {
                    if (p.isAnnotatedTo(tid)) {
                        arr.add("\\cellcolor{gray!25} \\OK");
                    } else {
                        arr.add(" ");
                    }
                }
                String row = String.join("&", arr);
                writer.write(row + "\\\\ \n");
            }
            writer.write("\\hline \n");
            writer.write("\\end{longtable}\n");
            writer.write("\\end{document}\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
