#!/bin/bash
rm -f prf*.tsv
i=0
while read q ; do
i=$((i + 1));
prfout=prf-$i.tsv;
curl "http://localhost:25803/prf?query=$q&ranker=comprehensive&numdocs=10&numterms=10" > $prfout;
echo $q:$prfout >> prf.tsv
done < queries.tsv
java -cp "src:./lib/*" -Xmx512m edu.nyu.cs.cs2580.Bhattacharyya prf.tsv qsim.tsv
