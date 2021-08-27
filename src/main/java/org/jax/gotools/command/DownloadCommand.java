package org.jax.gotools.command;

import org.jax.gotools.io.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "download", aliases = {"D"},
        mixinStandardHelpOptions = true,
        description = "download files for GOtools")
public class DownloadCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);
    @CommandLine.Option(names = {"-d", "--data"}, description = "path to data download file")
    protected String dataDir = "data";
    @CommandLine.Option(names = {"-w", "--overwrite"}, description = "overwrite previously downloaded files?")
    private boolean overwrite;

    public DownloadCommand() {
    }


    @Override
    public Integer call() {
        logger.info(String.format("Download analysis to %s", dataDir));
        Downloader downloader = new Downloader(dataDir, overwrite);
        downloader.download();
        return 0;
    }
}

