package org.jax.gotools.tf;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse a hand-crafted format that divided Transcription factors into Up and Down
 */
public class TfParser {

    private final String pathToTfFile;

    private List<TranscriptionFactor> positiveSignificant;
    private List<TranscriptionFactor> negativeSignificant;
    private List<TranscriptionFactor> nonSignificant;


    public TfParser(String path) {
        pathToTfFile = path;
        parse();
        System.out.printf("[INFO] %d positive significant, %d negative significant, %d non significant TFs parsed.\n",
                positiveSignificant.size(), negativeSignificant.size(), nonSignificant.size());
    }


    private void parse() {
        List<TranscriptionFactor> positiveBuilder = new ArrayList<>();
        List<TranscriptionFactor>  negativeBuilder = new ArrayList<>();
        List<TranscriptionFactor>  nonBuilder = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.pathToTfFile))) {
            String line = br.readLine(); // should be the header
            if (! line.startsWith("Estimate")) {
                throw new RuntimeException("Bad header line: " + line);
            }

            while ((line = br.readLine()) != null) {
                if (line.startsWith("(Intercept)")) {
                    System.err.println("[WARNING] Skipping line " + line);
                    continue;
                }
                String [] fields = line.split("\t");
                TranscriptionFactor tf = new TranscriptionFactor(fields);
                if (tf.significant()) {
                    if (tf.positive()) {
                        positiveBuilder.add(tf);
                    } else {
                        negativeBuilder.add(tf);
                    }
                } else {
                    nonBuilder.add(tf);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        positiveSignificant = List.copyOf(positiveBuilder);
        negativeSignificant = List.copyOf(negativeBuilder);
        nonSignificant = List.copyOf(nonBuilder);
    }

    public List<TranscriptionFactor> getPositiveSignificant() {
        return positiveSignificant;
    }

    public List<TranscriptionFactor> getNegativeSignificant() {
        return negativeSignificant;
    }

    public List<TranscriptionFactor> getNonSignificant() {
        return nonSignificant;
    }
}
