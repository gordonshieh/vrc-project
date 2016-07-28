package ca.sfu.teambeta.logic;

import org.apache.commons.lang3.StringUtils;

import ca.sfu.teambeta.core.Scorecard;
import ca.sfu.teambeta.core.exceptions.InvalidInputException;
import ca.sfu.teambeta.persistence.DBManager;

import static ca.sfu.teambeta.AppController.NOT_PLAYING_STATUS;
import static ca.sfu.teambeta.AppController.PLAYING_STATUS;

/**
 * This class holds methods to validate input that is passed in
 * from the front-end.
 *
 */

public class InputValidator {
    private static final int MAX_EMAIL_LENGTH = 40;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int PHONE_NUMBER_LENGTH = 10;

    // MARK: Core Input Validation Methods
    public static void validateEmailFormat(String email) throws InvalidInputException {
        validateNullOrEmptyString(email);

        // See citations.txt for source of Regex pattern
        String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        boolean emailNotValid = !email.matches(emailPattern);

        boolean emailTooLong = email.length() > MAX_EMAIL_LENGTH;


        if (emailTooLong) {
            throw new InvalidInputException("The email address cannot"
                    + " exceed the allowed length of " + MAX_EMAIL_LENGTH
                    + " characters (includes special characters such as '@' and '.')");
        } else if (emailNotValid) {
            throw new InvalidInputException("The email address is not in a valid format");
        }
    }

    public static void validatePasswordFormat(String password) throws InvalidInputException {
        validateNullOrEmptyString(password);

        boolean passwordTooLong = password.length() > MAX_PASSWORD_LENGTH;
        boolean passwordTooShort = password.length() < MIN_PASSWORD_LENGTH;


        if (passwordTooLong) {
            throw new InvalidInputException("The password cannot exceed the "
                    + "allowed length of " + MAX_PASSWORD_LENGTH);
        } else if (passwordTooShort) {
            throw new InvalidInputException("The password cannot be less than "
                    + MIN_PASSWORD_LENGTH + " characters");
        }

    }

    public static void validatePhoneNumberFormat(String phoneNumber) throws InvalidInputException {
        validateNullOrEmptyString(phoneNumber);

        boolean invalidPhoneNumberLength = phoneNumber.length() != PHONE_NUMBER_LENGTH;
        boolean phoneNumberNonNumeric = !StringUtils.isNumeric(phoneNumber);


        if (invalidPhoneNumberLength) {
            throw new InvalidInputException("The phone number field must be "
                    + PHONE_NUMBER_LENGTH
                    + "characters. \nPlease ensure there are no dashes"
                    + " or spaces. IE: '6045551111'");
        } else if (phoneNumberNonNumeric) {
            throw new InvalidInputException("The phone number must only contain digits"
                    + "\nPlease ensure there are no dashes or spaces. IE: '6045551111'");
        }

    }

    public static void validateSessionIdFormat(String sessionId)
            throws InvalidInputException {
        validateNullOrEmptyString(sessionId);

    }

    public static void validateResults(Scorecard scorecard, String[][] results)
            throws InvalidInputException {

        int numTeams = scorecard.getReorderedPairs().size();
        int numRounds = results.length;
        if (numRounds != numTeams) {
            throw new InvalidInputException("Results must have " + numRounds + " rounds");
        }

        int numGamesPerRound = results.length;
        final int CORRECT_PLAYED_GAMES_PER_ROUND = 2;
        final int CORRECT_BYE_GAMES_PER_ROUND = numGamesPerRound - CORRECT_PLAYED_GAMES_PER_ROUND;

        for (String[] round : results) {
            int gamesNotPlayed = 0;
            for (String result : round) {
                if (result.equals("-")) {
                    gamesNotPlayed++;
                }
            }
            if (gamesNotPlayed != CORRECT_BYE_GAMES_PER_ROUND) {
                throw new InvalidInputException("Results must have "
                        + CORRECT_PLAYED_GAMES_PER_ROUND
                        + " games played every round.");
            }
        }
    }

    public static void validatePlayerName(String name) throws InvalidInputException {
        boolean isAlpha = name.chars().allMatch(Character::isAlphabetic);
        if (!isAlpha) {
            throw new InvalidInputException("Name is not alphabetic.");
        }
    }

    public static boolean checkPairExists(DBManager dbManager, int id) {
        return dbManager.hasPairID(id);
    }

    public static boolean checkPairActive(DBManager dbManager, GameSession gameSession, int id) {
        return dbManager.isActivePair(gameSession, id);
    }

    public static boolean checkLadderPosition(int position, int ladderSize) {
        return 0 <= position && position <= ladderSize;
    }


    public static boolean checkPlayingStatus(String status) {
        return status.equals(PLAYING_STATUS) || status.equals(
                NOT_PLAYING_STATUS);
    }

    // MARK: Helper Methods
    private static void validateNullOrEmptyString(String str) throws InvalidInputException {
        // This helper method will throw an exception if it encounters a
        //  null or empty string as opposed to returning a boolean so
        //  that the calling method doesn't crash down the road as it trys
        //  to call other methods on the String

        if (str == null || str.isEmpty()) {
            throw new InvalidInputException("The input cannot be empty");
        }
    }
}
