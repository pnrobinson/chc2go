package org.jax.gotools.chc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ChcInteractionParserTest {

    private static ChcInteractionParser parser;

    // the file has the first 100 lines of the mifsud file
    @BeforeAll
    static void init() {
        ClassLoader classLoader = ChcInteractionParserTest.class.getClassLoader();
        File file = new File(classLoader.getResource("small_interactions_with_genesymbols.tsv").getFile());
        parser = new ChcInteractionParser(file.getAbsolutePath());
    }

    @Test
    void testConstructor() {
        assertNotNull(parser);
    }

    @Test
    void testThat100InteractionsAreParsed() {
        List<ChcInteraction> interactions = parser.getInteractions();
        assertEquals(100,interactions.size());
    }

    /**
     * chr1:109040207-109050776;chr1:109546097-109552254	495321	NA	WDR47;GNAI3	1:4	AA
     */
    @Test
    void testFirstInteraction() {
        List<ChcInteraction> interactions = parser.getInteractions();
        ChcInteraction interaction1 = interactions.get(0);
        int expectedDistance = 495321;
        assertEquals(expectedDistance, interaction1.getDistance());
        ChcInteraction.InteractionType expectedInteractionType = ChcInteraction.InteractionType.INDEFINABLE;
        assertEquals(expectedInteractionType, interaction1.getItype());
        List<String> geneListA = List.of("WDR47");
        assertEquals(geneListA, interaction1.getGenelistA());
        List<String> geneListB = List.of("GNAI3");
        assertEquals(geneListB, interaction1.getGenelistB());
        assertEquals("chr1:109040207-109050776", interaction1.getPosA());
        assertEquals("chr1:109546097-109552254", interaction1.getPosB());
        assertEquals(1, interaction1.getSimple());
        assertEquals(4, interaction1.getTwisted());
    }


}
