import os
from collections import defaultdict

outfilename = 'isopret_term_counts.tsv'
dirpath = '/home/robinp/data/isopret'

files = ['isoform_function_list_bp.txt', 'isoform_function_list_mf.txt', 'isoform_function_list_cc.txt']

term_to_annotation_count_d = defaultdict(int)
n_annot = 0

for file in files:
    fullpath = os.path.join(dirpath, file)
    with open(fullpath) as f:
        for line in f:
            fields = line.rstrip().split('\t')
            if len(fields) == 2 and fields[1].startswith('GO:'):
                go_id = fields[1]
                term_to_annotation_count_d[go_id] += 1
                n_annot += 1
                
print(f"We parsed a total of {len(term_to_annotation_count_d)} distinct terms from {n_annot} annotations")

fh = open(outfilename, 'wt')
for k, v in term_to_annotation_count_d.items():
    fh.write(f"{k}\t{v}\n")
