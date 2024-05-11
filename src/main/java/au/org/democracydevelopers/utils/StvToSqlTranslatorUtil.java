package au.org.democracydevelopers.utils;

import static au.org.democracydevelopers.utils.StvReadingFunctionUtils.escapeChars;
import static au.org.democracydevelopers.utils.StvReadingFunctionUtils.findFiles;
import static au.org.democracydevelopers.utils.StvReadingFunctionUtils.getSanitisedVotesCount;
import static java.lang.System.exit;

import au.org.democracydevelopers.utils.domain.stv.Candidate;
import au.org.democracydevelopers.utils.domain.stv.ElectionData;
import au.org.democracydevelopers.utils.domain.stv.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;

/**
 * This utility class will take STV files as a command line input and translate them into a series of SQL
 * INSERT commands to produce the right database for colorado-rla and raire-service tests.
 * One example vote should look like:
 * INSERT INTO public.cast_vote_record (id, audit_board_index, comment, cvr_id, ballot_type, batch_id, county_id,
 *     cvr_number, imprinted_id, record_id, record_type, scanner_id, sequence_number, timestamp, version,
 *     rand, revision, round_number, uri)
 *     VALUES (3829000, null, null, null, 'Ballot 1 - Type 1', '1', 1, 1, '1-1-1', 1, 'UPLOADED', 1,
 *     0, null, 0, null, null, null, 'cvr:1:1-1-1');
 * INSERT INTO public.cvr_contest_info (cvr_id, county_id, choices, comment, consensus, contest_id, index, version)
 *     VALUES (3829000, 1, '["MCCARTHY Steve","JOHNSON Jeff","WILLIAMS Keith"]', null, null, 3828998, 0, null);
 * In colorado-rla, each CVR can have multiple contests, and there should be one cvr_contest_info for each.
 * Currently, this utility assumes only one contest per CVR, so the 'index' for every cvr_contest_info is zero.
 * There are two modes. In either case, the first argument is a comment.
 * If both an input and output file are specified on the command line, a single output file is created (for a single contest)
 * If one input is specified on the command line, it is taken to be a directory. We iterate over all
 * the .json files in the directory, translating them into SQL, with an incrementing county-and-contest ID.
 * This ensures that all the sql files produced can be read into a database without ID clashes, assuming that
 * the number of votes per contest does not exceed MAX_VOTES_PER_ELECTION.
 * Note there is NO EFFORT AT PROPER SQL ESCAPING so please don't use this for anything other than
 * generating test data from trustworthy sources.
 */
public class StvToSqlTranslatorUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final int MAX_RECORD_PER_BATCH = 78;

  // Used to pad the right number of zeros after the contest value in ID. If this assumption is
  // wrong, IDs may not be unique.
  // 7 digits are sufficient for vote IDs assuming there are no more than a million per contest.
  // If you change this, you have to change the string formatting (%07d) in buildSQLValues.
  private static final int MAX_VOTES_PER_ELECTION = 1000000;

  private static final String usage = "Usage: mvn clean compile exec:java -Dexec.mainClass=\"au.org.democracydevelopers.utils.StvTosqlTranslatorUtil\" -Dexec.args=\"commentForFiles sourceFile.json destinationFile \"\n"+
   "Alternative arguments for whole-directory run: -Dexec.args=\"commentForFiles sourceDirectory \"";
  public static void main(String[] args) throws Exception {
    if (args.length < 2 || args.length > 3) {
      System.err.println("Invalid number of arguments. Please use command as following");
      System.out.println(usage);
      exit(1);
    }

    String comment = args[0];

    // The single-file case.
    if(args.length == 3) {
      String sourceFilePath = args[1];
      String destinationFilePath = args[2];
      translateContest(1, comment, new File(sourceFilePath), destinationFilePath);
      System.out.println("translated CVR File is generated at: " + destinationFilePath);
    }

    // The multi-file case. We want to iterate through all the .json in the directory.
    // Also need to make a metadata file.
    if (args.length == 2) {
      String dataPath = args[1];
      translateAllContests(dataPath, comment);
      System.out.println("translated CVR Files and metadata are generated at: " + dataPath);
    }
  }

  public static void translateAllContests(String dataPath, String comment) {
    ElectionData electionData;

    Iterator<File> sourceFiles = findFiles(Paths.get(dataPath), ".json");
    try {

      FileWriter fw = new FileWriter(dataPath+"/Metadata.java", false);
      BufferedWriter bw = new BufferedWriter(fw);
      PrintWriter metadataOut = new PrintWriter(bw);

      int contestID = 1;
      while (sourceFiles.hasNext()) {
        File sourceFile = sourceFiles.next();
        electionData = translateContest(contestID, comment, sourceFile,
            FilenameUtils.removeExtension(sourceFile.toString()) + ".sql");
        writeMetadataRow(metadataOut, contestID, electionData);
        contestID++;
      }
      metadataOut.close();
    } catch (Exception ex) {
      System.out.println("Error with file I/O");
      throw new RuntimeException(ex);
    }
  }


  public static ElectionData translateContest(int countyAndContestID, String comment, File sourceFilePath,
      String destinationFilePath) throws Exception {
    ElectionData electionData = objectMapper.readValue(
        sourceFilePath,
        ElectionData.class);
    System.out.println("Read electionData");

    if(electionData.getAtl().size() + electionData.getBtl().size() >= MAX_VOTES_PER_ELECTION) {
      System.out.println("Error - too many votes for unique vote IDs");
      throw new RuntimeException("Too many votes");
    }

    Map<List<Integer>, Integer> sanitisedMap = getSanitisedVotesCount(electionData);

    System.out.println("Building SQL");
    try (FileWriter fw = new FileWriter(destinationFilePath, false);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw)) {
      writeComment(out, comment);
      writeSQLCountyAndContestInfo(out, countyAndContestID, electionData.getMetadata());
      writeSQLValues(out, countyAndContestID, electionData.getMetadata().getCandidates(), sanitisedMap);
    }
    System.out.println("Successfully Finished building SQL");
    return electionData;
  }

  private static void writeSQLValues(PrintWriter out, int countyandContestID, List<Candidate> candidates, Map<List<Integer>, Integer> buildVoteMap) {
    System.out.println("Building SQL values");
    int count = 1; // Total count, including possible multiple instances of same choices.
    int batchId = 1;
    int startRecordId = 0;
    int recordId;
    for (Entry<List<Integer>, Integer> entry : buildVoteMap.entrySet()) {

      // Translate the choices (integers) into a list of strings
      List<String> choices = entry.getKey().stream().map(c -> candidates.get(c).getName()).toList();

      for (int i = 0; i < entry.getValue(); i++) {
        if (startRecordId < MAX_RECORD_PER_BATCH) {
          recordId = ++startRecordId;
        } else {
          recordId = startRecordId = 1;
          batchId++;
        }
        // Unique ID from unique contestID and current count, prepended with zeros as needed for fixed width.
        String cvrID = countyandContestID+String.format("%07d",count);
        String imprintedID = "1-"+batchId+'-'+recordId;      // Imprinted ID: scanner_id - batch_id - record_id. ScannerID can always be 1.


        // Note there is NO EFFORT AT PROPER SQL ESCAPING - this is just a scratch file for test data
        // generation from trustworthy sources.
        String castVoteRecordValue =
            "INSERT INTO public.cast_vote_record (id, audit_board_index, comment, cvr_id, ballot_type, "+
            "batch_id, county_id, cvr_number, imprinted_id, record_id, record_type, scanner_id, "+
            "sequence_number, timestamp, version, rand, revision, round_number, uri) VALUES ("+
            cvrID +','+
            "null, null, null,"+
            "'Type 1',"+ // Ballot type, ignored for now.
            "'" +batchId+ "'," + // BatchId seems to be stored as a string.
            countyandContestID+','+ // County ID. We'll use the same number for both county and contest ID for now.
            count+','+  // CVR number - can match 'count' because it's allowed to repeat between counties.
            "'"+imprintedID+"',"+
            recordId+','+
            "'UPLOADED',"+ // record type , might as well always be "uploaded".
            "1,"+ // Scanner ID. Always 1.
            count+','+ // Sequence number. No need to be unique between counties.
            "null, 0, null, null, null,"+ // More data we don't care about: timestamp, version, rand, revision, round number.
            "'cvr:1:"+imprintedID+"');"; // Not sure what this uri is, but it seems to be basically the imprinted ID and some extra text.
        out.println(castVoteRecordValue);

        // Note there is NO EFFORT AT PROPER SQL ESCAPING - this is just a scratch file for test data
        // generation from trustworthy sources.
        String cvrContestInfoValue =
            "INSERT INTO public.cvr_contest_info (cvr_id, county_id, choices, contest_id, index) VALUES ("+
            // Note the record generated by colorado-rla has other values, which we ignore for these tests:
            // "INSERT INTO public.cvr_contest_info (cvr_id, county_id, choices, comment, consensus, contest_id, "+
            // "index, version) VALUES ("+
            cvrID +','+
            countyandContestID+','+ // county ID.
            "'[\""+String.join("\",\"",choices.stream().map(StvReadingFunctionUtils::escapeChars).toList())+"\"]',"+ // The votes as a string.
            countyandContestID+','+ // contest ID.
            "0);"; // Index zero, because all our cvrs have only one contest.
        out.println(cvrContestInfoValue);

        count++;
      }
    }
  }

  // Note there is NO EFFORT AT PROPER SQL ESCAPING - this is just a scratch file for test data
  // generation from trustworthy sources.
  private static void writeSQLCountyAndContestInfo(PrintWriter out, int i,
      Metadata electionMetadata) {
    out.println("-- Test data for "+electionMetadata.getName().getElectorate());
    out.println();
    out.println(
        "INSERT INTO county (id, name) VALUES ("+i+",'"+escapeChars(electionMetadata.getName().getElectorate())+" County');"
    );
    out.println(
      "INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES ("+
      i+','+ // CountyID. We have the same county id and contest id, though that is not required by colorado-rla
      i+','+ // ContestID.
      "0,"+ // Version.
      "'IRV',"+ // vote type. Always IRV for these test cases.
      "'"+escapeChars(electionMetadata.getName().getElectorate())+"',"+ // Contest name - for these tests, always the file name.
      i+','+ // Sequence number. For these tests, same as contest and county IDs, though this is not true in general.
      electionMetadata.getCandidates().size()+','+ // Votes allowed - one preference for each candidate.
      "1);" // Winners allowed - always 1.
    );
    out.println();
  }

  private static void writeComment(PrintWriter out, String comment) {
    out.println("-- "+comment);
    out.println("--");
  }

  private static void writeMetadataRow(PrintWriter out, int contestID, ElectionData electionData) {

    out.println("// Contest "+electionData.getMetadata().getName().getElectorate());
    out.println("private static final String nameContest_"+contestID+" = \""+electionData.getMetadata().getName().getElectorate()+"\";");
    out.println("private static final List<String> choicesContest_"+contestID+" = List.of(\""+
        electionData.getMetadata().getCandidates().stream().map(Candidate::getName).collect(Collectors.joining("\",\""))+"\");");
    int ballotCount  = electionData.countBallots();
    out.println("private static final int ballotCountContest_"+contestID+" = "+ballotCount+";");
    out.println("private static final double difficultyContest_"+contestID+" = 0; // TODO - get correct value.");
  }
}
