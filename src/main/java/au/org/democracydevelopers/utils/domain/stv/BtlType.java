package au.org.democracydevelopers.utils.domain.stv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BtlType {
  @JsonProperty("vote_type")
  private String voteType;
  @JsonProperty("first_index_inclusive")
  private int firstIndexInclusive;
  @JsonProperty("last_index_exclusive")
  private int lastIndexExclusive;
}
