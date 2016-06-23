package ca.sfu.teambeta.logic;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import ca.sfu.teambeta.core.Observer;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.ScorecardAdapter;

/**
 * Created by Gordon Shieh on 25/05/16.
 */
public class GameManager {
    private List<Pair> ladder;
    @Expose
    private List<ScorecardAdapter> groups;
    private Observer observer = null;
    private int groupsDone = 0;

    public GameManager(List<Pair> activeLadder, LadderManager ladderManager) {
        ladder = activeLadder;
        groups = new ArrayList<>();
        observer = () -> {
            groupsDone++;
            if (groupsDone == groups.size()) {
                ladderManager.processLadder(groups);
            }
        };


        splitLadderIntoGroups();
    }

    public List<ScorecardAdapter> getScorecards() {
        return groups;
    }

    private void splitLadderIntoGroups() {
        int playingCount = ladder.size();

        ArrayList<Pair> groupings = new ArrayList<>();

        if (playingCount % 3 == 0) {
            //All 3 team groups.
            int noOfTripleGroups = playingCount / 3;
            makeTripleGroups(noOfTripleGroups, groupings);

        } else if (playingCount % 3 == 1) {
            //One 4 team group.
            int noOftripleGroups = playingCount / 3 - 1;
            int currentIndex = makeTripleGroups(noOftripleGroups, groupings);
            makeQuadGroup(currentIndex, groupings);
        } else {
            //Two 4 team groups.
            int noOftripleGroups = playingCount / 3 - 2;
            int currentIndex = makeTripleGroups(noOftripleGroups, groupings);
            makeQuadGroup(currentIndex, groupings);
        }
    }

    private void makeQuadGroup(int num, List<Pair> groupings) {
        for (int i = num; i < ladder.size(); i++) {
            groupings.add(ladder.get(i));

            if (groupings.size() == 4) {
                ScorecardAdapter sc = new ScorecardAdapter(groupings, observer);
                groups.add(sc);
                System.out.println();
                groupings.clear();
            }
        }
    }

    private int makeTripleGroups(int num, List<Pair> groupings) {
        int doneGroups = 0;
        int indexPosition = 0;

        for (int i = 0; i < ladder.size(); i++) {
            groupings.add(ladder.get(i));

            if (groupings.size() == 3) {
                ScorecardAdapter sc = new ScorecardAdapter(groupings, observer);
                groups.add(sc);
                System.out.println();
                groupings.clear();
                doneGroups++;
            }
            if (doneGroups == num) {
                indexPosition = i + 1;
                break;
            }
        }
        return indexPosition;
    }

    public void inputMatchResults(ScorecardAdapter s, String[][] results) {
        List<Pair> teams = s.getTeamRankings();
        int rows = results.length;
        int cols = teams.size();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (results[i][j].equals("W")) {
                    s.setWin(teams.get(j), i);
                } else if (results[i][j].equals("L")) {
                    s.setLose(teams.get(j), i);
                }
            }
        }
    }

    public void removePlayingPair(Pair pair) {
        ladder.remove(pair);
        groups.clear();
        splitLadderIntoGroups();
    }

    public void addPlayingPair(Pair pair) {
        ladder.add(pair);
        groups.clear();
        splitLadderIntoGroups();
    }

    public void updateGroups(List<Pair> active) {
        ladder = active;
        groups.clear();
        splitLadderIntoGroups();
    }
}
