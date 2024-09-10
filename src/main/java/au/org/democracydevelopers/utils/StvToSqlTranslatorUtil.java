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
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;

/**
 * This utility class will take STV files as a command line input and translate them into a series of SQL
 * INSERT commands to produce the right database for colorado-rla and raire-service tests.
 * You can get the .stv files from vote.andrewconway.org. Change the file suffix from .stv to .json.
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
 * There are two modes. In all cases, the first argument is a comment.
 * * 3 arguments: If both an input and output file are specified on the command line, a single output file is created
 *                (for a single contest)
 * * 2 arguments: If one input is specified on the command line, it is taken to be a directory. We iterate over all
 *                the .json files in the directory, translating them into SQL, with an incrementing county-and-contest ID.
 *                This ensures that all the sql files produced can be read into a database without ID clashes, assuming
 *                that the number of votes per contest does not exceed MAX_VOTES_PER_ELECTION.
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
   "Alternative arguments for whole-directory run: -Dexec.args=\"commentForFiles sourceDirectory.\"";

  private final static String CAST_VOTE_RECORD_INSERT =
      "INSERT INTO cast_vote_record (id, audit_board_index, comment, cvr_id, ballot_type, "+
        "batch_id, county_id, cvr_number, imprinted_id, record_id, record_type, scanner_id, "+
        "sequence_number, timestamp, version, rand, revision, round_number, uri) VALUES";

  private final static String CVR_CONTEST_INFO_INSERT =
      "INSERT INTO cvr_contest_info (cvr_id, county_id, choices, contest_id, index) VALUES";

  private final static boolean DO_BULK = true;

  public static void main(String[] args) throws Exception {
    // TODO: make this a commandline parameter.
    boolean doBulk = DO_BULK;

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
      translateContest(1, comment, new File(sourceFilePath), destinationFilePath, doBulk);
      System.out.println("translated CVR File is generated at: " + destinationFilePath);
    }

    // The multi-file case. We want to iterate through all the .json in the directory.
    // Also need to make a metadata file.
    if (args.length == 2) {
      String dataPath = args[1];
      translateAllContests(dataPath, comment, doBulk);
      System.out.println("translated CVR Files and metadata are generated at: " + dataPath);
    }
  }

  public static void translateAllContests(String dataPath, String comment, boolean doBulk) {
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
            FilenameUtils.removeExtension(sourceFile.toString()), doBulk);
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
      String destinationFilePath, boolean doBulk) throws Exception {
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
    // If we're not doing bulk, this file will be used to store all the data
    try (FileWriter fw = new FileWriter(destinationFilePath+".sql", false);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);

        FileWriter fw_cvr_contest_info = new FileWriter(destinationFilePath + "_cvr_contest_info.sql", false);
        BufferedWriter bw_cvr_contest_info = new BufferedWriter(fw_cvr_contest_info);
        PrintWriter out_cvr_contest_info = new PrintWriter(bw_cvr_contest_info)) {

      writeComment(out, comment);
      if(doBulk) {
        writeComment(out_cvr_contest_info, comment);
      }
      writeSQLCountyAndContestInfo(out, countyAndContestID, electionData.getMetadata());
      writeSQLValues(out, out_cvr_contest_info, countyAndContestID, electionData.getMetadata().getCandidates(), sanitisedMap, doBulk);

      out_cvr_contest_info.close();
      out.close();
    }


    System.out.println("Successfully Finished building SQL");
    return electionData;
  }

  private static void writeSQLValues(PrintWriter out, PrintWriter out_cvr_contest_info, int countyandContestID, List<Candidate> candidates, Map<List<Integer>, Integer> buildVoteMap, boolean doBulk) {
    System.out.println("Building SQL values for contest number "+countyandContestID);

    int count = 1; // Total count, including possible multiple instances of same choices.
    int batchId = 1;
    int startRecordId = 0;
    int recordId;

    // If we're doing bulk insert we need to print the insert statement at the start
    if(doBulk) {
      out.println(CAST_VOTE_RECORD_INSERT);
      out_cvr_contest_info.println(CVR_CONTEST_INFO_INSERT);
    }

    // Iterate over all the different vote types (i.e. choices)
    var iterator = buildVoteMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<List<Integer>, Integer> entry = iterator.next();

      // We need to keep track of the very last vote so we can put a semicolon rather than a comma
      // at the end of the file.
      boolean isLastChoicesType = !iterator.hasNext();

      // Translate the choices (integers) into a list of strings
      List<String> choices = entry.getKey().stream().map(c -> candidates.get(c).getName()).toList();

      // Print out a line for each vote - each line has the same choices but different IDs.
      for (int i = 0; i < entry.getValue(); i++) {
        boolean isLast = isLastChoicesType && (i == entry.getValue() - 1);  // This is the last vote if it's the last instance of the last kind.
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
            // If we're not doing bulk insert, we need to print the insert statement every time
            (doBulk ? "" : CAST_VOTE_RECORD_INSERT) +
            // Write out the data
            "(" + cvrID +','+
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
            "'cvr:1:"+imprintedID+"')" + // Not sure what this uri is, but it seems to be basically the imprinted ID and some extra text.
            ( (doBulk && !isLast) ? "," : ";"); // If we're doing bulk inserts, the separate lines should be separated by a comma,
                                                // unless it's the last vote, in which case it gets a semicolon.
                                                // If not bulk, they're separate statements and all need a semicolon.
        out.println(castVoteRecordValue);

        // Note there is NO EFFORT AT PROPER SQL ESCAPING - this is just a scratch file for test data
        // generation from trustworthy sources.
        String cvrContestInfoValue =
            // If we're not doing bulk insert, we need to print the insert statement every time
            (doBulk ? "" : CVR_CONTEST_INFO_INSERT) +
            // Write out the data
            // Note the record generated by colorado-rla has other values, which we ignore for these tests:
            // "INSERT INTO public.cvr_contest_info (cvr_id, county_id, choices, comment, consensus, contest_id, "+
            // "index, version) VALUES ("+
            "( " + cvrID +','+
            countyandContestID+','+ // county ID.
            "'[\""+String.join("\",\"",choices.stream().map(StvReadingFunctionUtils::escapeChars).toList())+"\"]',"+ // The votes as a string.
            countyandContestID+','+ // contest ID.
            "0)"; // Index zero, because all our cvrs have only one contest.

        // If we're not doing bulk insert, we just print the cvrContestInfo into the same file as the
        // Cast_vote_record value. If we are doing bulk insert, they go into separate files.
        // If we're doing bulk inserts, the separate lines should be separated by a comma,
        // unless it's the last vote, in which case it gets a semicolon.
        // If not bulk, they're separate statements and all need a semicolon.
        if(doBulk) {
          out_cvr_contest_info.println(cvrContestInfoValue + (isLast ? ";" : ",") );
        } else {
          out.println(cvrContestInfoValue + ";");
        }

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
