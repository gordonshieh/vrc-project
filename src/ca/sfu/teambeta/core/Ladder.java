package ca.sfu.teambeta.core;

import java.util.List;

/**
 * Ladder ranking object. Contains a List of pairs to indicate the ranking of each pair
 */
public class Ladder {
    //used for shiftPositions
    private static final int SHIFT_LEFT = 1;
    private static final int SHIFT_RIGHT = 2;

    private List<Pair> ladder;
    private int numPairs;

    public Ladder(List<Pair> ladder) {
        this.ladder = ladder;
        this.numPairs = ladder.size();
    }

    //returns false if pair was not found
    public boolean removePair(Pair pair) {
        int index = ladder.indexOf(pair);
        if (index != -1) { //pair was found
            ladder.remove(index);
            shiftPositions(SHIFT_LEFT, index);
            numPairs--;
        } else {
            return false;
        }
        return true;
    }

    /* omitted unless there is a need for function overload
    public void removePair(Player firstPlayer, Player secondPlayer){
        boolean removed = false;
        for (Pair current : passivePairs){
            if (current.hasPlayer(firstPlayer, secondPlayer)){
                int iPair = current.getPosition();
                passivePairs.remove(iPair);
                members--;
                removed = true;
            }
            if (removed){ //Process following pairs by moving them up
                int position = current.getPosition();
                current.setPosition(position - 1);
            }
        }
    }
    */
    //for use in add, remove methods. Shifts the position field of every pair starting at index.
    private void shiftPositions(int direction, int index) {
        if (direction == SHIFT_LEFT) {
            for (int i = index; i < ladder.size(); i++) {
                int position = ladder.get(i).getPosition();
                ladder.get(i).setPosition(position - 1);
            }
        } else if (direction == SHIFT_RIGHT) {
            for (int i = index; i < ladder.size(); i++) {
                int position = ladder.get(i).getPosition();
                ladder.get(i).setPosition(position + 1);
            }
        }
    }

    public void insertAtIndex(int index, Pair pair) {
        ladder.add(index, pair);
        ladder.get(index).setPosition(index + 1);
        shiftPositions(SHIFT_RIGHT, index + 1);
        numPairs++;
    }

    public void insertAtEnd(Pair pair) {
        ladder.add(pair);
        ladder.get(numPairs).setPosition(numPairs + 1);
        numPairs++;
    }

    public List<Pair> getLadder() {
        return ladder;
    }

    public void assignNewLadder(List<Pair> newLadder) {
        ladder = newLadder;
    }

    public Pair getPairAtIndex(int index) {
        return ladder.get(index);
    }

    public int getLadderLength() {
        return numPairs;
    }
/* omitted but keeping in case we ever need it
    public void dumpLadder() {
        for(Pair pair : passivePairs) {
            System.out.println(pair);
        }
    } */
}
