package au.org.democracydevelopers.utils;

import static java.lang.System.exit;

import au.org.democracydevelopers.utils.domain.cvr.Cvr;
import au.org.democracydevelopers.utils.domain.stv.Btl;
import au.org.democracydevelopers.utils.domain.stv.Candidate;
import au.org.democracydevelopers.utils.domain.stv.ElectionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * This utility class will take STV file as a command line input and translate it to the Colorado
 * RLA CSV format.
 */
public class StvToCvrTranslatorUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Invalid number of arguments. Please use command as following");
      System.out.println(
          "mvn clean compile exec:java -Dexec.mainClass=\"au.org.democracydevelopers.utils.StvToCvrTranslatorUtil\" -Dexec.args=\"sourceFilePath destinationFilePath\"");
      exit(1);
    }
    String sourceFilePath = args[0];
    String destinationFilePath = args[1];
    translate(sourceFilePath, destinationFilePath);
    System.out.println("translated CVR File is generated at: " + destinationFilePath);
  }

  public static void translate(String sourceFilePath, String destinationFilePath) throws Exception {
    ElectionData electionData = objectMapper.readValue(
        new File(sourceFilePath),
        ElectionData.class);
    System.out.println("Read electionData");
    List<Cvr> cvrs = translateToCvr(electionData);
    System.out.println("Building CSV");
    buildCsv(cvrs, electionData, destinationFilePath);
    System.out.println("Successfully Finished building Csv");
  }

  private static List<Cvr> translateToCvr(ElectionData electionData) throws Exception {
    List<Candidate> candidates = electionData.getMetadata().getCandidates();
    int numberOfCandidates = candidates.size();
    Map<List<Integer>, Integer> sanitisedMap = getSanitisedVotesCount(electionData);
    Map<List<Integer>, Integer> buildVoteMap = getCvrBitTranslatedVotesMap(numberOfCandidates,
        sanitisedMap);
    return buildCvrs(buildVoteMap);
  }

  private static Map<List<Integer>, Integer> getCvrBitTranslatedVotesMap(int numberOfCandidates,
      Map<List<Integer>, Integer> sanitisedMap) {
    Map<List<Integer>, Integer> buildVoteMap = new LinkedHashMap<>();
    for (Entry<List<Integer>, Integer> entry : sanitisedMap.entrySet()) {
      List<Integer> preferenceBitList = new ArrayList<>(numberOfCandidates * numberOfCandidates);
      initialize(preferenceBitList, numberOfCandidates);
      for (int i = 0; i < entry.getKey().size(); i++) {
        preferenceBitList.set(entry.getKey().get(i) * numberOfCandidates + i, 1);
      }
      buildVoteMap.put(preferenceBitList, entry.getValue());
    }
    return buildVoteMap;
  }

  private static Map<List<Integer>, Integer> getSanitisedVotesCount(ElectionData electionData) {
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

  private static List<Cvr> buildCvrs(Map<List<Integer>, Integer> buildVoteMap)
      throws Exception {
    System.out.println("Building Cvrs");
    List<Cvr> cvrs = new ArrayList<>();
    int count = 1;
    int batchId = 1;
    int startRecordId = 1;
    int maxRecordId = 78;
    int recordId;
    for (Entry<List<Integer>, Integer> entry : buildVoteMap.entrySet()) {
      for (int i = 0; i < entry.getValue(); i++) {
        if (startRecordId < maxRecordId) {
          recordId = ++startRecordId;
        } else {
          recordId = startRecordId = 1;
          batchId++;
        }
        Cvr cvr = Cvr.builder()
            .tabulatorNum(1)
            .batchId(batchId)
            .recordId(recordId)
            .cvrNumber(count++)
            .precinctPortion("Precinct 1")
            .ballotType("Ballot 1 - Type 1")
            .imprintedId("1-" + batchId + "-" + recordId)
            .votes(entry.getKey())
            .build();
        cvrs.add(cvr);
      }
    }
    return cvrs;

  }

  private static void buildCsv(List<Cvr> cvrs, ElectionData electionData,
      String destinationFilePath) throws Exception {
    List<String> candidates = electionData.getMetadata().getCandidates().stream()
        .map(Candidate::getName)
        .collect(Collectors.toList());
    try (FileWriter fw = new FileWriter(destinationFilePath, false);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw)) {
      String headerRow = new StringBuilder()
          .append(electionData.getMetadata().getName().getYear())
          .append(" ")
          .append(electionData.getMetadata().getName().getElectorate())
          .append(" ")
          .append(electionData.getMetadata().getName().getName())
          .append(",")
          .append("5.10.11.24").toString();
      headerRow = headerRow + StringUtils.repeat(",", 5 + candidates.size() * candidates.size());
      out.println(headerRow);
      out.println(buildCountyHeader(candidates.size()));
      out.println(buildCandidateHeader(candidates));
      for (Cvr cvr : cvrs) {
        out.println(cvr);
      }
    }
  }

  private static String buildCandidateHeader(List<String> candidates) {
    int numberOfCandidates = candidates.size();
    StringBuilder candidateHeadersRow = new StringBuilder(StringUtils.repeat(",", 7));
    int countOfCandidates = 0;
    for (int i = 0; i < numberOfCandidates; i++) {
      for (int j = 0; j < numberOfCandidates; j++) {
        candidateHeadersRow
            .append(candidates.get(j))
            .append("(")
            .append(i + 1)
            .append(")");
        if (countOfCandidates < numberOfCandidates * numberOfCandidates - 1) {
          candidateHeadersRow.append(",");
        }
        countOfCandidates++;
      }
    }
    return candidateHeadersRow.toString();
  }

  private static String buildCountyHeader(int numberOfCandidates) {
    StringBuilder countyHeaderRow = new StringBuilder(StringUtils.repeat(",", 7));
    for (int i = 0; i < numberOfCandidates * numberOfCandidates; i++) {
      countyHeaderRow.append("\"IRV for Test County (Number of positions=1, Number of ranks=")
          .append(numberOfCandidates).append(")\"");
      if (i < numberOfCandidates * numberOfCandidates - 1) {
        countyHeaderRow.append(",");
      }
    }
    return countyHeaderRow.toString();
  }

  private static void initialize(List<Integer> preferenceBitList, int numberOfCandidates) {
    for (int i = 0; i < numberOfCandidates * numberOfCandidates; i++) {
      preferenceBitList.add(0);
    }
  }
}
