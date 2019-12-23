package org.jax.gotools.chc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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

    /** One of the hundred lines was valid. */
    @Test
    void testThat100InteractionsAreParsed() {
        List<ChcInteraction> interactions = parser.getInteractions();
        assertEquals(1,interactions.size());
    }

    /**
     * chr17:72411026-72411616;chr17:72712662-72724357	301046	T	FAKE1;FAKE2	6:5	AA	0.69	-1/-1	;
     */
    @Test
    void testFirstInteraction() {
        List<ChcInteraction> interactions = parser.getInteractions();
        ChcInteraction interaction1 = interactions.get(0);
        int expectedDistance = 301046;
        assertEquals(expectedDistance, interaction1.getDistance());
        ChcInteraction.InteractionType expectedInteractionType = ChcInteraction.InteractionType.TWISTED;
        assertEquals(expectedInteractionType, interaction1.getItype());
        List<String> geneListA = new ArrayList<>(); geneListA.add("FAKE1");
        assertEquals(geneListA, interaction1.getGenelistA());
        List<String> geneListB = new ArrayList<>();  geneListB.add("FAKE2");
        assertEquals(geneListB, interaction1.getGenelistB());
        assertEquals("chr17:72411026-72411616", interaction1.getPosA());
        assertEquals("chr17:72712662-72724357", interaction1.getPosB());
        assertEquals(6, interaction1.getSimple());
        assertEquals(5, interaction1.getTwisted());
    }


}
