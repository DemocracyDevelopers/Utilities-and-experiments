package au.org.democracydevelopers.ballotUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/* A ballot is a set of (rank, candidate name) pairs. This data structure allows them to be
 * invalid, that is, with repeated candidate names or repeated preferences.
 */
public class IRVChoices {
    private List<Preference> choices;

    public int getLength() {
        return choices.size();
    }

    public IRVChoices(List<Preference> mutableChoices) {
        mutableChoices.sort(Preference::compareTo);
        choices = Collections.unmodifiableList(mutableChoices);
    }


    // Build a new ballot from the string of CandidateName(rank) strings in the colorado-rla database.
    public IRVChoices(String sanitizedChoices) {
        ArrayList<Preference> mutableChoices = new ArrayList<>();

        String[] preferences = sanitizedChoices.trim().split(",");
        if (preferences.length == 0) {
            return;
        }
        for (String preference : preferences) {
            String[] vote = preference.split("\\(");
            String rank = vote[1].split("\\)")[0];
            if (!StringUtils.isNumeric(rank)) {
                throw new RuntimeException("Couldn't parse ballot");
                //Invalid rank. Ballot is not usable. IF ballot is invalid, this safeguards us against number format exception
            }
            mutableChoices.add(new Preference(Integer.parseInt(rank), vote[0].trim()));
        }

        mutableChoices.sort(Preference::compareTo);
        choices = Collections.unmodifiableList(mutableChoices);
    }

    /* Check whether the ballot is a valid list of preferences without repeats of candidate names or ranks and
     * without skipped or invalid preferences.
     */
    public boolean IsValid() {

        Set<String> candidateNamesSet = new HashSet<>();
        // add to set returns false if the item is already present.
        boolean allNamesDistinct = choices.stream().map(c -> candidateNamesSet.add(c.getCandidateName())).reduce(true, (b1,b2) -> b1 && b2);
        if (!allNamesDistinct) {
            return false;
        }

        // Test for a perfect integer sequence of length choices.size(), by filling in the i-th element when i is read.
        boolean[] preferencesInOrder = new boolean[choices.size()];
        for(Preference p : choices) {
            // If this preference is out of bounds or already used, the pref list is invalid
            if (p.getRank() < 1 || p.getRank() > choices.size() || preferencesInOrder[p.getRank()-1]) {
                return false;
            }
            preferencesInOrder[p.getRank()-1] = true;
        }

        return true;
    }

    /* Applies Rule 26.7.1 from
     * https://www.sos.state.co.us/pubs/rule_making/CurrentRules/8CCR1505-1/Rule26.pdf
     * Returns new updated ballot.
     */
    public IRVChoices ApplyRule1() {

        assert IsSorted(choices) : "Error: Trying to apply Rule 1 to unsorted preferences.";

        List<Preference> mutableChoices = new ArrayList<Preference>(choices);
        for (int i=0 ; i < mutableChoices.size()-1 ; i++) {
            if ((mutableChoices.get(i)).compareTo(mutableChoices.get(i + 1)) == 0) {
                // We found an overvote. Skip from this item.
                mutableChoices.subList(i, mutableChoices.size()).clear();
                break;
            }
        }

        return new IRVChoices(mutableChoices);
    }


    /* Applies Rule 26.7.2 from
     * https://www.sos.state.co.us/pubs/rule_making/CurrentRules/8CCR1505-1/Rule26.pdf
     * Returns new updated ballot.
     */
    public IRVChoices ApplyRule2() {

        assert IsSorted(choices) : "Error: Trying to apply Rule 2 to unsorted preferences.";


        // If the first element is not rank 1, the ballot is effectively blank.
        if(!choices.isEmpty() && choices.get(0).getRank() != 1) {
            return new IRVChoices(new ArrayList<Preference>());
        }

        List<Preference> mutableChoices = new ArrayList<Preference>(choices);

        for (int i=0 ; i < mutableChoices.size()-1 ; i++) {
            if ((mutableChoices.get(i+1)).getRank() > mutableChoices.get(i).getRank() + 1) {
                // We found a skipped rank. Skip from the _next_ item.
                mutableChoices.subList(i+1, mutableChoices.size()).clear();
                break;
            }
        }

        return new IRVChoices(mutableChoices);
    }

    /* Applies Rule 26.7.3 from
     * https://www.sos.state.co.us/pubs/rule_making/CurrentRules/8CCR1505-1/Rule26.pdf
     * Returns new updated ballot.
     */
    public IRVChoices ApplyRule3() {

        assert IsSorted(choices) : "Error: Trying to apply Rule 3 to unsorted preferences.";

        HashSet<String> candidates = new HashSet<>();
        List<Preference> mutableChoices = new ArrayList<Preference>();

        for (Preference choice : choices) {
            if (!candidates.contains(choice.getCandidateName())) {
                // This candidate hasn't been mentioned before. Add it to both the vote and the set of mentioned candidates.
                mutableChoices.add(choice);
                candidates.add(choice.getCandidateName());
            }
        }

        return new IRVChoices(mutableChoices);
    }

    public String toString() {
        return choices.stream().map(Preference::toString).collect(Collectors.joining(","));
    }

    private boolean IsSorted(List<Preference> choices) {
        for (int i=0 ; i < choices.size()-1 ; i++) {
            if ((choices.get(i)).compareTo(choices.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }
}