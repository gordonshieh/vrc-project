package ca.sfu.teambeta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.List;

import ca.sfu.teambeta.core.JsonExtractedData;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Scorecard;
import ca.sfu.teambeta.core.exceptions.AccountRegistrationException;
import ca.sfu.teambeta.core.exceptions.InternalHashingException;
import ca.sfu.teambeta.core.exceptions.InvalidCredentialsException;
import ca.sfu.teambeta.core.exceptions.InvalidUserInputException;
import ca.sfu.teambeta.core.exceptions.NoSuchUserException;
import ca.sfu.teambeta.logic.AccountManager;
import ca.sfu.teambeta.persistence.DBManager;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.patch;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.secure;
import static spark.Spark.staticFiles;

/**
 * Created by NoorUllah on 2016-06-16.
 */
public class AppController {
    private static final String ID = "id";
    private static final String STATUS = "newStatus";
    private static final String POSITION = "position";
    private static final String PLAYING = "playing";
    private static final String NOT_PLAYING = "not playing";

    private static final String PENALTY = "penalty";
    private static final String LATE = "late";
    private static final String MISS = "miss";
    private static final String ACCIDENT = "accident";

    private static final String PAIR_NOT_FOUND = "No pair was found with given id";
    private static final String ID_NOT_INT = "Id is not of integer type";

    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final int OK = 200;

    private static final String KEYSTORE_LOCATION = "testkeystore.jks";
    private static final String KEYSTORE_PASSWORD = "password";

    private static Gson gson;
    private final String SESSION_TOKEN_KEY = "sessionToken";

    public AppController(DBManager dbManager) {
        port(8000);
        staticFiles.location(".");

        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        secure(KEYSTORE_LOCATION, KEYSTORE_PASSWORD, null, null);

        /*before("/api/*", (request, response) -> {
            // Allow access to the login endpoint, so they can sign up/log in
            String endpoint = request.splat()[0];
            if (!endpoint.contains("login")) {

                String sessionToken = request.cookie(SESSION_TOKEN_KEY);
                try {
                    boolean authenticated = UserSessionManager.authenticateSession(sessionToken);
                    if (!authenticated) {
                        halt(401, getNotAuthenticatedResponse("You must be logged in view this page."));
                    }
                } catch (NoSuchSessionException exception) {
                    halt(401, getNotAuthenticatedResponse("You must be logged in view this page."));
                }

            }
        });*/

        //homepage: return ladder
        get("/api/ladder", (request, response) -> {
            String json = dbManager.getJSONLadder();
            if (!json.isEmpty()) {
                return json;
            } else {
                response.status(NOT_FOUND);
                return getErrResponse("No ladder was found");
            }
        });

        //updates a pair's playing status or position
        patch("/api/ladder/:id", (request, response) -> {
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }

            int newPosition = -1;
            try {
                newPosition = Integer.parseInt(request.queryParams(POSITION)) - 1;
            } catch (Exception ignored) {

            }

            String status = request.queryParams(STATUS);
            if (status == null) {
                status = "";
            }

            boolean validNewPos = 0 <= newPosition && newPosition <= dbManager.getLadderSize();
            boolean validStatus = status.equals(PLAYING) || status.equals(NOT_PLAYING);

            if (!dbManager.hasPairID(id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            if (!validStatus && !validNewPos) {
                response.status(BAD_REQUEST);
                return getErrResponse("Specify what to update: position or status");
            } else if (validStatus && !validNewPos) {
                if (status.equals(PLAYING)) {
                    boolean statusChanged = dbManager.setPairActive(id);
                    if (statusChanged) {
                        return getOkResponse("");
                    } else {
                        Player activePlayer = dbManager.getAlreadyActivePlayer(id);
                        String firstName = activePlayer.getFirstName();
                        String lastName = activePlayer.getLastName();
                        response.status(NOT_FOUND);
                        return getErrResponse("Player " + firstName + " " + lastName + " is already playing");
                    }
                } else if (status.equals(NOT_PLAYING)) {
                    dbManager.setPairInactive(id);
                    return getOkResponse("");
                }

            } else if (!validStatus && validNewPos) {
                dbManager.movePair(id, newPosition);
                return getOkResponse("");

            } else {
                response.status(BAD_REQUEST);
                return getErrResponse("Cannot change both: position and status");
            }

            return getOkResponse("");
        });

        //add pair to ladder
        //in case of adding a pair at the end of ladder, position is length of ladder
        post("/api/ladder", (request, response) -> {
            String body = request.body();
            JsonExtractedData extractedData = gson.fromJson(body, JsonExtractedData.class);
            final int MAX_SIZE = 2;

            boolean validPos = 0 < extractedData.getPosition()
                    && extractedData.getPosition() <= dbManager.getLadderSize();

            List<Player> newPlayers = extractedData.getPlayers();

            if (newPlayers.size() != MAX_SIZE) {
                response.status(BAD_REQUEST);
                return getErrResponse("A Pair cannot have more than 2 players.");
            }

            for (int i = 0; i < MAX_SIZE; i++) {
                Integer existingId = newPlayers.get(i).getExistingId();
                if (existingId != null && existingId >= 0) {
                    newPlayers.remove(i);
                    newPlayers.add(i, dbManager.getPlayerFromID(existingId));
                }
            }

            Pair pair = new Pair(newPlayers.get(0), newPlayers.get(1));

            if (validPos) {
                dbManager.addPair(pair, extractedData.getPosition() - 1);
                response.status(OK);
            } else {
                dbManager.addPair(pair);
                response.status(OK);
            }

            return getOkResponse("");
        });

        //remove player from ladder
        delete("/api/ladder/:id", (request, response) -> {
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }

            if (!dbManager.hasPairID(id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            dbManager.removePair(id);
            response.status(OK);

            return getOkResponse("");
        });

        //add a penalty to a pair
        post("/api/matches/:id", (request, response) -> {
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }

            if (!dbManager.hasPairID(id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            String penaltyType = request.queryParams(PENALTY);

            if (penaltyType.equals(LATE)) {
                dbManager.addPenaltyToPairToLatestGameSession(id, Penalty.LATE);
            } else if (penaltyType.equals(MISS)) {
                dbManager.addPenaltyToPairToLatestGameSession(id, Penalty.MISSING);
            } else if (penaltyType.equals(ACCIDENT)) {
                dbManager.addPenaltyToPairToLatestGameSession(id, Penalty.ACCIDENT);
            } else {
                response.status(BAD_REQUEST);
                return getErrResponse("Invalid Penalty Type");
            }
            return getOkResponse("");
        });

        post("/api/matches", (request, response) -> {
            String json = dbManager.generateScorecards();
            return json;
        });

        //Show a list of matches
        get("/api/matches", (request, response) -> {
            String json = dbManager.getJSONScorecards();
            if (!json.isEmpty()) {
                response.status(OK);
                return json;
            } else {
                response.status(NOT_FOUND);
                return getErrResponse("No scorecards were found");
            }
        });

        //Input match results
        patch("/api/matches/:id", (request, response) -> {
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }
            String body = request.body();
            JsonExtractedData extractedData = gson.fromJson(body, JsonExtractedData.class);
            Scorecard scorecard = dbManager.getScorecardFromGame(id);

            try {
                dbManager.inputMatchResults(scorecard, extractedData.results.clone());
                response.status(OK);
                return getOkResponse("");
            } catch (Exception e) {
                response.body("Invalid result format.");
                response.status(BAD_REQUEST);
                return getErrResponse("Invalid result format.");
            }
        });

        //Remove a pair from a match
        delete("/api/matches/:id", (request, response) -> {
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }

            if (!dbManager.hasPairID(id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND);
            }

            if (!dbManager.isActivePair(id)) {
                response.status(BAD_REQUEST);
                return getErrResponse("The pair is not on the scorecard " + id);
            }

            dbManager.setPairInactive(id);

            response.status(OK);
            return getOkResponse("");
        });

        //logging in an existing users
        post("/api/login", (request, response) -> {
            String body = request.body();
            JsonExtractedData extractedData = gson.fromJson(body, JsonExtractedData.class);
            String email = extractedData.getEmail();
            String pwd = extractedData.getPassword();

            JsonObject successResponse = new JsonObject();
            String errMessage = "";
            String sessionToken = "";
            try {
                sessionToken = AccountManager.login(email, pwd);
                successResponse.addProperty(SESSION_TOKEN_KEY, sessionToken);
                return gson.toJson(successResponse);
            } catch (InternalHashingException e) {
                errMessage = e.getMessage();
            } catch (NoSuchUserException e) {
                errMessage = e.getMessage();
            } catch (InvalidUserInputException e) {
                errMessage = e.getMessage();
            } catch (InvalidCredentialsException e) {
                errMessage = e.getMessage();
            }

            return getErrResponse(errMessage);
        });

        //registers a new user
        post("/api/login/new", (request, response) -> {
            String body = request.body();
            JsonExtractedData extractedData = gson.fromJson(body, JsonExtractedData.class);
            String email = extractedData.getEmail();
            String pwd = extractedData.getPassword();

            String message = "";
            try {
                AccountManager.register(email, pwd);
                return getOkResponse("Account registered");
            } catch (InternalHashingException e) {
                message = e.getMessage();
            } catch (AccountRegistrationException e) {
                message = e.getMessage();
            } catch (InvalidUserInputException e) {
                message = e.getMessage();
            }

            return getErrResponse(message);
        });

    }

    private String getOkResponse(String message) {
        JsonObject okResponse = new JsonObject();
        okResponse.addProperty("status", "OK");
        okResponse.addProperty("message", message);
        return gson.toJson(okResponse);
    }

    private String getErrResponse(String message) {
        JsonObject errResponse = new JsonObject();
        errResponse.addProperty("status", "ERROR");
        errResponse.addProperty("message", message);
        return gson.toJson(errResponse);
    }

    private String getNotAuthenticatedResponse(String message) {
        JsonObject authResponse = new JsonObject();
        authResponse.addProperty("status", "AUTH_ERROR");
        authResponse.addProperty("message", message);
        return gson.toJson(authResponse);
    }
}
