package au.org.democracydevelopers.ballotUtils.InterpretationFunctionExamples;

import au.org.democracydevelopers.ballotUtils.BallotInterpretationFunction;
import au.org.democracydevelopers.ballotUtils.IRVChoices;

public class DuplicatesOnly extends BallotInterpretationFunction {

    public static IRVChoices InterpretValidIntent(final IRVChoices b) {
        return b.ApplyRule3();
    }
}
