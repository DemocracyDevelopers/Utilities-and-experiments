#!/bin/bash

# Run this from the Utilities-and-experiments directory.
# Note this does not work for filenames with whitespace.

for jsonfile in $(find src/main/resources/test-data/ -type f -name "*Mayoral.json")
do
   mvn exec:java -Dexec.mainClass="au.org.democracydevelopers.utils.StvToCvrTranslatorUtil" -Dexec.args="${jsonfile} ${jsonfile%.json}.csv"
done
