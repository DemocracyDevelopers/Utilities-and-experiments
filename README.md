# Utilities-and-experiments
Useful utils and random experiments peripherally related to colorado-rla but not intended to be part of the final code.

To Convert an STV file as input and translate to CVR format expected by colorado-rla please use the following command

`mvn compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="sourceFilePath destinationFilePath"`

example 1: resource path from current directory

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="src/main/resources/test-data/Bellingen.json src/main/resources/test-data/Bellingen.csv"`

example 2: fully qualified path from system root

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="/Users/sandeepbajwa/Documents/democracy/Utilities-and-experiments/src/main/resources/test-data/ballina.json /Users/sandeepbajwa/Documents/democracy/Utilities-and-experiments/src/main/resources/test-data/ballina.csv"
`

This will also make  
 - a ballot manifest file called `destinationFilePath.manifest.csv` describing the cvr file.
 - a file called `destinationFilePath-raire-service.json` for the raire-service, in which each vote is a list of candidate names.

Optionally, you can add a third (integer) argument, which the time allowed to the raire-service to compute the assertions. 

In each case, if you have already compiled and you just want to run it again on a new file, you can omit 'compile' and 'clean compile.'

## Scripts for multiple runs
The scripts folder contains two simple scripts designed to produce a suite of test data for colorado-rla

- `computeallCSVHash.sh` runs the StvToCVRAndRAIREService translator on all the .json in the directory
- `computeallCSVHash.sh` computes the SHA256 hash of every .csv file in the directory. This is useful for colorado-rla uploads.
