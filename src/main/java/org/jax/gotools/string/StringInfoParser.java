package org.jax.gotools.string;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class StringInfoParser {

    //9606.protein.info.v11.0.txt.gz
    private final String pathToGzippedProteinInfoFile;

    public StringInfoParser(String gzipPath) {
        pathToGzippedProteinInfoFile = gzipPath;
        parse();
    }


    private void parse() {
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(this.pathToGzippedProteinInfoFile))));
            String line;
            while ((line=br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
