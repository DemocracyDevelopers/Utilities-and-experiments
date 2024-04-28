# Utilities-and-experiments
Useful utils and random experiments peripherally related to colorado-rla but not intended to be part of the final code.

## Producing Colorado-rla CSV data
To Convert an STV file as input and translate to CVR format expected by colorado-rla please use the following command

`mvn compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="sourceFilePath destinationFilePath"`

example 1: resource path from project root directory

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="src/main/resources/test-data/Bellingen_Mayoral.json src/main/resources/test-data/Bellingen_Mayoral.csv"`

example 2: fully qualified path from system root

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="/Users/sandeepbajwa/Documents/democracy/Utilities-and-experiments/src/main/resources/test-data/ballina.json /Users/sandeepbajwa/Documents/democracy/Utilities-and-experiments/src/main/resources/test-data/ballina.csv"
`

This will also make  
 - a ballot manifest file called `destinationFilePath.manifest.csv` describing the cvr file.
 - a file called `destinationFilePath-raire-service.json` for the raire-service, in which each vote is a list of candidate names.

Optionally, you can add a third (integer) argument, which the time allowed to the raire-service to compute the assertions. 

In each case, if you have already compiled and you just want to run it again on a new file, you can omit 'compile' and 'clean compile.'

### Scripts for multiple runs
The scripts folder contains two simple scripts designed to produce a suite of test data for colorado-rla

- `computeallCSVHash.sh` runs the StvToCVRAndRAIREService translator on all the .json in the directory
- `computeallCSVHash.sh` computes the SHA256 hash of every .csv file in the directory. This is useful for colorado-rla uploads.

## Producing sql files for loading automatically into the corla database
To Convert an STV file (something.json) as input and translate to SQL format expected by corla you can use the following command
for a single file (stating the source and destination file names)

`mvn compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToSqlTranslatorUtil" -Dexec.args="sourceFilePath destinationFilePath"`

example 1: resource path from project root directory

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToSqlTranslatorUtil" -Dexec.args=" 'Test comment' src/main/resources/test-data/Bellingen_Mayoral.json src/main/resources/test-data/Bellingen_Mayoral.sql"`

or the following to list an entire directory.
`mvn compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToSqlTranslatorUtil" -Dexec.args="comment sourceFileDirectory"`

example 1: resource path from project root directory

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToSqlTranslatorUtil" -Dexec.args=" 'Test comment' src/main/resources/test-data/"`

Either way, the comment will be prepended to all the .sql files. This is useful for source/copyright notices.

The test data in [raire-service](https://github.com/DemocracyDevelopers/raire-service)'s NSW2021Data
was generated using the 2-argument version, with the NSW CC copyright message and the complete set of NSW 2021 .stv (.json) data.




