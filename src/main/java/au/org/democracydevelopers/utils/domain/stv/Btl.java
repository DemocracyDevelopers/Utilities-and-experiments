package au.org.democracydevelopers.utils.domain.stv;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Btl {
  @JsonProperty("n")
  public int count;
  @JsonProperty("candidates")
  public List<Integer> preferences;
}
