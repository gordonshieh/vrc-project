package ca.sfu.teambeta.logic;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;

import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Scorecard;
import ca.sfu.teambeta.core.Time;
import ca.sfu.teambeta.persistence.Persistable;

@Entity(name = "session")
public class GameSession extends Persistable {
    @OneToOne(cascade = CascadeType.ALL)
    private Ladder ladder;

    @OneToOne(cascade = CascadeType.ALL)
    private Ladder reorderedLadder = null;

    @ManyToMany
    private Set<Pair> activePairs = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @OrderColumn
    private List<Scorecard> scorecards = new ArrayList<>();

    @ElementCollection
    private Map<Pair, Penalty> penalties = new HashMap<>();

    @ElementCollection
    private Map<Pair, Time> timeSlots = new HashMap<>();

    private Timestamp timestamp;

    // Default constructor for Hibernate
    public GameSession() {
    }

    public GameSession(Ladder ladder) {
        this.ladder = ladder;
        initializeActivePlayers();
        createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
        updateTimestamp();
    }

    // Constructor for testing
    public GameSession(Ladder ladder, Timestamp timestamp) {
        this.ladder = ladder;
        initializeActivePlayers();
        createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
        timestamp = timestamp;
    }

    public void updateTimestamp() {
        Calendar calender = Calendar.getInstance();
        Date now = calender.getTime();
        timestamp = new Timestamp(now.getTime());
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    // Use me ONLY for importing a CSV file!!
    public void replaceLadder(Ladder ladder) {
        this.ladder = ladder;
        initializeActivePlayers();
        updatePairsLastWeekPositions();
        createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
        updateTimestamp();
    }

    public List<Scorecard> createGroups(ScorecardGenerator generator, TimeSelection timeSelector) {
        //Generate groups
        scorecards = generator.generateScorecards(getActivePairs());
        //Set dominant time slots for each group
        timeSelector.distributePairs(scorecards, timeSlots);

        return Collections.unmodifiableList(scorecards);
    }

    public List<Pair> getAllPairs() {
        return new LinkedList<>(ladder.getPairs());
    }

    public List<Pair> getActivePairs() {
        return ladder.getPairs().stream()
                .filter(pair -> activePairs.contains(pair))
                .collect(Collectors.toList());
    }

    public void initializeActivePlayers() {
        for (Pair p : this.ladder.getPairs()) {
            if (p.isPlaying()) {
                setPairActive(p);
            }
        }
    }

    public Set<Pair> getActivePairSet() {
        return new HashSet<>(activePairs);
    }

    public List<Pair> getPassivePairs() {
        return ladder.getPairs().stream()
                .filter(pair -> !activePairs.contains(pair))
                .collect(Collectors.toList());
    }

    public List<Scorecard> getScorecards() {
        return Collections.unmodifiableList(scorecards);
    }

    public void setScorecards(List<Scorecard> scorecards) {
        this.scorecards = scorecards;
    }

    public Ladder getReorderedLadder() {
        return reorderedLadder;
    }

    public boolean setPairActive(Pair pair) {
        try {
            if (getAlreadyActivePlayer(pair) == null) {
                activePairs.add(pair);
                timeSlots.put(pair, Time.NO_SLOT);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Player getAlreadyActivePlayer(Pair pair) throws Exception {
        if (ladder.getPairs().contains(pair)) {
            List<Player> team = pair.getPlayers();
            Player first = team.get(0);
            Player second = team.get(1);
            if (searchActivePlayer(first)) {
                return first;
            } else if (searchActivePlayer(second)) {
                return second;
            } else {
                return null;
            }
        } else {
            throw new Exception("Pair is not in the ladder");
        }
    }

    public void setPairInactive(Pair pair) {
        activePairs.remove(pair);
    }

    public void setPenaltyToPair(Pair pair, Penalty penalty) {
        penalties.put(pair, penalty);
    }

    public void removePenaltyFromPair(Pair pair) { //TODO should be automatic
        penalties.remove(pair);
    }

    public boolean isActivePair(Pair pair) {
        List<Pair> activeList = getActivePairs();
        boolean active = activeList.contains(pair);

        return active;
    }

    public void reorderLadder(LadderReorderer reorderer, TimeSelection timeSelector) {
        updatePairsLastWeekPositions();
        List<Pair> reorderedList =
                reorderer.reorder(getAllPairs(), scorecards, activePairs, penalties);
        if (reorderedLadder == null) {
            reorderedLadder = new Ladder(reorderedList);
        } else {
            reorderedLadder.setNewPairs(reorderedList);
        }
        updateTimestamp();
        for (Pair p : getAllPairs()) {
            p.setPairScore(0);
        }
    }

    private void updatePairsLastWeekPositions() {
        List<Pair> pairList = this.ladder.getPairs();
        for (int i = 0; i < pairList.size(); i++) {
            Pair pair = pairList.get(i);
            pair.setLastWeekPosition(i + 1);
        }
    }

    public boolean addNewPairAtIndex(Pair newPair, int index) {
        boolean pairExists = ladder.getPairs().contains(newPair);
        if (!pairExists) {
            newPair.setLastWeekPosition(index + 1);
            ladder.insertAtIndex(index, newPair);
        }
        updateTimestamp();
        return pairExists;
    }

    public boolean addNewPairAtEnd(Pair newPair) {
        boolean pairExists = ladder.getPairs().contains(newPair);
        if (!pairExists) {
            newPair.setLastWeekPosition(ladder.getLadderLength() + 1);
            ladder.insertAtEnd(newPair);
        }
        updateTimestamp();
        return pairExists;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 57 * ladder.hashCode() * reorderedLadder.hashCode()
                * activePairs.hashCode() * scorecards.hashCode()
                * penalties.hashCode();
    }

    private boolean searchActivePlayer(Player player) {
        for (Pair current : activePairs) {
            if (current.hasPlayer(player)) {
                return true;
            }
        }
        return false;
    }

    public Scorecard getScorecardByIndex(int index) {
        for (Scorecard s : scorecards) {
            if (s.getID() == index) {
                return s;
            }
        }
        return null;
    }

    public void setTimeSlot(Pair pair, Time time) {
        timeSlots.put(pair, time);
    }

    public Map<Pair, Time> getTimeSlots() {
        return new HashMap<>(timeSlots);
    }
}
