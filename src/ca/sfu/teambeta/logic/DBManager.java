package ca.sfu.teambeta.logic;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Player;

/**
 * Utility class that reads and writes data to the database
 */
public class DBManager {
    private static final String DEFAULT_FILENAME = "ladder.csv";
    private static final int PLAYERID_INDEX = 0;
    private static final int PLAYERNAME_INDEX = 1;
    private static String OVERRIDDEN_FILENAME = null;

    /**
     * Saves values to the database.
     *
     * Schema: PlayerID, Player Name
     *
     * @param ladder: A Ladder object
     */
    public static void saveToDB(Ladder ladder) {
        List<String[]> values = new ArrayList<>();

        for (Pair pair : ladder.getLadder()) {
            List<Player> players = pair.getPlayers();
            for (Player player : players) {
                String[] playerData = {String.valueOf(player.getPlayerID()), player.getName()};
                values.add(playerData);
            }
        }

        String filename = OVERRIDDEN_FILENAME != null ? OVERRIDDEN_FILENAME : DEFAULT_FILENAME;
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            for (String[] nextLine : values) {
                writer.writeNext(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveToDB(Ladder ladder, String filename) {
        OVERRIDDEN_FILENAME = filename;
        saveToDB(ladder);
        OVERRIDDEN_FILENAME = null;
    }

    /**
     * Loads values from the database.
     *
     * @return Ladder
     */
    public static Ladder loadFromDB() {
        String filename = OVERRIDDEN_FILENAME != null ? OVERRIDDEN_FILENAME : DEFAULT_FILENAME;
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            List<String[]> ladderEntries = reader.readAll();
            List<Pair> pairs = new ArrayList<>();

            Iterator<String[]> ladderIter = ladderEntries.iterator();
            while (ladderIter.hasNext()) {
                String[] player1Data = ladderIter.next();
                String[] player2Data = ladderIter.next();
                Player player1 = new Player(
                        Integer.parseInt(player1Data[PLAYERID_INDEX]),
                        player1Data[PLAYERNAME_INDEX]
                );
                Player player2 = new Player(
                        Integer.parseInt(player2Data[PLAYERID_INDEX]),
                        player2Data[PLAYERNAME_INDEX]
                );
                Pair pair = new Pair(player1, player2);
                pairs.add(pair);
            }

            return new Ladder(pairs);
        } catch (IOException e) {
            return new Ladder(new ArrayList<>());
        }
    }

    public static Ladder loadFromDB(String filename) {
        OVERRIDDEN_FILENAME = filename;
        Ladder ladder = loadFromDB();
        OVERRIDDEN_FILENAME = null;
        return ladder;
    }
}
