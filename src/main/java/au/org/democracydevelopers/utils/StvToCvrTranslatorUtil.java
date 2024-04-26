package au.org.democracydevelopers.utils;

import static au.org.democracydevelopers.utils.StvReadingFunctionUtils.getCvrBitTranslatedVotesMap;
import static au.org.democracydevelopers.utils.StvReadingFunctionUtils.getSanitisedVotesCount;
import static java.lang.System.exit;

import au.org.democracydevelopers.utils.domain.cvr.Cvr;
import au.org.democracydevelopers.utils.domain.raireservice.ContestRequest;
import au.org.democracydevelopers.utils.domain.stv.Candidate;
import au.org.democracydevelopers.utils.domain.stv.ElectionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * This utility class will take STV file as a command line input and translate it to the Colorado
 * RLA CSV format.
 */
public class StvToCvrTranslatorUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final int MAX_RECORD_PER_BATCH = 78;
  private static final int DEFAULT_TIME_ALLOWED = 10;
  private static final String usage = "Usage: mvn clean compile exec:java -Dexec.mainClass=\"au.org.democracydevelopers.utils.StvToCvrTranslatorUtil\" -Dexec.args=\"sourceFile.json destinationFile [Optional] timeAllowed\"";
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Invalid number of arguments. Please use command as following");
      System.out.println(usage);
      exit(1);
    }

    int time = DEFAULT_TIME_ALLOWED;
    if (args.length == 3) {
      try {
        time = Integer.parseInt(args[2]);
      } catch (Exception e) {
        System.err.println("Invalid time allowed. Please use command as following");
        System.out.println(usage);
      }
    }

    String sourceFilePath = args[0];
    String destinationFilePath = args[1];
    translate(sourceFilePath, destinationFilePath, time);
    System.out.println("translated CVR File is generated at: " + destinationFilePath);
  }

  public static void translate(String sourceFilePath, String destinationFilePath, int timeAllowed) throws Exception {
    ElectionData electionData = objectMapper.readValue(
        new File(sourceFilePath),
        ElectionData.class);
    System.out.println("Read electionData");
    List<Cvr> cvrs = translateToCvr(electionData);
    System.out.println("Building CSV");
    buildCsv(cvrs, electionData, destinationFilePath+".csv");
    System.out.println("Successfully Finished building Csv");
    buildManifest(cvrs.size(), "TestCounty", destinationFilePath+"-manifest.csv");
    System.out.println("Successfully Finished building manifest");

    System.out.println("Building .json for RAIRE service");
    writeJsonForRaireService(timeAllowed, cvrs, electionData, destinationFilePath+"-raire-service.json");
    System.out.println("Successfully Finished building json for RAIRE service.");
  }


  private static List<Cvr> translateToCvr(ElectionData electionData) {
    List<Candidate> candidates = electionData.getMetadata().getCandidates();
    int numberOfCandidates = candidates.size();
    Map<List<Integer>, Integer> sanitisedMap = getSanitisedVotesCount(electionData);
    Map<List<Integer>, Integer> buildVoteMap = getCvrBitTranslatedVotesMap(numberOfCandidates, sanitisedMap);
    return buildCvrs(buildVoteMap);
  }

  private static List<Cvr> buildCvrs(Map<List<Integer>, Integer> buildVoteMap) {
    System.out.println("Building Cvrs");
    List<Cvr> cvrs = new ArrayList<>();
    int count = 1;
    int batchId = 1;
    int startRecordId = 0;
    int recordId;
    for (Entry<List<Integer>, Integer> entry : buildVoteMap.entrySet()) {
      for (int i = 0; i < entry.getValue(); i++) {
        if (startRecordId < MAX_RECORD_PER_BATCH) {
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

  // Make a test manifest file that assumes only one scanner/batch and uses the name "test county", which
  // doesn't seem to be read.
  private static void buildManifest(int ballotCount, String countyName, String destinationFilePath) throws Exception {
    try (FileWriter fw = new FileWriter(destinationFilePath, false);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter out = new PrintWriter(bw)) {

      String columnHeadingRow = "CountyID,ScannerID,BatchID,NumBallots,StorageLocation";
      out.println(columnHeadingRow);
      int batchID;
      for ( batchID = 1 ; batchID <= ballotCount / MAX_RECORD_PER_BATCH ; batchID++) {
        String dataRow = countyName + ",1," + batchID + "," + MAX_RECORD_PER_BATCH + ",Bin 1";
        out.println(dataRow);
      }
      int remainder = ballotCount % MAX_RECORD_PER_BATCH;
      if (remainder != 0) {
        String dataRow = countyName + ",1," + batchID + "," + remainder + ",Bin 1";
        out.println(dataRow);
      }
    }
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
      out.println(buildCountyHeader(electionData, candidates.size()));
      out.println(buildCandidateHeader(candidates));
      String columnHeadingRow = "CvrNumber,TabulatorNum,BatchId,RecordId,ImprintedId,PrecinctPortion,BallotType"
              +StringUtils.repeat(",",candidates.size() * candidates.size());
      out.println(columnHeadingRow);
      for (Cvr cvr : cvrs) {
        out.println(cvr);
      }
    }
  }

  private static void writeJsonForRaireService(int time, List<Cvr> cvrs, ElectionData electionData, String destinationFilePath) throws Exception {
    List<String> candidates = electionData.getMetadata().getCandidates().stream().map(Candidate::getName).collect(Collectors.toList());
    List<List<String>> votesWithNames = new ArrayList<>();

    // A map from candidate ID list to the number of repeats
    Map<List<Integer>, Integer> sanitisedMap = getSanitisedVotesCount(electionData);

    // Turn it into a list of candidates, repeat it as many times as required
    sanitisedMap.forEach( (vote, n) -> {
        List<String> voteStrings = vote.stream().map(candidates::get).toList();
        votesWithNames.addAll(Collections.nCopies(n, voteStrings));
    });

    // Build the ContestRequest object
    String contestName = electionData.getMetadata().getName().getName();
    int totalAuditableBallots = cvrs.size();
    ContestRequest cr = new ContestRequest(contestName, totalAuditableBallots, time, candidates, votesWithNames);

    // Serialize it and write it to a file
    try (FileWriter fw = new FileWriter(destinationFilePath, false);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter out = new PrintWriter(bw)) {

         out.print(objectMapper.writeValueAsString(cr));
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

  private static String buildCountyHeader(ElectionData electionData, int numberOfCandidates) {
    StringBuilder countyHeaderRow = new StringBuilder(StringUtils.repeat(",", 7));
    for (int i = 0; i < numberOfCandidates * numberOfCandidates; i++) {
      countyHeaderRow
          .append("\"")
          .append(electionData.getMetadata().getName().getElectorate())
          .append(" (Number of positions=1, Number of ranks=")
          .append(numberOfCandidates).append(")\"");
      if (i < numberOfCandidates * numberOfCandidates - 1) {
        countyHeaderRow.append(",");
      }
    }
    return countyHeaderRow.toString();
  }
}
