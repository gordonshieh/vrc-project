package ca.sfu.teambeta.core;


import com.google.gson.annotations.Expose;

import javax.persistence.Entity;

import ca.sfu.teambeta.persistence.Persistable;

/**
 * Created by Gordon Shieh on 25/05/16.
 */
@Entity(name = "Player")
public class Player extends Persistable {

    @Expose
    private String firstName;
    @Expose
    private String lastName;
    private String phoneNumber = "";

    public Player() {
    }

    public Player(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Player(String firstName, String lastName, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        final Player otherPlayer = (Player) other;
        return getFirstName().equals(otherPlayer.getFirstName()) &&
                getLastName().equals(otherPlayer.getLastName()) &&
                getPhoneNumber().equals(otherPlayer.getPhoneNumber());
    }

    @Override
    public int hashCode() {
        return 23 * getFirstName().hashCode() *
                getLastName().hashCode() * getPhoneNumber().hashCode();
    }
}
