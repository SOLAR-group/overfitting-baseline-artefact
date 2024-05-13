firstpatches/./first_patches.sh 1 > out1
firstpatches/./first_patches.sh 2 > out2
firstpatches/./first_patches.sh 3 > out3
firstpatches/./first_patches.sh 4 > out4
firstpatches/./first_patches.sh 5 > out5
firstpatches/./first_patches.sh 20 > out20
firstpatches/./first_patches.sh 21 > out21
firstpatches/./first_patches.sh 36 > out36
firstpatches/./first_patches.sh 37 > out37
paste -d ',' firstpatches/toolnames out1 out2 out3 out4 out5 out20 out21 out36 out37
rm out1
rm out2
rm out3
rm out4
rm out5
rm out20
rm out21
rm out36
rm out37
