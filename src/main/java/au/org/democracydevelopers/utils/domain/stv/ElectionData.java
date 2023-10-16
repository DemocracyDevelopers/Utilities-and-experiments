package au.org.democracydevelopers.utils.domain.stv;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ElectionData {
  private Metadata metadata;
  private List<Object> atl;
  private List<Btl> btl;
  @JsonProperty("btl_types")
  private List<BtlType> btlTypes;
  private int informal;
}
