package au.org.democracydevelopers.utils.domain.stv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Name {
  private int year;
  private String authority;
  private String name;
  private String electorate;

}
