package au.org.democracydevelopers.ballotUtils;

import org.junit.jupiter.api.Test;

class BallotUtilsTests {

    @Test
    void testValidBallots() {
        IRVChoices b = new IRVChoices("Alice(1)");
        assert b.IsValid();

        b = new IRVChoices("Alice(1),Bob(2)");
        assert b.IsValid();

        b = new IRVChoices("Alice(1),Chuan(3),Bob(2)");
        assert b.IsValid();

        b = new IRVChoices("Diego(4),Alice(1),Chuan(3),Bob(2)");
        assert b.IsValid();
    }

    @Test
    void testInvalidBallot() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(1)");
        assert !b.IsValid();

        b = new IRVChoices("Alice(1),Alice(2)");
        assert !b.IsValid();

        b = new IRVChoices("Alice(1),Bob(1)");
        assert !b.IsValid();

        b = new IRVChoices("Alice(1),Bob(3)");
        assert !b.IsValid();

        b = new IRVChoices("Alice(2),Bob(2)");
        assert !b.IsValid();

        b = new IRVChoices("Alice(2),Bob(3)");
        assert !b.IsValid();
    }

    @Test
    void applyRule1Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2)");
        IRVChoices i = b.ApplyRule1();
        assert i.getLength() == 2;
    }

    @Test
    void applyRule1Test2() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(1)");
        IRVChoices i = b.ApplyRule1();
        assert i.getLength() == 0;
    }

    @Test
    void applyRule1Test3() {
        IRVChoices b = new IRVChoices("Alice(2),Alice(1)");
        IRVChoices i = b.ApplyRule1();
        assert i.getLength() == 2;
    }

    @Test
    void applyRule1Test4() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2)");
        IRVChoices i = b.ApplyRule1();
        assert i.getLength() == 1;
    }

    @Test
    void applyRule1Test5() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2),Diego(3)");
        IRVChoices i = b.ApplyRule1();
        assert i.getLength() == 1;
    }

    @Test
    void applyRule1Test6() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(3),Diego(3)");
        IRVChoices i = b.ApplyRule1();
        assert i.getLength() == 2;
    }

    @Test
    void applyRule2Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 2;
    }

    @Test
    void applyRule2Test2() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(1)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 2;
    }

    @Test
    void applyRule2Test3() {
        IRVChoices b = new IRVChoices("Alice(2),Alice(1)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 2;
    }

    @Test
    void applyRule2Test4() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(3)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 3;
    }

    @Test
    void applyRule2Test5() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(3)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 1;
    }

    @Test
    void applyRule2Test6() {
        IRVChoices b = new IRVChoices("Bob(2),Chuan(3)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 0;
    }

    @Test
    void applyRule2Test7() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(4)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 2;
    }

    @Test
    void applyRule2Test8() {
        IRVChoices b = new IRVChoices("Alice(3),Bob(2),Chuan(4)");
        IRVChoices i2 = b.ApplyRule2();
        assert i2.getLength() == 0;
    }

    @Test
    void applyRule1AndRule2Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2),Diego(4)");
        IRVChoices i = b.ApplyRule1();
        IRVChoices i2 = i.ApplyRule2();
        assert i2.getLength() == 1;
    }

    @Test
    void applyRule1AndRule2Test2() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2),Diego(4)");
        IRVChoices i2 = b.ApplyRule2();
        IRVChoices i1 = i2.ApplyRule1();
        assert i1.getLength() == 1;
    }

    @Test
    void applyRule3Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 2;
    }

    @Test
    void applyRule3Test2() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(1)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 1;
    }

    @Test
    void applyRule3Test3() {
        IRVChoices b = new IRVChoices("Alice(2),Alice(1)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 1;
    }

    @Test
    void applyRule3Test4() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(3)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 3;
    }

    @Test
    void applyRule3Test5() {
        IRVChoices b = new IRVChoices("Alice(1),Bob(3)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 2;
    }

    @Test
    void applyRule3Test6() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 2;
    }

    @Test
    void applyRule3Test7() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2),Chuan(4),Bob(3)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 3;
    }

    @Test
    void applyRule3Test8() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Alice(4)");
        IRVChoices i3 = b.ApplyRule3();
        assert i3.getLength() == 1;
    }
    @Test
    void applyRule3AndRule2Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2)");
        IRVChoices i3 = b.ApplyRule3();
        IRVChoices i2 = i3.ApplyRule2();
        assert i2.getLength() == 2;
        assert i2.IsValid();
    }

    @Test
    void applyRule3AndRule2Test3() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(3)");
        IRVChoices i3 = b.ApplyRule3();
        IRVChoices i2 = i3.ApplyRule2();
        assert i2.getLength() == 1;
        assert i2.IsValid();
    }

    @Test
    void applyRule2AndRule3Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2)");
        IRVChoices i2 = b.ApplyRule2();
        IRVChoices i3 = i2.ApplyRule3();
        assert i3.getLength() == 2;
        assert i3.IsValid();
    }

    @Test
    void applyRule2AndRule3Test3() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(3)");
        IRVChoices i2 = b.ApplyRule2();
        IRVChoices i3 = i2.ApplyRule3();
        assert i3.getLength() == 2;
        assert !i3.IsValid();
    }
    @Test
    void applyRule1AndRule3Test1() {
        IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2)");
        IRVChoices i1 = b.ApplyRule1();
        IRVChoices i3 = i1.ApplyRule3();
        assert i3.getLength() == 1;
        assert i3.IsValid();
    }

     @Test
     void applyRule3AndRule1Test1() {
         IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2)");
         IRVChoices i3 = b.ApplyRule3();
         IRVChoices i1 = i3.ApplyRule1();
         assert i1.getLength() == 2;
         assert i1.IsValid();
    }
}

