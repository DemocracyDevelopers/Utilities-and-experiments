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
    builder.append(cvrNumber).append(",")
        .append(tabulatorNum).append(",")
        .append(batchId).append(",")
        .append(recordId).append(",")
        .append(imprintedId).append(",")
        .append(precinctPortion).append(",")
        .append(ballotType).append(",");
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
