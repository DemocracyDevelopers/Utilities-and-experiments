package au.org.democracydevelopers.utils.domain.raireservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.ConstructorProperties;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContestRequest {
   private String contestName;
   private int totalAuditableBallots;
   private Integer timeProvisionForResult;
   private List<String> candidates;
   private List<List<String>> votes;

   @ConstructorProperties({"contestName", "totalAuditableBallots", "timeProvisionForResult", "candidates", "votes"})
   public ContestRequest( String contestName, int totalAuditableBallots, int timeProvisionForResult, List<String> candidates, List<List<String>> votes) {
      this.contestName = contestName;
      this.totalAuditableBallots = totalAuditableBallots;
      this.timeProvisionForResult = timeProvisionForResult;
      this.candidates = candidates;
      this.votes = votes;

   }
}
