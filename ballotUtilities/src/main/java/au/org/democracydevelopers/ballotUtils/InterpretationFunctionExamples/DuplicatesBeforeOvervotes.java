package au.org.democracydevelopers.ballotUtils.InterpretationFunctionExamples;

import au.org.democracydevelopers.ballotUtils.BallotInterpretationFunction;
import au.org.democracydevelopers.ballotUtils.IRVChoices;

public class DuplicatesBeforeOvervotes extends BallotInterpretationFunction {

        public static IRVChoices InterpretValidIntent(final IRVChoices b) {
            IRVChoices i3 = b.ApplyRule3();
            IRVChoices i1 = i3.ApplyRule1();
            return i1.ApplyRule2();
        }
}
