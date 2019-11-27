package org.jax.gotools.go;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TermIdPairTest {


    private static TermId tidA;
    private static TermId tidB;
    private static TermId tidC;
    private static TermId tidD;


    @BeforeAll
    static void init() {
        tidA = TermId.of("GO:0000123");
        tidB = TermId.of("GO:0000124");
        tidC = TermId.of("GO:0000125");
    }


    @Test
    void testEquality() {
        TermIdPair pair1 = new TermIdPair(tidA, tidB);
        TermIdPair pair2 = new TermIdPair(tidA, tidB);
        TermIdPair pair3 = new TermIdPair(tidB, tidC);
        TermIdPair pair4 = new TermIdPair(tidC, tidB);
        assertTrue(pair1.equals(pair2));
        assertFalse(pair1.equals(pair3));
        // order of terms reversed but they should still be equal
        assertTrue(pair3.equals(pair4));
    }
}
