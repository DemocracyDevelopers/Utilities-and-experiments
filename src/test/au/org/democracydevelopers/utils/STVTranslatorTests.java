package au.org.democracydevelopers.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

class STVTranslatorTests {

@Test
  void translateVotesToNamesTest () {
        List<String> testStrings = List.of("A", "B");

        assert testStrings.length() == 2;
  }



}