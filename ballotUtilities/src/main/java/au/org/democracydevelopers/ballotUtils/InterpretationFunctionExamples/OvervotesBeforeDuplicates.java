package au.org.democracydevelopers.ballotUtils.InterpretationFunctionExamples;

import au.org.democracydevelopers.ballotUtils.BallotInterpretationFunction;
import au.org.democracydevelopers.ballotUtils.IRVChoices;

public class OvervotesBeforeDuplicates extends BallotInterpretationFunction {

    public static IRVChoices InterpretValidIntent(final IRVChoices b) {
        IRVChoices i1 = b.ApplyRule1();
        IRVChoices i3 = i1.ApplyRule3();
        return i3.ApplyRule2();
    }
}
