#!/bin/bash
rm -f prf*.tsv
i=0
while read q ; do
i=$((i + 1));
prfout=prf-$i.tsv;
curl "http://localhost:12306/prf?query=$q&ranker=favorite&numdocs=10&numterms=5" > $prfout;
echo $q:$prfout >> prf.tsv
done < queries.tsv
