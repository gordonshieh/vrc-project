package ca.sfu.teambeta.core;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

/**
 * Created by NoorUllah on 2016-06-22.
 */
public class JsonExtractedData {

    @Expose
    public List<Map<String, String>> results;
    @Expose
    private List<Player> players;
    @Expose
    private int position;
    @Expose
    private String email;
    @Expose
    private String password;
    @Expose
    private String time;
    @Expose
    private String file;


    public JsonExtractedData() {
        this.players = null;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPosition() {
        return position;
    }

    public List<Map<String, String>> getResults() {
        return results;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
