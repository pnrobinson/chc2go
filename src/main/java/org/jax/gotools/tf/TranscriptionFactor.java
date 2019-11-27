package org.jax.gotools.tf;

/**
 * A POJO to encapsulate information about a Transcription factor and its relation to our splicing/expression groups
 * Estimate	Std. Error	z value	Pr(>|z|)
 * ZFX_HUMAN	0.525909405831321	0.0934589926391215	5.62716749860599	1.83192823700686e-08
 */
public class TranscriptionFactor {

    private final String name;
    private final double coefficient;
    private final double error;
    private final double Zvalue;
    private final double Pvalue;

    private final static double PVALUE_THRESHOLD = 0.05;

    public TranscriptionFactor(String [] fields) {
        if (fields.length != 5) {
            throw new RuntimeException("Bad number of fields: " + fields.length);
        }
        name = fields[0];
        coefficient = Double.parseDouble(fields[1]);
        error = Double.parseDouble(fields[2]);
        Zvalue = Double.parseDouble(fields[3]);
        Pvalue = Double.parseDouble(fields[4]);
    }


    public boolean positive() {
        return coefficient > 0;
    }

    public boolean significant() {
        return Pvalue < PVALUE_THRESHOLD;
    }
}
