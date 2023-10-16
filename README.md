# Utilities-and-experiments
Useful utils and random experiments peripherally related to colorado-rla but not intended to be part of the final code.

To Convert an STV file as input and translate to CVR format expected by colorado-rla please use the following command
`mvn compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.domain.util.StvToCvrTranslatorUtil" -Dexec.args="sourceFilePath destinationFilePath"`
example:
`mvn clean compile exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="src/main/resources/test-data/Bellingen.json"`
