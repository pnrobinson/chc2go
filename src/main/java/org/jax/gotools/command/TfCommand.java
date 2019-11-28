package org.jax.gotools.command;

import org.jax.gotools.analysis.TranscriptionFactorCompare;
import org.jax.gotools.string.StringInfoParser;
import org.jax.gotools.string.StringParser;
import org.jax.gotools.tf.TfParser;
import org.jax.gotools.tf.TranscriptionFactor;
import org.jax.gotools.tf.UniprotEntry;
import org.jax.gotools.tf.UniprotTextParser;

import java.io.File;
import java.io.FileReader;
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
        List<UniprotEntry> upentrylist = upParser.getUpentries();
        String stringDir = "/Users/peterrobinson/Documents/data/string";
        String stringInfo = String.format("%s%s%s",stringDir, File.separator, "9606.protein.info.v11.0.txt.gz");
       // StringInfoParser siparser = new StringInfoParser(stringInfo);
        String stringLinks = String.format("%s%s%s", stringDir, File.separator, "9606.protein.links.v11.0.txt.gz");
        //StringParser sparser = new StringParser(stringLinks, positiveSignificant, negativeSignificant);

        TranscriptionFactorCompare.Builder builder = new TranscriptionFactorCompare.Builder()
                .transcriptionFactorListA(positiveSignificant)
                .transcriptionFactorListB(nonSignificant)
                .uniprotEntries(upentrylist);

        TranscriptionFactorCompare tfcompare = builder.build();
        tfcompare.parseString(stringLinks);
        String rbpPath = "/Users/peterrobinson/Documents/data/string/RBP.tsv";
        tfcompare.parseRBPs(rbpPath);
        tfcompare.compareSetAandB();
    }
}
