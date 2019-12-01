package org.jax.gotools.mgsa;

import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertTrue;
import static org.jax.gotools.mgsa.MgsaParam.Type.MCMC;

public class MgsaParamTest {

    @Test
    void testMCMC() {
        MgsaParam param = new DoubleParam(MCMC, 0.3);
        assertTrue(param.isMCMC());

    }
}
