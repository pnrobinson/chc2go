package org.jax.gotools.command;

import org.jax.gotools.tf.TfParser;
import org.jax.gotools.tf.TranscriptionFactor;
import org.jax.gotools.tf.UniprotTextParser;

import java.util.List;

public class TfCommand extends Chc2GoCommand {



    public TfCommand() {}


    @Override
    public void run() {
        // to develop hard code path
        // Guy's output file
        String pathToTfFile = "src/test/resources/logistic_ranking.txt";
        // 399 TF proteins
        String pathToUniprotFile = "src/test/resources/TF.uprot.txt";
        TfParser tfParser = new TfParser(pathToTfFile);
        List<TranscriptionFactor> positiveSignificant = tfParser.getPositiveSignificant();
        List<TranscriptionFactor> negativeSignificant = tfParser.getNegativeSignificant();
        List<TranscriptionFactor> nonSignificant = tfParser.getNonSignificant();
        UniprotTextParser upParser = new UniprotTextParser(pathToUniprotFile);
    }
}
