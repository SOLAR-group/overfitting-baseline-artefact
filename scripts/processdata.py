import sys
import pandas as pd
from scipy.stats import hypergeom as hg
from statistics import median

root='results/'

# aggregated by x, tool or bug
x = sys.argv[1]
y = sys.argv[2]
sample = int(sys.argv[3])

df = pd.read_csv(root+'preprocessed.csv')

cols = df.columns
for col in cols[3:]:
  df = df.astype({col: 'int64'})

dfres =pd.DataFrame({'paper':[]
                    ,x:[]
                    ,y+'s':[]
                    ,y+'s_fixed':[]
                    ,'correct_patches':[]
                    ,'incorrect_patches':[]
                    ,'sufficient_median_0.8':[]
                    ,'sufficient_median_0.9':[]
                    ,'sufficient_median_1.0':[]
                    ,'sufficient_max_0.8':[]
                    ,'sufficient_max_0.9':[]
                    ,'sufficient_max_1.0':[]
                    ,'ignored_unknown_patches':[]})
papers=df.paper.unique()
xs = df[x].unique()
idx = 0
for p in papers:
  for bt in xs:
    dfr = df.loc[(df[x]==bt) & (df.paper==p)]
    if len(dfr)==0: 
      continue
    result = {0.8:[],0.9:[],1.0:[]}
    cc = 0
    oc = 0
    uc = 0
    bugs_fixed = 0
    N = sample # starting sample drawn
    for row in dfr.itertuples():
      M = row.correct + row.incorrect
      n = row.correct
      if n>0: bugs_fixed += 1
      cc += n
      oc += row.incorrect
      uc += row.unknown
      N = sample # number drawn
      k = 0 # desired items
      if n==0: # no correct patches
        result[0.8].append(0)
        result[0.9].append(0)
        result[1.0].append(0)
      elif M==n: # no incorrect patches
        result[0.8].append(1)
        result[0.9].append(1)
        result[1.0].append(1)
      elif N>=M: # sample greater or equal to all patches
        result[0.8].append(1)
        result[0.9].append(1)
        result[1.0].append(1)
      else: 
        # find sufficient sample
        pmfn = 1-hg.pmf(k,M,n,N)
        suffices = (pmfn>=0.8)
        while (not suffices):
          N += 1
          pmfn = 1-hg.pmf(k,M,n,N)
          suffices = (pmfn>=0.8)
        result[0.8].append(N)
        suffices = (pmfn>=0.9)
        while (not suffices):
          N += 1
          pmfn = 1-hg.pmf(k,M,n,N)
          suffices = (pmfn>=0.9)
        result[0.9].append(N)
        suffices = (pmfn>=1.0)
        while (not suffices):
          N += 1
          pmfn = 1-hg.pmf(k,M,n,N)
          suffices = (pmfn>=1.0)
        result[1.0].append(N)

    rm0 = [x for x in result[0.8] if x != 0]
    med80 = 0
    if rm0!=[]: med80 = median(rm0)
    max80 = max(result[0.8]) 

    rm0 = [x for x in result[0.9] if x != 0]
    med90 = 0
    if rm0!=[]: med90 = median(rm0)
    max90 = max(result[0.9]) 

    rm0 = [x for x in result[1.0] if x != 0]
    med100 = 0
    if rm0!=[]: med100 = median(rm0)
    max100 = max(result[1.0]) 

    dfres.loc[idx]=[p,bt,len(dfr),bugs_fixed,cc,oc,med80,med90,med100,max80,max90,max100,uc]
    idx+=1
dfres=dfres.sort_values(by=['paper',x])
cols = dfres.columns
#dfres['randomprob'] = dfres['randomprob'].map(lambda x: '%2.1f' % x)
for col in cols[2:]:
  dfres = dfres.astype({col: 'int64'})
dfres = dfres.rename(columns=lambda name: name.replace('_', ' '))
dfres.to_csv(root+'result_by_'+x+'.csv')
