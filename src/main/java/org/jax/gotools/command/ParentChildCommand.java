package org.jax.gotools.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.analysis.DirectAndIndirectTermAnnotations;
import org.monarchinitiative.phenol.analysis.StudySet;
import org.monarchinitiative.phenol.annotations.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.stats.GoTerm2PValAndCounts;
import org.monarchinitiative.phenol.stats.ParentChildIntersectionPValueCalculation;
import org.monarchinitiative.phenol.stats.ParentChildUnionPValueCalculation;
import org.monarchinitiative.phenol.stats.TermForTermPValueCalculation;
import org.monarchinitiative.phenol.stats.mtc.Bonferroni;
import org.monarchinitiative.phenol.stats.mtc.MultipleTestingCorrection;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Parameters(commandDescription = "Run pairwise analysis of diachromatic output files")
public class ParentChildCommand extends GoToolsCommand {

    private static final double THRESHOLD = 0.05;

    @Parameter(names = {"-s", "--study"}, description = "path to study set", required = true)
    protected String studyPath;

    @Parameter(names = {"-p", "--population"}, description = "path to population set", required = true)
    private String populationPath;

    @Override
    public void run() {
        initGoPathsToDefault();
        File studyFile = new File(studyPath);
        File populationFile = new File(populationPath);
        if (! studyFile.exists()) {
            throw new PhenolRuntimeException("Could not find study file");
        }
        if (! populationFile.exists()) {
            throw new PhenolRuntimeException("Could not find population file");
        }
        runParentChild(studyFile, populationFile);
    }


    private void runParentChild(File study, File population) {
        Ontology geneOntology = OntologyLoader.loadOntology(new File(goOboPath));
        int n_terms = geneOntology.countAllTerms();
        System.out.println("[INFO] parsed " + n_terms + " GO terms.");
        System.out.println("[INFO] parsing  " + goGafPath);
        List<TermAnnotation> goAnnots = GoGeneAnnotationParser.loadTermAnnotations(goGafPath);
        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
        AssociationContainer associationContainer = AssociationContainer.loadGoGafAssociationContainer(goGafPath);
        int n = associationContainer.getTotalNumberOfAnnotatedItems();
        System.out.println("[INFO] parsed " + n + " annotated terms");

        StudySet studySet = getStudySetFromFile(study, "study", associationContainer, geneOntology);
        StudySet populationSet = getStudySetFromFile(population, "population" , associationContainer, geneOntology);
        System.out.printf("[INFO] Study set with %d entries, pop set with %d.\n", studySet.getAnnotatedItemCount(), populationSet.getAnnotatedItemCount());
        MultipleTestingCorrection bonf = new Bonferroni();

        System.out.println("########### PC Intersect ###################");

        ParentChildIntersectionPValueCalculation pci =
                new ParentChildIntersectionPValueCalculation(geneOntology, associationContainer,populationSet, studySet, bonf);
        System.out.println(GoTerm2PValAndCounts.header());
        int totalStudy = studySet.getAnnotatedItemCount();
        int totalPopulation = populationSet.getAnnotatedItemCount();
        for (GoTerm2PValAndCounts gt : pci.calculatePVals()) {
            if (! gt.passesThreshold(THRESHOLD)) {
                continue;
            }
            System.out.println(gt.getRow(geneOntology, totalStudy, totalPopulation));
        }
        System.out.println("########### PC Union ###################");
        ParentChildUnionPValueCalculation pcu =
                new ParentChildUnionPValueCalculation(geneOntology,
                        associationContainer,populationSet,
                        studySet, bonf);
        System.out.println(GoTerm2PValAndCounts.header());
        for (GoTerm2PValAndCounts gt : pcu.calculatePVals()) {
            if (! gt.passesThreshold(THRESHOLD)) {
                continue;
            }
            System.out.println(gt.getRow(geneOntology, totalStudy, totalPopulation));
        }
        System.out.println("########### TermForTerm ###################");
        TermForTermPValueCalculation tft =
                new TermForTermPValueCalculation(geneOntology, associationContainer, populationSet, studySet, bonf);
        System.out.println(GoTerm2PValAndCounts.header());
        for (GoTerm2PValAndCounts gt : tft.calculatePVals()) {
            if (! gt.passesThreshold(THRESHOLD)) {
                continue;
            }
            System.out.println(gt.getRow(geneOntology, totalStudy, totalPopulation));
        }
    }

    private StudySet getStudySetFromFile(File path, String name, AssociationContainer acontainer, Ontology ontology) {
        Set<TermId> genes = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                String [] fields = line.split(("\\s+"));
                String gene = fields[0]; // assume gene symbol is in first field
                genes.add(TermId.of(gene));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<TermId, DirectAndIndirectTermAnnotations> studyAssociations = acontainer.getAssociationMap(genes, ontology);
        return new StudySet(genes, name, studyAssociations);
    }

}
