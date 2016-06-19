package ca.sfu.teambeta.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Scorecard;

/**
 * Created by constantin on 27/05/16. <p> <p> USAGE: After all of the games took place
 * setIsPlaying(Pair) returns false if any of players are already playing (1) pass groups to
 * LadderManager (2) call processLadder() for all the computations to be complete.
 */

public class LadderManager {
    private Ladder ladder;
    private List<Pair> activePairs;
    private List<Pair> passivePairs;

    public LadderManager(List<Pair> dbLadder) {
        int index = 1;
        for (Pair current : dbLadder) {
            current.setPosition(index);
            index++;
        }
        ladder = new Ladder(dbLadder);
        activePairs = findPairs(ladder.getLadder(), true);
        passivePairs = findPairs(ladder.getLadder(), false);
    }

    public LadderManager(Ladder loadedLadder) {
        if (loadedLadder.getLadder().size() == 0) {
            ladder = new Ladder(new ArrayList<Pair>());
        } else {
            ladder = loadedLadder;
        }
        activePairs = new ArrayList<>();
        passivePairs = new ArrayList<>();
    }

    public int ladderSize() {
        return ladder.getLadderLength();
    }

    public List<Pair> getLadder() {
        List<Pair> list =  ladder.getLadder();
        Comparator<Pair> makeSorter = getPairPositionComparator();

        Collections.sort(list, makeSorter);
        return list;
    }

    public List<Pair> getActivePairs() {
        split();
        return activePairs;
    }

    public List<Pair> getPassivePairs() {
        split();
        return passivePairs;
    }

    public boolean addNewPair(Pair newPair) {
        boolean pairExists = ladder.getLadder().contains(newPair);
        if (!pairExists) {
            newPair.setPosition(ladder.getLadderLength());
            setIsPlaying(newPair);
            ladder.insertAtEnd(newPair);
        }
        return !pairExists;
    }

    public boolean addNewPairAtIndex(Pair newPair, int index) {
        boolean pairExists = ladder.getLadder().contains(newPair);
        if (!pairExists) {
            newPair.setPosition(ladder.getLadderLength());
            setIsPlaying(newPair);
            ladder.insertAtIndex(index, newPair);
        }
        return !pairExists;
    }

    public boolean removePairAtIndex(int index) {
        if (index >= 0 && index < ladder.getLadderLength()) {
            Pair pairToRemove = ladder.getPairAtIndex(index);
            ladder.removePair(pairToRemove);
            return true;
        }

        return false;
    }

    public Pair searchPairById(String id){
        for (Pair current : ladder.getLadder()){
            if (current.getId() == id){
                return current;
            }
        }
        return null;
    }

        return false;
    }
    public Pair searchPairById(String id){
        for (Pair current : ladder.getLadder()){
            if (current.getId() == id){
                return current;
            }
        }
        return null;
    }

    public boolean setIsPlaying(Pair pair) {
        //Set pair to playing if players are unique(returns true)
        if (ladder.getLadder().contains(pair)) {
            List<Player> team = pair.getPlayers();
            Player first = team.get(0);
            Player second = team.get(1);
            if (!searchActivePlayer(first) && !searchActivePlayer(second)) {
                pair.activate();
                getActivePairs();
                return true;
            }
        }
        return false;
    }

    public void setNotPlaying(Pair pair) {
        if (ladder.getLadder().contains(pair)) {
            pair.deActivate();
            getActivePairs();
        }
    }

    public void processLadder(List<Scorecard<Pair>> scorecards) {
        split();
        applyAbsentPenalty();
        swapBetweenGroups(scorecards);
        assignNewPositionsToActivePairs();
        combineActivePassive();
        applyLateMissPenalty();
        savePositions();
    }

    private void split() {
        List<Pair> fullLadder = ladder.getLadder();

        activePairs = findPairs(fullLadder, true);
        passivePairs = findPairs(fullLadder, false);
    }

    private void applyAbsentPenalty() {
        int previousTakenPosition = ladder.getLadderLength();
        int size = passivePairs.size();

        for (int i = size - 1; i >= 0; i--) {
            Pair pair = passivePairs.get(i);
            int position = pair.getPosition();
            int possibleShift = previousTakenPosition - position;

            switch (possibleShift) {
                case 0:
                    break;
                case 1:
                    pair.setPosition(position + 1);
                    break;
                default:
                    pair.setPosition(position + Penalty.ABSENT.getPenalty());
            }

            previousTakenPosition = pair.getPosition() - 1;
        }
    }

    private List<Pair> swapBetweenGroups(List<Scorecard<Pair>> scorecards) {
        // SWAPPING between groups and saving result in activePairs

        // Setup a list to hold the decompiled Scorecard's and
        //  one to hold the first group
        List<Pair> completedPairs = new ArrayList<Pair>();
        List<Pair> firstGroup = scorecards.get(0).getTeamRankings();
        List<Pair> previousGroup = firstGroup;

        for (int i = 1; i < scorecards.size(); i++) {
            // Swap the player's in the first and last position of subsequent groups
            List<Pair> currentGroup = scorecards.get(i).getTeamRankings();
            swapPlayers(previousGroup, currentGroup);

            completedPairs.addAll(previousGroup);
            previousGroup = currentGroup;
        }

        // The for loop omits the last group, thus add it now:
        completedPairs.addAll(previousGroup);
        this.activePairs = completedPairs;

        return completedPairs;
    }

    private void assignNewPositionsToActivePairs() {
        int size = ladder.getLadderLength();
        int[] emptyPositions = new int[activePairs.size()];
        int[] takenPositions = new int[passivePairs.size()];

        //Fill in takenPositions
        int index = 0;
        for (Pair current : passivePairs) {
            takenPositions[index] = current.getPosition();
            index++;
        }

        //Fill in emptyPositions
        int takenIndex = 0;
        int emptyIndex = 0;
        for (int position = 1; position <= size; position++) {
            if (takenPositions[takenIndex] != position) {
                emptyPositions[emptyIndex] = position;
                emptyIndex++;
            } else {
                if (takenIndex != passivePairs.size() - 1) { //Not last possible index
                    takenIndex++;
                }
            }
        }

        //Give activePairs new positions
        index = 0;
        for (Pair current : activePairs) {
            current.setPosition(emptyPositions[index]);
            index++;
        }
    }

    private void combineActivePassive() {
        List<Pair> newLadder = new ArrayList<>();
        for (Pair current : activePairs) {
            current.deActivate();
        }

        newLadder.addAll(passivePairs);
        newLadder.addAll(activePairs);
        Comparator<Pair> makeSorter = getPairPositionComparator();
        Collections.sort(newLadder, makeSorter);
        passivePairs.clear();
        activePairs.clear();
        ladder = new Ladder(newLadder);
    }

    private void applyLateMissPenalty() {
        List<Pair> pairList = ladder.getLadder();
        int size = ladder.getLadderLength();

        for (Pair current : pairList) {
            int penalty = current.getPenalty();
            if (penalty != Penalty.ZERO.getPenalty()) {
                current.setPenalty(Penalty.ZERO.getPenalty());
                int actualPosition = current.getPosition();
                int newPosition = 0;

                if (penalty == Penalty.LATE.getPenalty()) {
                    newPosition = actualPosition + penalty;
                } else if (penalty == Penalty.MISSING.getPenalty()) {
                    newPosition = current.getOldPosition() + penalty;
                }

                if (newPosition > size) {
                    newPosition = size;
                }
                movePair(actualPosition, newPosition);
            }
        }
    }

    private void savePositions() {
        List<Pair> listPairs = ladder.getLadder();
        for (Pair current : listPairs) {
            current.establishPosition();
        }
    }

    private void swapPlayers(List<Pair> firstGroup, List<Pair> secondGroup) {
        // This method swaps the last member of 'firstGroup' with the first member of 'secondGroup'

        int lastIndexOfFirstGroup = firstGroup.size() - 1;

        Pair temp = firstGroup.get(lastIndexOfFirstGroup);

        firstGroup.set(lastIndexOfFirstGroup, secondGroup.get(0));
        secondGroup.set(0, temp);
    }

    private Comparator<Pair> getPairPositionComparator() {
        return new Comparator<Pair>() {
            @Override
            public int compare(Pair p1, Pair p2) {
                int result;
                if (p1.getPosition() == p2.getPosition()) {
                    result = p1.getOldPosition() - p2.getOldPosition();
                } else {
                    result = p1.getPosition() - p2.getPosition();
                }
                return result;
            }
        };
    }

    private List<Pair> findPairs(List<Pair> fullLadder, boolean isPlaying) {
        return fullLadder.stream()
                .filter(p -> p.isPlaying() == isPlaying)
                .collect(Collectors.toList());
    }

    private void swapPair(int firstIndex, int secondIndex) {
        List<Pair> listPairs = ladder.getLadder();
        Pair first = listPairs.get(firstIndex);
        Pair second = listPairs.get(secondIndex);

        first.setPosition(secondIndex + 1);
        second.setPosition(firstIndex + 1);

        listPairs.set(firstIndex, second);
        listPairs.set(secondIndex, first);

        ladder.assignNewLadder(listPairs);
    }

    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();

        for (Pair current : ladder.getLadder()) {
            players.addAll(current.getPlayers());
        }

        return players;
    }

    private boolean searchActivePlayer(Player player) {
        split();
        for (Pair current : activePairs) {
            if (current.hasPlayer(player)) {
                return true;
            }
        }
        return false;
    }

    public void movePair(int oldPosition, int newPosition) {
        for (int i = oldPosition; i < newPosition; i++) {
            swapPair(i - 1, i);
        }
    }
}
