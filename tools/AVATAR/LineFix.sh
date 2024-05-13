#!/bin/bash

# The path of containing Defects4J bugs.
d4jData=/Users/matias/develop/post-overfitting/AVATAR/temp/

# The path of Defects4J git repository.
d4jPath=/Users/matias/develop/defects4jv2/ 

# The fault localization metric used to calculate suspiciousness value of each code line.
metric=Ochiai

# The buggy project ID: e.g., Chart_1
bugId=Math_85


java -Xmx4g -cp "target/Avatar-0.0.1-SNAPSHOT.jar:target/dependency/*" edu.lu.uni.serval.main.Main_Pos $d4jData $d4jPath $bugId $metric L `pwd`/outAvatarTest/ false