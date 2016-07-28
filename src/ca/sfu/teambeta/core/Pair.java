package ca.sfu.teambeta.core;

//Pair should have information about pairs activity
//Ladder should return the size of itself

import com.google.gson.annotations.Expose;

import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import ca.sfu.teambeta.persistence.Persistable;


/**
 * Created by Gordon Shieh on 25/05/16.
 */
@Entity(name = "Pair")
@Embeddable
public class Pair extends Persistable {
    public static final int NUM_PLAYERS = 2;

    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Player> players = new HashSet<>();
    @Column(name = "date_created")
    @Type(type = "timestamp")
    private Date dateCreated;

    @Transient
    private int lastWeekPosition;
    @Transient
    @Expose
    private boolean isPlaying;
    @Expose
    private int pairScore;
    @Transient
    @Expose
    private Time timeSlot;

    public Pair() {
    }

    public Pair(Set<Player> players) {
        this.players = new HashSet<>(players);
        dateCreated = new Date();
        this.isPlaying = false;
        timeSlot = Time.NO_SLOT;
    }

    public Pair(Player firstPlayer, Player secondPlayer) {
        players.add(firstPlayer);
        players.add(secondPlayer);
        dateCreated = new Date();
        this.isPlaying = false;
        timeSlot = Time.NO_SLOT;
    }

    public Pair(Player firstPlayer, Player secondPlayer, boolean isPlaying) {
        players.add(firstPlayer);
        players.add(secondPlayer);
        dateCreated = new Date();
        this.isPlaying = isPlaying;
        timeSlot = Time.NO_SLOT;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public boolean hasPlayer(Player searchPlayer) {
        return (getPlayers().get(0).equals(searchPlayer)
                || getPlayers().get(1).equals(searchPlayer));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Pair otherPair = (Pair) other;

        return players.equals(otherPair.players);
    }

    @Override
    public int hashCode() {
        return getPlayers().hashCode();
    }

    public String toString() {
        return getPlayers().get(0).getFirstName()
                + " & " + getPlayers().get(1).getFirstName();
    }

    public int getPairScore() {
        return pairScore;
    }

    public void setPairScore(int pairScore) {
        this.pairScore = pairScore;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public Time getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(Time timeSlot) {
        this.timeSlot = timeSlot;
    }

    public int getLastWeekPosition() {
        return lastWeekPosition;
    }

    public void setLastWeekPosition(int lastWeekPosition) {
        this.lastWeekPosition = lastWeekPosition;
    }
}
