#!/bin/bash

# Run this from the Utilities-and-experiments directory.
# Note this does not work for filenames with whitespace.

for file in $(find src/main/resources/test-data/ -type f -name "*.csv")
do
   sha256sum ${file} >  "${file}.sha256sum"
done
