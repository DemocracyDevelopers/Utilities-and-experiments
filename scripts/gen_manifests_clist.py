# Given a CVR export and county name provided as input, generate a Ballot 
# Manifest and Canonical List for the county.
import csv
import argparse
import re
import random

if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    # CVR export file for given county
    parser.add_argument('-f', dest='cvrexport')

    # County name
    parser.add_argument('-n', dest='name')

    args = parser.parse_args()

    with open(args.cvrexport, "r") as data:
        reader = csv.reader(data)

        tab2batches = {}
        cid2candidates = {}
        cid2name = {}

        # Read contests from second CVR export line, extract
        # contest names
        next(reader)
        contest_row = [t for t in next(reader) if t != '']
        contests = set(contest_row)
        for c in contests:
            cid2candidates[c] = []

            patternIRV = "(Number of positions=1, Number of ranks=[0-9]+)"
            patternPL = "(Vote For=[0-9]+)"
           
            res = re.search(patternIRV, c)
            if res == None:
                res = re.search(patternPL, c)

            s,_ = res.span()
            cid2name[c] = c[:s-1].strip()

        cand_row = [t for t in next(reader) if t != '']
       
        for cid, c in zip(contest_row, cand_row):
            cid2candidates[cid].append(c) 
            
        # Create canonical lists file (note this will only capture contests
        # from a single CVR export file).
        with open(args.name + "_canonical_list.csv", "w") as clfile:
            print("CountyName,ContestName,ContestChoices", file=clfile)

            for c, candidates in cid2candidates.items():
                print("{},{},\"{}\"".format(args.name, cid2name[c], \
                    ",".join(candidates)), file=clfile)

        # Read ballots in CVR export, and keep track of batch numbers assigned
        # to tabulators, and record numbers assigned to tabulator-batches.
        next(reader)
        for row in reader:
            tab = row[1]
            batch = row[2]
            record = row[3]

            if tab in tab2batches:
                batch2records = tab2batches[tab]
                if batch in batch2records:
                    batch2records[batch].append(record)
                else:
                    batch2records[batch] = [record]
            else:
                tab2batches[tab] = {batch : [record]}

        # Create manifest file for county
        with open(args.name + "_manifest.csv", "w") as mffile:
            bincntr = 1
            print("CountyID,ScannerID,BatchID,NumBallots,StorageLocation",\
                file=mffile)

            for tab in tab2batches:
                for batch,records in tab2batches[tab].items():
                    print("{},{},{},{},Bin {}".format(args.name, tab, batch, \
                        len(records), bincntr), file=mffile)

                    if random.randint(0,1):
                        bincntr += 1
            
        
        
