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

/*

RT
OS
RX
GN
OX
KW
DR
FT
RA
DT
RC
OC
RG
PE
RL
ID
RN
RP
SQ
*/
    private List<String> idLines = new ArrayList<>();
    private List<String> deLines = new ArrayList<>(); // definition lines
    private List<String> gnLines = new ArrayList<>(); // gene lines
    private List<String> acLines;

    public UniprotTextParser(String path) {
        uniprotTextPath = path;
        parse();
    }


    private void createEntry() {
        System.out.println("ENTRY");
    }


    private void parse() {
        Set<String> keys = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.uniprotTextPath))){
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                String key = line.substring(0,2);
                if (key.equals("//")) {
                    createEntry();
                }
                String restOfLine = line.substring(5);

                switch (key) {
                    case "ID":
                        idLines.add(restOfLine);
                        break;
                    case "DE":
                        deLines.add(restOfLine);
                        break;
                    case "GN":
                        gnLines.add(restOfLine);
                        break;
                    case "DT": // no op, Date
                    case "OS": // no op, should always be Homo sapiens
                    case "OC": // no op, genera

                }
                keys.add(key);
               // System.out.println(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String k : keys) {
            System.out.println(k);
        }
    }



}
