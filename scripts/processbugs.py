import sys
import pandas as pd
from scipy.stats import hypergeom as hg
from statistics import median

root='results/'

x = 'Bug'
sample = 1 # starting sample

df = pd.read_csv(root+'preprocessed_by_bug.csv')

cols = df.columns
for col in cols[2:]:
  df = df.astype({col: 'int64'})

dfres =pd.DataFrame({'paper':[], x:[],'correct_patches':[],'incorrect_patches':[],'prob80':[],'prob90':[],'prob100':[],'ignored_unknown_patches':[]})
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
    N = int(sample) # starting sample drawn
    for row in dfr.itertuples(): # only one row
      cc += row.correct
      oc += row.incorrect
      uc += row.unknown
    k = 0 # desired items
    if cc==0: # no correct patches
      result[0.8] = 0
      result[0.9] = 0
      result[1.0] = 0
    elif oc==0: # no incorrect patches
      result[0.8] = 1
      result[0.9] = 1
      result[1.0] = 1
    elif N>=(cc+oc): # sample greater or equal to all patches
      result[0.8] = 1
      result[0.9] = 1
      result[1.0] = 1
    else: 
      # find sufficient sample
      pmfn = 1-hg.pmf(k,cc+oc,cc,N)
      suffices = (pmfn>=0.8)
      while (not suffices):
        N += 1
        pmfn = 1-hg.pmf(k,cc+oc,cc,N)
        suffices = (pmfn>=0.8)
      result[0.8] = N
      suffices = (pmfn>=0.9)
      while (not suffices):
        N += 1
        pmfn = 1-hg.pmf(k,cc+oc,cc,N)
        suffices = (pmfn>=0.9)
      result[0.9] = N
      suffices = (pmfn>=1.0)
      while (not suffices):
        N += 1
        pmfn = 1-hg.pmf(k,cc+oc,cc,N)
        suffices = (pmfn>=1.0)
      result[1.0] = N

    dfres.loc[idx]=[p,bt,cc,oc,result[0.8],result[0.9],result[1.0],uc]
    idx+=1
dfres=dfres.sort_values(by=['prob100','paper',x])
cols = dfres.columns
#dfres['randomprob'] = dfres['randomprob'].map(lambda x: '%2.1f' % x)
for col in cols[2:]:
  dfres = dfres.astype({col: 'int64'})
dfres = dfres.rename(columns=lambda name: name.replace('_', ' '))
dfres.to_csv(root+'result_by_'+x+'_only.csv')
