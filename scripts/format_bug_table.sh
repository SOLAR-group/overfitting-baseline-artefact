f=results/result_by_Bug_only.csv
h=$(awk 'END{print FNR/2+1;}' $f)
t=$(awk 'END{print FNR/2-1;}' $f)
head -n $h $f > out1
tail -n $t $f > out2
cat bug_table_header.txt out2 > out3
paste out1 out3
rm out1
rm out2
rm out3
