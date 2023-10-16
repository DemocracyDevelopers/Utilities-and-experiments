package au.org.democracydevelopers.utils.domain.cvr;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class Cvr {

  private int cvrNumber;
  private int tabulatorNum;
  private int batchId;
  private int recordId;
  private String imprintedId;
  private String precinctPortion = "Precinct 1";
  private String ballotType = "Ballot 1 - Type 1";
  private List<Integer> votes;

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("\"").append(cvrNumber).append("\"").append(",")
        .append("\"").append(tabulatorNum).append("\"").append(",")
        .append("\"").append(batchId).append("\"").append(",")
        .append("\"").append(recordId).append("\"").append(",")
        .append("\"").append(imprintedId).append("\"").append(",")
        .append("\"").append(precinctPortion).append("\"").append(",")
        .append("\"").append(ballotType).append("\"").append(",");
    for (int i = 0; i < votes.size(); i++) {
      builder
//          .append("\"")
          .append(votes.get(i));
//          .append("\"");
      if (i < votes.size() - 1) {
        builder.append(",");
      }
    }
    return builder.toString();
  }
}
