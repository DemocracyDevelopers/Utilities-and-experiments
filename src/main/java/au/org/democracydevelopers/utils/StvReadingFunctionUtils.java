/*
Copyright 2024 Democracy Developers

The Raire Service is designed to connect colorado-rla and its associated database to
the raire assertion generation engine (https://github.com/DemocracyDevelopers/raire-java).

This file is part of raire-service.

raire-service is free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

raire-service is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with
raire-service. If not, see <https://www.gnu.org/licenses/>.
*/

package au.org.democracydevelopers.utils;

import au.org.democracydevelopers.utils.domain.stv.Btl;
import au.org.democracydevelopers.utils.domain.stv.ElectionData;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class StvReadingFunctionUtils {

  public static Map<List<Integer>, Integer> getCvrBitTranslatedVotesMap(int numberOfCandidates,
      Map<List<Integer>, Integer> sanitisedMap) {
    Map<List<Integer>, Integer> buildVoteMap = new LinkedHashMap<>();
    for (Entry<List<Integer>, Integer> entry : sanitisedMap.entrySet()) {
      List<Integer> preferenceBitList = new ArrayList<>(numberOfCandidates * numberOfCandidates);
      initialize(preferenceBitList, numberOfCandidates);
      for (int i = 0; i < entry.getKey().size(); i++) {
        // if candidate c is at index i, this tells us to put a 1 in the c'th column of the i-th preference group
        preferenceBitList.set(i * numberOfCandidates + entry.getKey().get(i), 1);
      }
      buildVoteMap.put(preferenceBitList, entry.getValue());
    }
    return buildVoteMap;
  }

  public static Map<List<Integer>, Integer> getSanitisedVotesCount(ElectionData electionData) {
    Map<List<Integer>, Integer> sanitisedMap = new HashMap<>();
    int totalVotes = 0;
    for (Btl entry : electionData.getBtl()) {
      totalVotes += entry.getCount();
      if (sanitisedMap.containsKey(entry.getPreferences())) {
        sanitisedMap.put(entry.getPreferences(),
            sanitisedMap.get(entry.getPreferences()) + entry.getCount());
      } else {
        sanitisedMap.put(entry.getPreferences(), entry.getCount());
      }
    }
    System.out.println("Total Number of Votes: " + totalVotes);
    return sanitisedMap;
  }

  private static void initialize(List<Integer> preferenceBitList, int numberOfCandidates) {
    for (int i = 0; i < numberOfCandidates * numberOfCandidates; i++) {
      preferenceBitList.add(0);
    }
  }

  // Copied from https://www.baeldung.com/java-recursive-search-directory-extension-match
  public static Iterator<File> findFiles(Path startPath, String extension) {
    if (!extension.startsWith(".")) {
      extension = "." + extension;
    }
    return FileUtils.iterateFiles(
        startPath.toFile(),
        WildcardFileFilter.builder().setWildcards("*" + extension).get(),
        TrueFileFilter.INSTANCE);
  }

  // For escaping the single quotes in names like O'Brien, which otherwise confuse the sql reader.
  public static String escapeChars(String in) {
    // Escape once for Java, once for regexp.
      return in.replaceAll("'", "''");
  }
}
