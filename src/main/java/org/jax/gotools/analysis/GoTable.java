package org.jax.gotools.analysis;


import org.apache.commons.lang.builder.HashCodeBuilder;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class GoTable {

    private final Ontology geneOntology;
    /**
     * Use by the GO framework to store annotations.
     */
    private List<Protein> proteins;

    private Map<TermId, String> goTermMap;

    private Map<String, Protein> uprotMap;
    /** Key, a symbol such as FBN1. Value. The corresponding NCBI Entrez Gene id, e.g. 2200. */
    private Map<String, Integer> symbolToEntrezGeneIdMap;

    private Map<Integer, String> entrzGeneId2SymbolMap;

    private Map<Integer, List<Integer> >geneId2mimIdMap;


    public GoTable(String goObo, String goGaf, String geneInfoPath, String mim2genePath, Map<TermId, String> goTermMap) {
        File goOboFile = new File(goObo);
        // GoGaf file.
        File goGafFile = new File(goGaf);
        if (!goOboFile.exists()) {
            throw new RuntimeException("Could not find go.obo file");
        }
        if (!goGafFile.exists()) {
            throw new RuntimeException("Could not find go.gaf file");
        }
        System.out.println("[INFO] Parsing GO data.....");
        this.geneOntology = OntologyLoader.loadOntology(goOboFile, "GO");
        int n_terms = geneOntology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        System.out.println("[INFO] parsing  " + goGafFile.getAbsolutePath());
        this.goTermMap = goTermMap;
        this.uprotMap = new HashMap<>();
        parseGeneInfo(geneInfoPath);
        parseMim2Gene(mim2genePath);
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
                if (!category.equals("protein"))
                    continue;
                Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(this.geneOntology, goTerm, true);
                for (TermId targetID : this.goTermMap.keySet()) {
                    if (ancs.contains(targetID)) {
                        // we have a match!
                        int geneId = this.symbolToEntrezGeneIdMap.getOrDefault(symbol,-1);
                        if (geneId < 0) {
                            System.err.println("[WARNING] Could not retrieve id for gene " + symbol);
                            continue; // Skip genes without geneIDs
                        }
                        this.uprotMap.putIfAbsent(uprotID, new Protein(symbol, geneId, uprotID, label));
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
            return geneSymbol.compareTo(other.geneSymbol);
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
            if (!(obj instanceof Protein)) return false;
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
            String h = String.format("\\rot{%s}", s);
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
            //String h = String.format("\\rot{%s}", s);
            header.add(headerField(s));
        }
        return String.join(" & ", header);
    }


    public void outputLatexLongTableToFile(String filename) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("\\documentclass{article} \n");
            writer.write("\\usepackage[table]{xcolor} \n");
            writer.write("\\usepackage{longtable} \n");
            writer.write("\\usepackage{array,graphicx} \n");
            writer.write("\\usepackage{pifont} \n");
            writer.write("\\newcommand*\\rot{\\rotatebox{90}}\n");
            writer.write("\\newcommand*\\OK{\\ding{51}}\n\n");
            writer.write("\\begin{document}\n");

            String fields = "{lllp{6cm}|";
            for (int i = 0; i < this.goTermMap.size(); i++) {
                fields += "l";
            }
            fields += "|}";
            int number_of_fixed_fields = 4; // gene, uniprotid, entrez id, name
            int totalcolumns = this.goTermMap.size() + number_of_fixed_fields;

            writer.write("\\begin{longtable}" + fields + "\n" +
                    "\\caption{GO Table Label.} \\label{tab:go} \\\\\n" +
                    "\n" +
                    "\\hline \n " +
                    getHeaderForLongTable() +
                    " \\\\ \\hline \n" +
                    "\\endfirsthead\n" +
                    "\n" +
                    "\\multicolumn{" + totalcolumns + "}{c}%\n" +
                    "{{\\bfseries \\tablename\\ \\thetable{} -- continued from previous page}} \\\\\n" +
                    "\\hline " +
                    getHeaderForLongTable() +
                    " \\\\ \\hline \n" +
                    "\\endhead\n" +
                    "\n" +
                    "\\hline \\multicolumn{" + totalcolumns + "}{|r|}{{Continued on next page}} \\\\ \\hline\n" +
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
                        arr.add("\\cellcolor{gray!25} \\OK ");
                    } else {
                        arr.add(" ");
                    }
                }
                String row = String.join(" & ", arr);
                writer.write(row + " \\\\ \n");
            }
            writer.write("\\hline \n");
            writer.write("\\end{longtable}\n");
            writer.write("\\end{document}\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void parseMim2Gene(String path) {
        geneId2mimIdMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                String []A = line.split("\t");
                if (! A[2].equals("phenotype")) {
                    continue;
                }
                Integer mimid = Integer.parseInt(A[0]);
                String geneIdStr = A[1];
                if (geneIdStr.equals("-")) {
                    continue;
                }
                Integer geneId = Integer.parseInt(geneIdStr);
                geneId2mimIdMap.putIfAbsent(geneId,new ArrayList<>());
                List<Integer> mimlist = geneId2mimIdMap.get(geneId);
                mimlist.add(mimid);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void parseGeneInfo(String path) {
        this.symbolToEntrezGeneIdMap = new HashMap<>();
        this.entrzGeneId2SymbolMap = new HashMap<>();
        try {
            InputStream fileStream = new FileInputStream(path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream);
            BufferedReader br = new BufferedReader(decoder);
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("9606")) {
                    // wrong subspecies
                    continue;
                }
                String A[] = line.split("\t");
                if (A.length < 10) {
                    continue;
                }
                Integer id = Integer.parseInt(A[1]);
                String sym = A[2];
                this.symbolToEntrezGeneIdMap.put(sym, id);
                this.entrzGeneId2SymbolMap.put(id, sym);


            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERROR] COuld not open gziped Gene file");
            System.exit(1);
        }
        System.out.printf("[INFO] Parsed %d gene symbols.\n", this.symbolToEntrezGeneIdMap.size());
    }

    private static<K> void increment(Map<K, Integer> map, K key) {
        map.putIfAbsent(key, 0);
        map.put(key, map.get(key) + 1);
    }

    public void outputTSV(String path,Map<TermId, String> goId2Labels ) {
        Map<TermId, Integer> annotcounts = new HashMap<>();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            // header
            String categories = String.join("\t", goId2Labels.values());
            writer.write("Gene\tuprot.id\tgene.id\t" + categories + "\tomim\n");
            for (Protein p : this.proteins) {
                // output to tsv file
                List<String> annotated = new ArrayList<>();
                for (TermId t: goId2Labels.keySet()) {
                    if (p.isAnnotatedTo(t)) {
                        annotated.add("T");
                        increment(annotcounts, t);
                    } else {
                        annotated.add("F");
                    }
                }
                String mim = "n/a";
                if (this.geneId2mimIdMap.containsKey(p.geneID)) {
                    mim = this.geneId2mimIdMap.get(p.geneID).stream().map(String::valueOf).collect(Collectors.joining(";"));
                }
                String annots = String.join("\t",annotated);
                writer.write(String.format("%s\t%s\t%d\t%s\t%s\n",p.geneSymbol,p.uniprotID,p.geneID,annots, mim));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (TermId t: annotcounts.keySet()) {
            System.out.printf("[INFO] %s: %d annotations.\n", geneOntology.getTermMap().get(t).getName(),
                    annotcounts.get(t));
        }
    }


}
