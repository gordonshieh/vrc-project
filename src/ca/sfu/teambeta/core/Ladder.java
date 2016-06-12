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

    public Ladder(List<Pair> ladder) {
        this.ladder = ladder;
    }

    //returns false if pair was not found
    public boolean removePair(Pair pair) {
        return ladder.remove(pair);
    }

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
    }

    public void insertAtEnd(Pair pair) {
        ladder.add(pair);
        ladder.get(ladder.size() - 1).setPosition(ladder.size());
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
        return ladder.size();
    }
/* omitted but keeping in case we ever need it
    public void dumpLadder() {
        for(Pair pair : passivePairs) {
            System.out.println(pair);
        }
    } */
}
