package org.jax.gotools.tf;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class parses the TEXT output format of Uniprot
 * The XML file is too big for convenient parsing.
 * We are parsing just the 399 TFs from Guy's file
 * Here is how to get them
 * use strict;
 * use warnings;
 * use LWP::UserAgent;
 *
 * my $list = "identifiers.txt";# $ARGV[0]; # File containg list of UniProt identifiers.
 *
 * my $base = 'http://www.uniprot.org';
 * my $tool = 'uploadlists';
 *
 * my $contact = ''; # Please set a contact email address here to help us debug in case of problems (see https://www.uniprot.org/help/privacy).
 * my $agent = LWP::UserAgent->new(agent => "libwww-perl $contact");
 * push @{$agent->requests_redirectable}, 'POST';
 *
 * my $response = $agent->post("$base/$tool/",
 *                             [ 'file' => [$list],
 *                               'format' => 'txt',
 *                               'from' => 'ACC+ID',
 *                               'to' => 'ACC',
 *                             ],
 *                             'Content_Type' => 'form-data');
 *
 * while (my $wait = $response->header('Retry-After')) {
 *   print STDERR "Waiting ($wait)...\n";
 *   sleep $wait;
 *   $response = $agent->get($response->base);
 * }
 *
 * $response->is_success ?
 *   print $response->content :
 *   die 'Failed, got ' . $response->status_line .
 *     ' for ' . $response->request->uri . "\n";
 */
public class UniprotTextParser {
    private final String uniprotTextPath;

    private List<UniprotEntry> upentries = new ArrayList<>();

    private List<String> idLines;
    private List<String> deLines; // definition lines
    private List<String> gnLines; // gene lines
    private List<String> acLines; // accession number
    private List<String> ccLines; // comments, with function
    private List<String> drLines; // various cross refs
    private List<String> peLines; // evidence..
    private List<String> kwLines; // keywords
    private List<String> ftLines; // features


    public UniprotTextParser(String path) {
        uniprotTextPath = path;
        resetLists();
        parse();
       //debugPrintEntries();
        System.out.printf("We got %d UniProt entries.\n",upentries.size());
    }



    private void debugPrintEntries() {
        int c = 0;
        for (UniprotEntry e : upentries) {
            c++;
            System.out.println(c +": "+e);
        }
    }

    private void resetLists() {
        idLines = new ArrayList<>();
        deLines = new ArrayList<>();
        gnLines = new ArrayList<>();
        acLines = new ArrayList<>();
        ccLines = new ArrayList<>();
        drLines = new ArrayList<>();
        peLines = new ArrayList<>();
        kwLines = new ArrayList<>();
        ftLines = new ArrayList<>();
    }


    private void createEntry() {
        UniprotEntry entry = new
            UniprotEntry(idLines,
                deLines,
                gnLines,
                acLines,
                ccLines,
                drLines,
                peLines,
                kwLines,
                ftLines);
        upentries.add(entry);
        resetLists();
    }


    private void parse() {
        boolean sequenceMode = false; // SQ starts a block where the keyword is not repeated
        try (BufferedReader br = new BufferedReader(new FileReader(this.uniprotTextPath))){
            String line;
            while ((line = br.readLine()) != null) {
                String key = line.substring(0,2);
                if (key.equals("//")) {
                    createEntry();
                    sequenceMode = false;
                    continue;
                }
                if (sequenceMode) {
                    continue;
                }
                if (line.length() < 6) {
                    System.err.println("Short line: " + line);
                    continue;
                }
                String restOfLine = line.substring(5);

                switch (key) {
                    case "AC":
                        acLines.add(restOfLine);
                        break;
                    case "ID":
                        idLines.add(restOfLine);
                        break;
                    case "DE":
                        deLines.add(restOfLine);
                        break;
                    case "GN":
                        gnLines.add(restOfLine);
                        break;
                    case "CC":
                        ccLines.add(restOfLine);
                        break;
                    case "DR":
                        drLines.add(restOfLine);
                        break;
                    case "PE":
                        peLines.add(restOfLine);
                        break;
                    case "KW":
                        kwLines.add(restOfLine);
                        break;
                    case "FT":
                        ftLines.add(restOfLine);
                        break;
                    case "DT": // no op, Date
                    case "OS": // no op, should always be Homo sapiens
                    case "OC": // no op, genera
                    case "OX": // no op, taxon
                    case "RN":
                    case "RP": // no open, references
                    case "RA":
                    case "RT":
                    case "RX":
                    case "RL":
                    case "RC":
                    case "RG":
                        break;
                    case "SQ": // skip sequence, we do not need it now
                        sequenceMode = true;
                        break;
                    default:
                        System.out.println(key + ":" + restOfLine);
                        System.exit(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<UniprotEntry> getUpentries() {
        return upentries;
    }
}
