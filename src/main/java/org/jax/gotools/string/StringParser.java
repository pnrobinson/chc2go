package org.jax.gotools.string;

import com.google.common.collect.ImmutableSet;
import org.jax.gotools.tf.TranscriptionFactor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class StringParser {

    private final Integer STRING_QUALITY_THRESHOLD = 700;

    //9606.protein.links.v11.0.txt.gz
    private final String pathToGzippedStringFile;
    private final Set<Integer> targetSetA;
    private final Set<Integer> targetSetB;


    private final List<PPI> setAppis;

    private final List<PPI> setBppis;

    public StringParser(String gzipPath, Set<Integer> targetsA, Set<Integer> targetsB) {
        this.pathToGzippedStringFile = gzipPath;
        targetSetA = targetsA;
        targetSetB = targetsB;
        System.out.printf("Target set A n=%d set B n=%d", targetsA.size(), targetsB.size());
        setAppis = new ArrayList<>();
        setBppis = new ArrayList<>();
        parse();
    }

    public List<PPI> getSetAppis() {
        return setAppis;
    }

    public List<PPI> getSetBppis() {
        return setBppis;
    }

    private void parse() {
        int c = 0;
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(this.pathToGzippedStringFile))));
            String line = br.readLine(); // header
            while ((line=br.readLine()) != null) {
                //System.out.println(line);
                c++;
                String []F = line.split("\\s+");
                if (F.length != 3) {
                    throw new RuntimeException("Bad String line: " + line + ", length=" + F.length);
                }
                Integer score = Integer.parseInt(F[2]);
                if (score < STRING_QUALITY_THRESHOLD) {
                    continue;
                }
                Integer ensp1 = ensp2int(F[0]);
                Integer ensp2 = ensp2int(F[1]);
                if ( targetSetA.contains(ensp1) || targetSetA.contains(ensp2) ) {
                    PPI ppi = new PPI(ensp1, ensp2);
                    setAppis.add(ppi);
                    //System.out.println("Adding PPI: " + ppi);
                }
                if ( targetSetB.contains(ensp1) || targetSetB.contains(ensp2) ) {
                    PPI ppi = new PPI(ensp1, ensp2);
                    setBppis.add(ppi);
                   // System.out.println("Adding PPI: " + ppi);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("[INFO] Parsed %d STRING PPIs\n", c);
        System.out.printf("[INFO] SetA (n=%d): %d PPIS, Set B (n=%d): %d PPIS\n", targetSetA.size(), setAppis.size(), targetSetB.size(),setBppis.size());
    }

    private Integer ensp2int(String ensp) {
        if (ensp.startsWith("9606.ENSP")) {
            return Integer.parseInt(ensp.substring(9));
        } else {
            throw new RuntimeException("Could not parse ENSP in STRING file" + ensp);
        }
    }
}
