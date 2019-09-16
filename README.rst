######
chc2go
######

Capture Hi-C interactions to Gene Ontology analysis

Setup
~~~~~

CHC2GO is a Java 8 command-line application that analyzes functional similarity of
interacting digests in Capture Hi-C experiments. To set up the program, clone it from
github and build it using maven.



.. code-block:: bash

    $ git clone https://github.com/pnrobinson/chc2go.git
    $ cd chc2go
    $ mvn package


Gene Ontology Files
~~~~~~~~~~~~~~~~~~~
Download the Gene Ontology obo file from http://geneontology.org/docs/download-ontology/.
Download the Gene Ontology human annotation file
from http://current.geneontology.org/products/pages/downloads.html (download ``goa_human.gag.gz``).
Unpack the gzip file

.. code-block:: bash

    $ gunzip goa_human.gag.gz

Running the app
~~~~~~~~~~~~~~~

TODO -- describe how to create the input file mifsud_at_least_two_original_interactions_with_genesymbols.tsv

We can now run the app as follows.


.. code-block:: java

    $ java -jar CHC2GO.jar -c diachromatic/mifsud_at_least_two_original_interactions_with_genesymbols.tsv \
        -g go.obo -a goa_human.gaf

Note that the paths should be adjusted appropriately.

The app requires roughly **39 minutes** to complete. The implementation of the algorithm is currently
not as efficient as possible.

The output looks like this

######################overall######################


Number of observations: undirected: 74209 SIMPLE: 271
Undirected mean: 0.70, median 0.64, SIMPLE mean: 0.79, SIMPLE median: 0.74, T-test p-value 7.759184e-03

Number of observations: undirected: 74209 NO_GENE_IN_DIGEST: 2
Undirected mean: 0.70, median 0.64, NO_GENE_IN_DIGEST mean: -1.00, NO_GENE_IN_DIGEST median: -1.00, T-test p-value 0.000000e+00

Number of observations: undirected: 74209 UNKNOWN: 10464
Undirected mean: 0.70, median 0.64, UNKNOWN mean: 0.66, UNKNOWN median: 0.62, T-test p-value 2.344355e-12

Number of observations: undirected: 74209 TWISTED: 371
Undirected mean: 0.70, median 0.64, TWISTED mean: 0.77, TWISTED median: 0.69, T-test p-value 1.242560e-02