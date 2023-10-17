package au.org.democracydevelopers.ballotUtils;

import au.org.democracydevelopers.ballotUtils.InterpretationFunctionExamples.DuplicatesBeforeOvervotes;
import au.org.democracydevelopers.ballotUtils.InterpretationFunctionExamples.DuplicatesOnly;
import org.junit.jupiter.api.Test;

public class BallotInterpretationTests {

    /*
     * Using the DuplicatesBeforeOvervotes function and testing against the
     * specific examples in the Guide.
     */
    @Test
    void Example1OvervotesNoValidRankings() {
        IRVChoices b = new IRVChoices("Candidate A(1),Candidate B(1),Candidate C(1),Candidate C(2),Candidate B(3)");
        String expectedInterpretation = "";

        IRVChoices interpretation = DuplicatesBeforeOvervotes.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }

    @Test
    void Example2OvervoteWithValidRankings() {
        IRVChoices b = new IRVChoices("Candidate B(1),Candidate A(2),Candidate C(2),Candidate C(3)");
        String expectedInterpretation = "Candidate B(1)";

        IRVChoices interpretation = DuplicatesBeforeOvervotes.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }

    @Test
    void Example1SkippedRankings() {
        IRVChoices b = new IRVChoices("Candidate A(1),Candidate B(3)");
        String expectedInterpretation = "Candidate A(1)";

        IRVChoices interpretation = DuplicatesBeforeOvervotes.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }

    @Test
    void Example1DuplicateRankings() {
        IRVChoices b = new IRVChoices("Candidate A(1),Candidate A(2),Candidate B(3)");
        String expectedInterpretation = "Candidate A(1)";

        IRVChoices interpretation = DuplicatesBeforeOvervotes.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }

    @Test
    void Example1DuplicatesAndOvervotes() {
        IRVChoices b = new IRVChoices("Candidate B(1),Candidate A(2),Candidate C(2),Candidate C(3)");
        String expectedInterpretation = "Candidate B(1)";

        IRVChoices interpretation = DuplicatesBeforeOvervotes.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }

    @Test
    void Example2DuplicatesAndOvervotes() {
        IRVChoices b = new IRVChoices("Candidate B(1),Candidate A(2),Candidate B(2),Candidate C(3)");
        String expectedInterpretation ="Candidate B(1),Candidate A(2),Candidate C(3)";

        IRVChoices interpretation = DuplicatesBeforeOvervotes.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }

    /* Various tests on other functions, not relevant to CO
     */
    @Test
    void NonExample1OvervotesNoValidRankings() {
        IRVChoices b = new IRVChoices("Candidate A(1),Candidate B(1),Candidate C(1),Candidate C(2),Candidate B(3)");
        String expectedInterpretation = "Candidate A(1),Candidate B(1),Candidate C(1)";

        IRVChoices interpretation = DuplicatesOnly.InterpretValidIntent(b);

        assert interpretation.toString().equals(expectedInterpretation);

    }
}
