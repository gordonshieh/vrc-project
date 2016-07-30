package ca.sfu.teambeta.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Time;

/**
 * Created by Gordon Shieh on 30/06/16.
 */
public class LadderJSONSerializer implements JSONSerializer {
    private List<Pair> pairList;
    private Set<Pair> activePairs;
    private Map<Pair, Time> timeSlots;
    private Timestamp timeStamp;
    
    LadderJSONSerializer(List<Pair> pairList, Set<Pair> activePairs, Map<Pair, Time> timeSlots, Timestamp timeStamp) {
        this.pairList = pairList;
        this.activePairs = activePairs;
        this.timeSlots = timeSlots;
        this.timeStamp = timeStamp;
    }

    private JsonObject getPairJsonObject(Pair pair, int position, boolean isPlaying) {
        JsonObject pairJson = new JsonObject();
        JsonArray playersArray = new JsonArray();
        for (Player player : pair.getPlayers()) {
            JsonObject playerJson = new JsonObject();
            playerJson.addProperty("firstName", player.getFirstName());
            playerJson.addProperty("lastName", player.getLastName());
            playerJson.addProperty("id", player.getID());
            playersArray.add(playerJson);
        }
        pairJson.add("players", playersArray);
        pairJson.addProperty("id", pair.getID());
        pairJson.addProperty("pairScore", pair.getPairScore());
        pairJson.addProperty("position", position);
        int positionChange = ( pair.getLastWeekPosition() - position );
        pairJson.addProperty("positionChange", positionChange);
        pairJson.addProperty("isPlaying", isPlaying);
        if (timeSlots.containsKey(pair)) {
            pairJson.addProperty("timeSlot", timeSlots.get(pair).toString());
        }
        return pairJson;
    }

    @Override
    public String toJson() {
        JsonObject ladderObject = new JsonObject();
        ladderObject.addProperty("timeStamp", this.timeStamp.toString());
        JsonArray pairsArray = new JsonArray();
        int position = 1;
        for (Pair pair : pairList) {
            JsonObject pairJson = getPairJsonObject(pair, position,
                    activePairs.contains(pair));
            position++;
            pairsArray.add(pairJson);
        }
        ladderObject.add("pairs", pairsArray);
        return ladderObject.toString();
    }
}
