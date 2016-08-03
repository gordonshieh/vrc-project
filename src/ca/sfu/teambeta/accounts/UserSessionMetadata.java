package ca.sfu.teambeta.accounts;

import java.util.Calendar;

/**
 * The UserSessionMetadata class holds all the related information (metadata) on a logged in user's session.
 */

public class UserSessionMetadata {
    // TTL is calculated in days
    private static final int TIME_MEASUREMENT = Calendar.DAY_OF_MONTH;
    private static final int TIME_TO_LIVE_ADMIN = 3;
    private static final int TIME_TO_LIVE_ANON_USER = 2;
    private static final int TIME_TO_LIVE_DEFAULT = 15;
    private static final int TIME_TO_LIVE_EXTENDED = 60;

    private String email;
    private Calendar expiryDate;
    private UserRole role;

    // MARK: Constructors
    public UserSessionMetadata(String email, UserRole role) {
        this(email, role, false);
    }

    public UserSessionMetadata(String email, UserRole role, boolean extendSessionExpiry) {
        this.email = email;
        this.role = role;
        this.expiryDate = Calendar.getInstance();

        if (extendSessionExpiry) {
            setExtendedExpiry(role);
        } else {
            setNormalExpiry(role);
        }
    }

        // MARK: Getters
    public Calendar getExpiryDate() {
        return expiryDate;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }


    // MARK: Set/Check Expiration Methods
    public boolean isSessionExpired() {
        Calendar currentTime = Calendar.getInstance();
        int value = currentTime.compareTo(expiryDate);
        boolean expired = (value == 1);

        return expired;
    }

    private void setNormalExpiry(UserRole role) {
        switch (role) {
            case REGULAR:
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_DEFAULT);
                break;
            case ADMINISTRATOR:
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_ADMIN);
                break;
            case ANONYMOUS:
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_ANON_USER);
                break;
            default:
                // In case another role is added in the future this won't break
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_DEFAULT);
                break;
        }
    }

    private void setExtendedExpiry(UserRole role) {
        switch (role) {
            case REGULAR:
            case ADMINISTRATOR:
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_EXTENDED);
                break;
            case ANONYMOUS:
                // Leave out "remember me" behaviour for anonymous users
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_ANON_USER);
                break;
            default:
                // In case another role is added in the future this won't break
                expiryDate.add(TIME_MEASUREMENT, TIME_TO_LIVE_DEFAULT);
                break;
        }
    }



}
