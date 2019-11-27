package org.jax.chc2go.mgsa;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.analysis.AssociationContainer;
import org.monarchinitiative.phenol.analysis.StudySet;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.obo.go.GoGeneAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Bayes2GoCalculationTest {




    StudySet getFakeStudySet(AssociationContainer associationContainer, Ontology ontology ) {
        Set<TermId> allAnnotatedGenes = associationContainer.getAllAnnotatedGenes();
        Set<TermId> fakeStudyGenes = new HashSet<>();
        Random r = new Random();
        for (TermId tid : allAnnotatedGenes) {
            double rd = r.nextDouble();
            if (rd < 0.05) {
                fakeStudyGenes.add(tid);
            }
        }
        return new StudySet(fakeStudyGenes, "fake", associationContainer, ontology);
    }

    @Test
    void testMgsa() throws PhenolException {
        String localPathToGoObo = "/home/robinp/data/go/go.obo";
        Ontology ontology = OntologyLoader.loadOntology(new File(localPathToGoObo));
        System.out.printf("[INFO] loaded ontology with %d terms.\n", ontology.countNonObsoleteTerms());
        String localPathToGoGaf = "/home/robinp/data/go/goa_human.gaf";
        final GoGeneAnnotationParser annotparser = new GoGeneAnnotationParser(localPathToGoGaf);
        List<TermAnnotation> goAnnots = annotparser.getTermAnnotations();
        System.out.println("[INFO] parsed " + goAnnots.size() + " GO annotations.");
        AssociationContainer associationContainer = new AssociationContainer(goAnnots);

        Bayes2GOCalculation b2gCalc = new Bayes2GOCalculation();
        StudySet populationSet = new StudySet(associationContainer.getAllAnnotatedGenes(),"population", associationContainer, ontology);
        StudySet study = getFakeStudySet(associationContainer,ontology);
        b2gCalc.calculateStudySet(ontology, associationContainer, populationSet, study);
        assertTrue(true);
    }
}
