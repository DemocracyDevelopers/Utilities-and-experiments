package au.org.democracydevelopers.utils.domain.stv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * We ignore atl ballots and count / process only btl ones.
 */
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

  public int countBallots() {
    return btl.stream().mapToInt(Btl::getCount).sum();
  }

  /**
   * Split the ballots into n partitions. If there are c ballots in total, this puts c / n + 1 into the
   * first c mod n partitions, with c / n in the rest. If c >= n, this guarantees at least one ballot
   * in each partition.
   * @param  n the number of partitions.
   * @return a list of the partitions (of length n).
   */
  public List<ElectionData> split(int n) {
    List<ElectionData> result = new ArrayList<>();

    final int c = countBallots();
    // The target for filling the first partition is either c / n (if c is a multiple of n) or one greater (if there's a remainder).
    int target = c / n + ( c % n == 0 ? 0 : 1);
    int ballotsInPartition = 0;
    int currentPartition = 0;

    List<Btl> partition_btl = new ArrayList<>();
    for ( Btl b : btl ) {
      int setCount = b.count;

      // This set fills or overflows this partition. Take enough votes to fill the partition, and start a new partition.
      while (setCount >= target - ballotsInPartition) {
        partition_btl.add(new Btl(target - ballotsInPartition, b.preferences));
        result.add(new ElectionData(metadata, new ArrayList<>(), partition_btl, btlTypes, 0));
        setCount -= target - ballotsInPartition;

        // Start a new partition.
        partition_btl = new ArrayList<>();
        ballotsInPartition = 0;
        currentPartition++;
        target = c / n + ( c % n > currentPartition ? 1 : 0);
      }

      // (The rest of) this set doesn't fill up this partition. Add it all to the current partition.
      if (setCount < target - ballotsInPartition) {
        partition_btl.add(new Btl(setCount, b.preferences));
        ballotsInPartition += setCount;
      }
    }
    return result;
  }
}
