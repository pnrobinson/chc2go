package org.jax.gotools.io;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Command to download the {@code hp.obo} and {@code phenotype.hpoa} files that
 * we will need to run the LIRICAL approach.
 */
public class Downloader {
    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);
    /** Directory to which we will download the files. */
    private final String downloadDirectory;
    /** If true, download new version whether or not the file is already present. */
    private final boolean overwrite;

    private final static String GO_JSON = "go.json";
    /** URL of the hp.obo file. */
    private final static String GO_JSON_URL ="http://purl.obolibrary.org/obo/go.json";
    /** URL of the annotation file phenotype.hpoa. */
    private final static String GO_ANNOTATION_URL ="http://geneontology.org/gene-associations/goa_human.gaf.gz";
    /** Basename of the phenotype annotation file. */
    private final static String GO_ANNOTATION_GZIP ="goa_human.gaf.gz";
    private final static String GO_ANNOTATION ="goa_human.gaf";

    private final static String GENE_INFO = "Homo_sapiens_gene_info.gz";

    private final static String GENE_INFO_URL = "ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz";

    private final static String MIM2GENE = "mim2gene_medgen";

    private final static String MIM2GENE_URL = "ftp://ftp.ncbi.nih.gov/gene/DATA/mim2gene_medgen";


    public Downloader(String path){
        this(path,false);
    }

    public Downloader(String path, boolean overwrite){
        this.downloadDirectory=path;
        this.overwrite=overwrite;
        logger.trace("overwrite="+overwrite);
    }

    /**
     * Download the files unless they are already present.
     */
    public void download() {
        downloadFileIfNeeded(GO_JSON, GO_JSON_URL);
        downloadFileIfNeeded(GO_ANNOTATION_GZIP, GO_ANNOTATION_URL);
        gunzipFileIfNeeded(GO_ANNOTATION_GZIP, GO_ANNOTATION);
        downloadFileIfNeeded(GENE_INFO,GENE_INFO_URL);
        downloadFileIfNeeded(MIM2GENE, MIM2GENE_URL);

    }


    private void gunzipFileIfNeeded(String fname, String outname) {
        String inWithDirectory = String.format("%s%s%s",downloadDirectory,File.separator,fname);
        String outWithDirectory = String.format("%s%s%s",downloadDirectory,File.separator,outname);

        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(inWithDirectory))){
            try (FileOutputStream out = new FileOutputStream(outWithDirectory)){
                byte[] buffer = new byte[1024];
                int len;
                while((len = in.read(buffer)) != -1){
                    out.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // cannot recover
        }
    }







    private void downloadFileIfNeeded(String filename, String webAddress) {
        File f = new File(String.format("%s%s%s",downloadDirectory,File.separator,filename));
        if (f.exists() && (! overwrite)) {
            System.err.printf("Cowardly refusing to download %s since we found it at %s.\n",
                    filename,
                    f.getAbsolutePath());
            logger.trace(String.format("Cowardly refusing to download %s since we found it at %s",
                    filename,
                    f.getAbsolutePath()));
            return;
        }
        FileDownloader downloader=new FileDownloader();
        try {
            URL url = new URL(webAddress);
            logger.debug("Created url from "+webAddress+": "+url.toString());
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException e) {
            logger.error(String.format("Malformed URL for %s [%s]",filename, webAddress));
            logger.error(e.getMessage());
        } catch (FileDownloadException e) {
            logger.error(String.format("Error downloading %s from %s" ,filename, webAddress));
            logger.error(e.getMessage());
        }
        System.out.println("[INFO] Downloaded " + filename);
    }

}

