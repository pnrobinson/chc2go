package org.jax.gotools.tf;


import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
        ImmutableList.Builder positiveBuilder = new ImmutableList.Builder<>();
        ImmutableList.Builder negativeBuilder = new ImmutableList.Builder<>();
        ImmutableList.Builder nonBuilder = new ImmutableList.Builder<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.pathToTfFile))) {
            String line = br.readLine(); // should be the header
            if (! line.startsWith("Estimate")) {
                throw new RuntimeException("Bad header line: " + line);
            }

            while ((line = br.readLine()) != null) {
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
        positiveSignificant = positiveBuilder.build();
        negativeSignificant = negativeBuilder.build();
        nonSignificant = nonBuilder.build();
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
