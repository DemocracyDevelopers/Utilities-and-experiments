# Utilities-and-experiments
Useful utils and random experiments peripherally related to colorado-rla but not intended to be part of the final code.

To Convert an STV file as input and translate to CVR format expected by colorado-rla please use the following command

`mvn compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="sourceFilePath destinationFilePath"`

example 1: resource path from current directory

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="src/main/resources/test-data/Bellingen.json src/main/resources/test-data/Bellingen.csv"`

example 2: fully qualified path from system root

`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="/Users/sandeepbajwa/Documents/democracy/Utilities-and-experiments/src/main/resources/test-data/ballina.json /Users/sandeepbajwa/Documents/democracy/Utilities-and-experiments/src/main/resources/test-data/ballina.csv"
`

This will also make a ballot manifest file called `destinationFilePath.manifest.csv` describing the cvr file.

In each case, if you have already compiled and you just want to run it again on a new file, you can omit 'compile' and 'clean compile.'
