package au.org.democracydevelopers.utils.domain.stv;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Metadata {
  private Name name;
  private List<Candidate> candidates;
  private int[] results;
  private int vacancies;
  private int enrolment;
}
