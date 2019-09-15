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

The app requires roughly 15 minutes to complete. The implementation of the algorithm is currently
not as effficient as possible.