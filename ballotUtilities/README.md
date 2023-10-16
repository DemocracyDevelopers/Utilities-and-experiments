# Utilities for writing and testing IRV ballot interpretation functions.

These utilities allow you to experiment on IRV ballot interpretations.
The idea is that you can implement your own BallotInterpretationFunction,
perhaps inspired by the examples in InterpretationFunctionExamples. You
can then run a variety of tests on them to see if they are always valid.
The most useful is testValidityExhaustively, which will generate all possible
ballots of size up to 5, and then verify that your function applied 
to them always produces a valid result. (Be warned that it takes about
30 seconds to run. No promises about how slow it might be if you set n=6.)

The specific examples in BallotInterpretationTests are intended for use
in [colorado-rla](https://github.com/DemocracyDevelopers/colorado-rla).