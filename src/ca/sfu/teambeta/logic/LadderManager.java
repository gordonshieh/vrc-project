package ca.sfu.teambeta.logic;

//TODO SUGGESTIONS to distinguish ABSENT and (Accident)???
//Now ABSENT penalty has to be set from outside

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
        activePairs = findPairs(ladder.getPairs(), true);
        passivePairs = findPairs(ladder.getPairs(), false);
    }

    public int ladderSize() {
        return ladder.getLadderLength();
    }

    public List<Pair> getLadder() {
        return ladder.getPairs();
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
        boolean pairExists = ladder.getPairs().contains(newPair);
        if (!pairExists) {
            newPair.setPosition(ladder.getLadderLength());
            setIsPlaying(newPair);
            ladder.insertAtEnd(newPair);
        }
        return !pairExists;
    }

    public boolean addNewPairAtIndex(Pair newPair, int index) {
        boolean pairExists = ladder.getPairs().contains(newPair);
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

    public Pair searchPairById(int id) {
        for (Pair current : ladder.getPairs()) {
            if (current.getID() == id) {
                return current;
            }
        }
        return null;
    }

    public boolean setIsPlaying(Pair pair) {
        //Set pair to playing if players are unique(returns true)
        if (ladder.getPairs().contains(pair)) {
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
        if (ladder.getPairs().contains(pair)) {
            pair.deActivate();
            getActivePairs();
        }
    }

    public void processLadder(List<Scorecard<Pair>> scorecards) {
        split();
        applyAbsentPenalty();
        List<Pair> x = swapBetweenGroups(scorecards);

        System.out.println("-----------Swapped Groups----------------");
        for(Pair current : x){
            System.out.println(current);
        }

        assignNewPositionsToActivePairs();

        System.out.println("-----------New Pos for Active------------");
        for(Pair current : activePairs){
            System.out.println(current);
        }

        combineActivePassive();

        System.out.println("-----------Combined Ladder---------------");
        for(Pair current : ladder.getLadder()){
            System.out.println(current);
        }

        applyLateMissPenalty();

        System.out.println("-----------Final Ladder------------------");
        for(Pair current : ladder.getLadder()){
            System.out.println(current);
        }

    }

    private void split() {
        List<Pair> fullLadder = ladder.getPairs();

        activePairs = findPairs(fullLadder, true);
        passivePairs = findPairs(fullLadder, false);
    }

    private void applyAbsentPenalty() {
        int previousTakenPosition = ladder.getLadderLength();
        int size = passivePairs.size();

        //Move pairs starting from the worse pair to the best
        for (int i = size - 1; i >= 0; i--) {
            Pair pair = passivePairs.get(i);
            if (pair.getPenalty() != Penalty.ACCIDENT.getPenalty()) {
                int position = pair.getPosition();
                int possibleShift = previousTakenPosition - position;

                switch (possibleShift) {
                    case 0: //Do not move the pair
                        break;
                    case 1: //Move pair on 1 position
                        pair.setPosition(position + 1);
                        break;
                    default: //Move pair on 2 positions
                        pair.setPosition(position + Penalty.ABSENT.getPenalty());
                }
                pair.setPenalty(Penalty.ZERO.getPenalty());
                previousTakenPosition = pair.getPosition() - 1;

            } else {
                pair.setPenalty(Penalty.ZERO.getPenalty());
            }
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
        int[] takenPositions = new int[passivePairs.size()];

        //Fill in takenPositions
        int index = 0;
        for (Pair current : passivePairs) {
            takenPositions[index] = current.getPosition();
            index++;
        }

        //Fill in emptyPositions
        int takenIndex = 0;
        int activeIndex = 0;
        for (int position = 1; position <= size; position++) {
            if (takenPositions.length == 0 || takenPositions[takenIndex] != position) {
                Pair pair = activePairs.get(activeIndex);
                pair.setPosition(position);
                activeIndex++;
            } else {
                if (takenIndex != passivePairs.size() - 1) { //Not last possible index
                    takenIndex++;
                }
            }
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
        applyPenalty(Penalty.LATE.getPenalty());
        applyPenalty(Penalty.MISSING.getPenalty());
    }

    private void applyPenalty(int penalization) {
        List<Pair> pairList = ladder.getLadder();
        int size = ladder.getLadderLength();
        for (Pair current : pairList) {
            int penalty = current.getPenalty();
            if (penalty == penalization) {
                current.setPenalty(Penalty.ZERO.getPenalty());
                int actualPosition = current.getPosition();
                int newPosition = actualPosition + penalty;

                if (newPosition > size) {
                    newPosition = size;
                }
                movePair(actualPosition, newPosition);
            }
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
                return p1.getPosition() - p2.getPosition();
            }
        };
    }

    private List<Pair> findPairs(List<Pair> fullLadder, boolean isPlaying) {
        return fullLadder.stream()
                .filter(p -> p.isPlaying() == isPlaying)
                .collect(Collectors.toList());
    }

    private void swapPair(int firstIndex, int secondIndex) {
        List<Pair> listPairs = ladder.getPairs();
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

        for (Pair current : ladder.getPairs()) {
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


