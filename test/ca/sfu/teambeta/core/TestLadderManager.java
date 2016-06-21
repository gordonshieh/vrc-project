package ca.sfu.teambeta.core;

import ca.sfu.teambeta.logic.LadderManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Samuel Kim on 6/21/2016.
 */
public class TestLadderManager {

    @Test
    public void testConstructor() {
        List<Pair> testingList = new ArrayList<Pair>();
        for (int i = 0; i < 10; i++) {
            testingList.add(new Pair(new Player(i, "Player" + i), new Player(i+1, "Player" + (i+1))));
            i++;
        }

        LadderManager ladMan = new LadderManager(testingList);
        Assert.assertEquals(ladMan.getLadder(), testingList);

        LadderManager ladManWithNull = new LadderManager(Collections.emptyList());
        Assert.assertEquals(ladManWithNull.getLadder(), Collections.emptyList());

        for(Pair p: testingList) {
            System.out.println(p.toString());
        }
    }

    @Test
    public void testAddRemovePairs() {
        List<Pair> testingList = new ArrayList<Pair>();
        for (int i = 0; i < 10; i++) {
            testingList.add(new Pair(new Player(i, "Player" + i), new Player(i+1, "Player" + (i+1))));
            i++;
        }
        LadderManager ladMan = new LadderManager(testingList);

        Pair pairToAddAtEnd = new Pair(new Player(11, "Player 11"), new Player(12, "Player 12"));
        Pair pairToInsert = new Pair(new Player(13, "Player 13"), new Player(14, "Player 14"));

        ladMan.addNewPair(pairToAddAtEnd);
        testingList.add(pairToAddAtEnd);
        Assert.assertEquals(ladMan.getLadder(), testingList);

        ladMan.addNewPairAtIndex(pairToInsert, 2);
        testingList.add(2, pairToInsert);
        Assert.assertEquals(ladMan.getLadder(), testingList);

        ladMan.removePairAtIndex(3);
        testingList.remove(3);
        Assert.assertEquals(ladMan.getLadder(), testingList);

        for (int i = 0; i < testingList.size(); i++) {
            ladMan.removePairAtIndex(0);
            Assert.assertEquals(ladMan.getLadder(), testingList);
        }
    }

    @Test
    public void testSplit() {
        List<Pair> testingList = new ArrayList<Pair>();
        List<Pair> testingListActive = new ArrayList<Pair>();
        List<Pair> testingListPassive = new ArrayList<Pair>();
        for (int i = 0; i < 10; i++) {
            Pair pairToAdd = new Pair(new Player(i, "Player" + i), new Player(i+1, "Player" + (i+1)));
            if(i%2 == 0) {
                testingList.add(pairToAdd);
                testingListActive.add(pairToAdd);
            } else {
                pairToAdd.deActivate();
                testingList.add(pairToAdd);
                testingListPassive.add(pairToAdd);
            }
            i++;
        }
        LadderManager ladMan = new LadderManager(testingList);

        Assert.assertEquals(ladMan.getActivePairs(), testingListActive);
        Assert.assertEquals(ladMan.getPassivePairs(), testingListPassive);
    }

    @Test
    public void testSetPlayingStatus() {
        List<Pair> testingList = new ArrayList<Pair>();
        for (int i = 0; i < 10; i++) {
            testingList.add(new Pair(new Player(i, "Player" + i), new Player(i+1, "Player" + (i+1))));
            i++;
        }
        LadderManager ladMan = new LadderManager(testingList);

        List<Pair> passivePairs = new ArrayList<Pair>();
        int index = 1;
        for(Pair p: testingList) {
            ladMan.setNotPlaying(p);
            passivePairs.add(p);
            List<Pair> activePairs = testingList.subList(index, testingList.size());
            Assert.assertEquals(ladMan.getActivePairs(), activePairs);
            index++;
        }
        Assert.assertEquals(ladMan.getPassivePairs(), passivePairs);
        for(Pair p: passivePairs) {
            ladMan.setIsPlaying(p);
        }
        Assert.assertEquals(ladMan.getLadder(), testingList);
    }
}
