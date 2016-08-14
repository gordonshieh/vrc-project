package ca.sfu.teambeta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.MultipartConfigElement;

import ca.sfu.teambeta.accounts.AccountDatabaseHandler;
import ca.sfu.teambeta.accounts.AccountManager;
import ca.sfu.teambeta.accounts.CredentialsManager;
import ca.sfu.teambeta.accounts.Responses.PasswordResetResponse;
import ca.sfu.teambeta.accounts.Responses.SecurityQuestionResponse;
import ca.sfu.teambeta.accounts.Responses.SessionResponse;
import ca.sfu.teambeta.accounts.UserSessionManager;
import ca.sfu.teambeta.core.JsonExtractedData;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Time;
import ca.sfu.teambeta.core.exceptions.AccountRegistrationException;
import ca.sfu.teambeta.core.exceptions.GeneralUserAccountException;
import ca.sfu.teambeta.core.exceptions.InvalidCredentialsException;
import ca.sfu.teambeta.core.exceptions.InvalidInputException;
import ca.sfu.teambeta.core.exceptions.NoSuchSessionException;
import ca.sfu.teambeta.core.exceptions.NoSuchUserException;
import ca.sfu.teambeta.logic.GameSession;
import ca.sfu.teambeta.logic.InputValidator;
import ca.sfu.teambeta.logic.TimeManager;
import ca.sfu.teambeta.logic.VrcTimeSelection;
import ca.sfu.teambeta.persistence.DBManager;

import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.patch;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.secure;
import static spark.Spark.staticFiles;

/**
 * Created by NoorUllah on 2016-06-16.
 */
public class AppController {
    public static final String DEVELOP_STATIC_HTML_PATH = ".";
    public static final String JAR_STATIC_HTML_PATH = "/web";
    public static final int DEVELOP_SERVER_PORT = 8000;
    public static final int JAR_SERVER_PORT = 443;
    public static final String PLAYING_STATUS = "playing";
    public static final String NOT_PLAYING_STATUS = "not playing";
    private static final String ID = "id";
    private static final String STATUS = "newStatus";
    private static final String POSITION = "position";
    private static final String TIME_SLOT_1 = "08:00 pm";
    private static final String TIME_SLOT_2 = "09:30 pm";
    private static final String GAMESESSION = "gameSession";
    private static final String GAMESESSION_PREVIOUS = "previous";
    private static final String GAMESESSION_LATEST = "latest";

    private static final String PENALTY = "penalty";
    private static final String LATE = "late";
    private static final String MISS = "miss";
    private static final String ACCIDENT = "accident";
    private static final String ZERO = "zero";

    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String REMEMBER_ME = "rememberMe";
    private static final String ANSWER = "answer";
    private static final String VOUCHER_CODE = "voucherCode";
    private static final String SECURITY_QUESTION = "securityQuestion";

    private static final String PAIR_NOT_FOUND = "No pair was found with given id";
    private static final String ID_NOT_INT = "Id is not of integer type";
    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final int NOT_AUTHENTICATED = 401;
    private static final int SERVER_ERROR = 500;
    private static final int OK = 200;
    private static final String KEYSTORE_LOCATION = "testkeystore.jks";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String SESSION_TOKEN_KEY = "sessionToken";
    private static final String LADDER_DISABLED = "Ladder is Disabled";
    private static Gson gson;

    public AppController(DBManager dbManager, CredentialsManager credentialsManager, int port, String staticFilePath) {
        final AccountDatabaseHandler accountDatabaseHandler = new AccountDatabaseHandler(dbManager);
        final AccountManager accountManager = new AccountManager(accountDatabaseHandler);
        port(port);
        staticFiles.location(staticFilePath);

        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String keystorePath = this.getClass().getClassLoader()
                .getResource(KEYSTORE_LOCATION).toString();
        secure(keystorePath, KEYSTORE_PASSWORD, null, null);

        before("/api/*", (request, response) -> {
            // Allow access to the login endpoint, so they can sign up/log in
            String endpoint = request.splat()[0];
            if (!endpoint.contains("login")) {

                String sessionToken = request.cookie(SESSION_TOKEN_KEY);
                try {
                    boolean authenticated =
                            UserSessionManager.authenticateSession(sessionToken);
                    if (!authenticated) {
                        halt(NOT_AUTHENTICATED, getNotAuthenticatedResponse(
                                "You must be logged in to view this page."));
                    }
                } catch (NoSuchSessionException exception) {
                    halt(NOT_AUTHENTICATED, getNotAuthenticatedResponse(
                            "You must be logged in to view this page."));
                }
                //UserSessionManager.isAdministratorSession(sessionToken);
            }
        });

        before("/api/matches", (request, response) -> {
            String requestedGameSession = request.queryParams(GAMESESSION);
            if (requestedGameSession == null) {
                halt(BAD_REQUEST, getErrResponse("Must specify gameSession: latest or previous"));
            }
        });

        get("/ladder", (request, response) -> {
            PebbleEngine engine = new PebbleEngine.Builder()
                    .templateCache(null)
                    .autoEscaping(false)
                    .build();
            PebbleTemplate compiledTemplate = engine.getTemplate("templates/ladder.html");

            Writer writer = new StringWriter();
            compiledTemplate.evaluate(writer);

            String output = writer.toString();
            response.body(output);
            return output;
        });

        get("/groups", (request, response) -> {
            PebbleEngine engine = new PebbleEngine.Builder()
                    .templateCache(null)
                    .autoEscaping(false)
                    .build();
            PebbleTemplate compiledTemplate = engine.getTemplate("templates/groups.html");

            Writer writer = new StringWriter();
            compiledTemplate.evaluate(writer);

            String output = writer.toString();
            response.body(output);
            return output;
        });

        //homepage: return ladder
        get("/api/ladder", (request, response) -> {
            String json = dbManager.getJSONLadder(dbManager.getGameSessionLatest());
            if (!json.isEmpty()) {
                return json;
            } else {
                response.status(NOT_FOUND);
                return getErrResponse("No ladder was found");
            }
        });

        //logout
        post("/api/logout", (request, response) -> {
            String sessionToken = request.cookie(SESSION_TOKEN_KEY);
            accountManager.logout(sessionToken);
            return getOkResponse("Logged out.");
        });

        //updates a pair's playing status or position
        patch("/api/ladder/:id", (request, response) -> {
            if (TimeManager.getInstance().isExpired()) {
                response.status(NOT_FOUND);
                return getErrResponse(LADDER_DISABLED);
            }
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
                ignored.getMessage();
            }

            String status = request.queryParams(STATUS);
            if (status == null) {
                status = "";
            }

            GameSession gameSession = dbManager.getGameSessionLatest();

            boolean validNewPos = InputValidator.checkLadderPosition(newPosition,
                    dbManager.getLadderSize(gameSession));

            boolean validStatus = InputValidator.checkPlayingStatus(status);

            if (!InputValidator.checkPairExists(dbManager, id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            if (!validStatus && !validNewPos) {
                response.status(BAD_REQUEST);
                return getErrResponse("Specify what to update: position or status");
            } else if (validStatus && !validNewPos) {
                if (status.equals(PLAYING_STATUS)) {
                    boolean statusChanged = dbManager.setPairActive(gameSession, id);
                    if (statusChanged) {
                        return getOkResponse("");
                    } else {
                        Player activePlayer = dbManager.getAlreadyActivePlayer(gameSession, id);
                        String firstName = activePlayer.getFirstName();
                        String lastName = activePlayer.getLastName();
                        response.status(NOT_FOUND);
                        return getErrResponse(
                                "Player " + firstName + " "
                                        + lastName + " is already playing");
                    }
                } else if (status.equals(NOT_PLAYING_STATUS)) {
                    dbManager.setPairInactive(gameSession, id);
                    return getOkResponse("");
                }

            } else if (!validStatus && validNewPos) {
                dbManager.movePair(gameSession, id, newPosition);
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

            GameSession gameSession = dbManager.getGameSessionLatest();

            boolean validPos = InputValidator.checkLadderPosition(
                    extractedData.getPosition(), dbManager.getLadderSize(gameSession));

            List<Player> newPlayers = extractedData.getPlayers();

            try {
                InputValidator.validateNewPlayers(newPlayers, MAX_SIZE);
            } catch (InvalidInputException exception) {
                response.status(BAD_REQUEST);
                return getErrResponse(exception.getMessage());
            }

            for (int i = 0; i < MAX_SIZE; i++) {
                int existingId = newPlayers.get(i).getExistingId();
                if (existingId >= 0) {
                    newPlayers.remove(i);
                    newPlayers.add(i, dbManager.getPlayerFromID(existingId));
                }
            }

            Pair pair = new Pair(newPlayers.get(0), newPlayers.get(1));

            if (validPos) {
                dbManager.addPair(gameSession, pair, extractedData.getPosition() - 1);
                response.status(OK);
            } else {
                dbManager.addPair(gameSession, pair);
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

            if (!InputValidator.checkPairExists(dbManager, id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            dbManager.removePair(id);
            response.status(OK);

            return getOkResponse("");
        });

        post("/api/matches", ((request, response) -> {
            GameSession gameSession = getRequestedGameSession(dbManager,
                    request.queryParams(GAMESESSION));

            dbManager.reorderLadder(gameSession);
            dbManager.saveGameSession(gameSession);

            if (request.queryParams(GAMESESSION).equals(GAMESESSION_LATEST)) {
                GameSession newGameSession = dbManager.createNewGameSession(gameSession);
                dbManager.saveGameSession(newGameSession);
            }

            //Update timeManager which enables ladder editing
            TimeManager.getInstance().updateTime();
            return getOkResponse("");
        }));

        //add a penalty to a pair
        post("/api/matches/:id", (request, response) -> {
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }

            if (!InputValidator.checkPairExists(dbManager, id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            String penaltyType = request.queryParams(PENALTY);

            GameSession gameSession = getRequestedGameSession(dbManager,
                    request.queryParams(GAMESESSION));

            if (penaltyType.equals(LATE)) {
                dbManager.addPenaltyToPair(gameSession, id, Penalty.LATE);
            } else if (penaltyType.equals(MISS)) {
                dbManager.addPenaltyToPair(gameSession, id, Penalty.MISSING);
            } else if (penaltyType.equals(ACCIDENT)) {
                dbManager.addPenaltyToPair(gameSession, id, Penalty.ACCIDENT);
            } else if (penaltyType.equals(ZERO)) {
                dbManager.addPenaltyToPair(gameSession, id, Penalty.ZERO);
            } else {
                response.status(BAD_REQUEST);
                return getErrResponse("Invalid Penalty Type");
            }
            return getOkResponse("");
        });

        //Show a list of matches
        get("/api/matches", (request, response) -> {
            GameSession gameSession = getRequestedGameSession(dbManager,
                    request.queryParams(GAMESESSION));
            if (gameSession == null) {
                response.status(OK);
                return "[]";
            }

            String json = dbManager.getJSONScorecards(gameSession);
            final String EMPTY_JSON_ARRAY = "[]";
            if (!json.equals(EMPTY_JSON_ARRAY)) {
                response.status(OK);
                return json;
            } else {
                // Request was fine, but we want to show an error message on the front end to notify the user
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
            Map<Integer, Integer> rankings = new HashMap<Integer, Integer>();
            for (Map<String, String> map : extractedData.getResults()) {
                int pairId = Integer.parseInt(map.get("pairId"));
                int newRanking = Integer.parseInt(map.get("newRanking"));
                rankings.put(pairId, newRanking);
            }
            dbManager.setMatchResults(id, rankings);

            response.status(OK);
            return getOkResponse("");
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

            if (!InputValidator.checkPairExists(dbManager, id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND);
            }

            GameSession gameSession = getRequestedGameSession(dbManager,
                    request.queryParams(GAMESESSION));

            if (!InputValidator.checkPairActive(dbManager, gameSession, id)) {
                response.status(BAD_REQUEST);
                return getErrResponse("The pair is not on the scorecard " + id);
            }

            dbManager.setPairInactive(gameSession, id);

            response.status(OK);
            return getOkResponse("");
        });

        //logging in an existing users
        post("/api/login", (request, response) -> {
            String body = request.body();
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(body).getAsJsonObject();
            String email = jsonObject.get(EMAIL).getAsString();
            String password = jsonObject.get(PASSWORD).getAsString();
            boolean rememberMe = jsonObject.get(REMEMBER_ME).getAsBoolean();

            try {
                SessionResponse sessionResponse = accountManager.login(email, password, rememberMe);
                response.cookie(SESSION_TOKEN_KEY, sessionResponse.getSessionToken());
                return gson.toJson(sessionResponse);
            } catch (NoSuchUserException | InvalidCredentialsException e) {
                response.status(NOT_AUTHENTICATED);
                return "";
            }
        });

        //get security question for password reset
        get("/api/login/reset", (request, response) -> {
            String email = request.queryParams(EMAIL);

            String securityQuestion;
            try {
                securityQuestion = credentialsManager.getUserSecurityQuestion(email);
            } catch (NoSuchUserException e) {
                response.status(NOT_FOUND);
                return getErrResponse("User " + email + " not found");
            }

            PasswordResetResponse passwordResetResponse = new PasswordResetResponse(securityQuestion);

            response.status(OK);
            return gson.toJson(passwordResetResponse);
        });

        //verify security question for password reset
        post("/api/login/reset", (request, response) -> {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(request.body()).getAsJsonObject();
            String email = jsonObject.get(EMAIL).getAsString();
            String answer = jsonObject.get(ANSWER).getAsString();

            String voucherCode;
            try {
                voucherCode = credentialsManager.validateSecurityQuestionAnswer(email, answer);
            } catch (NoSuchUserException e) {
                response.status(NOT_FOUND);
                return getErrResponse("User " + email + " not found");
            } catch (InvalidCredentialsException e) {
                response.status(NOT_AUTHENTICATED);
                return getErrResponse("Incorrect answer.");
            }

            SecurityQuestionResponse securityQuestionResponse = new SecurityQuestionResponse(voucherCode);

            response.status(OK);
            return gson.toJson(securityQuestionResponse);
        });

        //change password
        post("/api/login/change", (request, response) -> {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(request.body()).getAsJsonObject();
            String email = jsonObject.get(EMAIL).getAsString();
            String voucherCode = jsonObject.get(VOUCHER_CODE).getAsString();
            String password = jsonObject.get(PASSWORD).getAsString();

            try {
                InputValidator.validatePasswordFormat(password);
                credentialsManager.changePassword(email, password, voucherCode);
            } catch (NoSuchUserException e) {
                response.status(NOT_FOUND);
                return getErrResponse("User " + email + " not found");
            } catch (InvalidInputException e) {
                response.status(BAD_REQUEST);
                return getErrResponse(e.getMessage());
            }

            response.status(OK);
            return getOkResponse("Password reset.");
        });

        //registers a new user
        post("/api/login/new", (request, response) -> {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(request.body()).getAsJsonObject();
            String email = jsonObject.get(EMAIL).getAsString();
            String password = jsonObject.get(PASSWORD).getAsString();

            String message = "";
            try {
                accountManager.registerUser(email, password);

                return getOkResponse("Account registered");
            } catch (GeneralUserAccountException e) {
                message = e.getMessage();
            } catch (AccountRegistrationException e) {
                message = e.getMessage();
            } catch (InvalidInputException e) {
                message = e.getMessage();
            }

            response.status(BAD_REQUEST);
            return getErrResponse(message);
        });

        //registers a new user alongside a player
        post("/api/login/newFull", (request, response) -> {
            String body = request.body();
            JsonExtractedData extractedData = gson.fromJson(body, JsonExtractedData.class);
            String message = "";
            String email = extractedData.getEmail();
            String pwd = extractedData.getPassword();
            int playerID = -1;
            try {
                playerID = Integer.parseInt(extractedData.getPlayerId());
            } catch (Exception e) {
                message = e.getMessage();
            }
            String securityQuestion = extractedData.getSecurityQuestion();
            String securityAnswer = extractedData.getSecurityAnswer();
            try {
                accountManager.registerUserWithPlayer(email, pwd, playerID, securityQuestion, securityAnswer);
                response.status(OK);
                return getOkResponse("Account registered");
            } catch (GeneralUserAccountException e) {
                message = e.getMessage();
            } catch (AccountRegistrationException e) {
                message = e.getMessage();
            } catch (InvalidInputException e) {
                message = e.getMessage();
            }

            response.status(BAD_REQUEST);
            return getErrResponse(message);
        });

        //set security question
        patch("/api/login/new", (request, response) -> {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(request.body()).getAsJsonObject();
            String email = jsonObject.get(EMAIL).getAsString();
            String securityQuestion = jsonObject.get(SECURITY_QUESTION).getAsString();
            String answer = jsonObject.get(ANSWER).getAsString();

            // Authenticate the token, since our normal authentication doesn't run on login endpoints
            String sessionToken = request.cookie(SESSION_TOKEN_KEY);
            try {
                boolean authenticated =
                        UserSessionManager.authenticateSession(sessionToken);
                if (!authenticated) {
                    response.status(NOT_AUTHENTICATED);
                    return getErrResponse("You need to be logged in to change your security question.");
                }

                credentialsManager.setSecurityQuestion(email, securityQuestion, answer, sessionToken);

            } catch (NoSuchSessionException exception) {
                response.status(NOT_AUTHENTICATED);
                return getErrResponse("You need to be logged in to change your security question.");
            } catch (GeneralUserAccountException e) {
                response.status(400);
                return getErrResponse(e.getMessage());
            }

            response.status(OK);
            return getOkResponse("Security question set.");
        });

        //Set time to a pair and dynamically assign times to scorecards.
        patch("/api/ladder/time/:id", (request, response) -> {
            if (TimeManager.getInstance().isExpired()) {
                response.status(NOT_FOUND);
                return getErrResponse(LADDER_DISABLED);
            }
            int id;
            try {
                id = Integer.parseInt(request.params(ID));
            } catch (Exception e) {
                response.status(BAD_REQUEST);
                return getErrResponse(ID_NOT_INT);
            }

            if (!InputValidator.checkPairExists(dbManager, id)) {
                response.status(NOT_FOUND);
                return getErrResponse(PAIR_NOT_FOUND + id);
            }

            String body = request.body();
            JsonExtractedData extractedData = gson.fromJson(body, JsonExtractedData.class);
            String time = extractedData.getTime();

            if (time.equalsIgnoreCase(TIME_SLOT_1)) {
                dbManager.setTimeSlot(id, Time.SLOT_1);
            } else if (time.equalsIgnoreCase(TIME_SLOT_2)) {
                dbManager.setTimeSlot(id, Time.SLOT_2);
            } else {
                dbManager.setTimeSlot(id, Time.NO_SLOT);
            }

            GameSession gameSession = dbManager.getGameSessionLatest();
            VrcTimeSelection timeSelector = new VrcTimeSelection();
            timeSelector.distributePairs(gameSession.getScorecards(), gameSession.getTimeSlots());
            dbManager.persistEntity(gameSession);
            return getOkResponse("");

        });

        //download ladder to a new csv file
        get("/api/ladder/download", (request, response) -> {

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = "ladder_" + dateFormat.format(date) + ".csv";

            response.raw().setContentType("text/csv");
            response.raw().setHeader("Content-Disposition", "attachment; filename=" + fileName);
            GameSession gameSession = getRequestedGameSession(dbManager, GAMESESSION_LATEST);

            OutputStream outputStream = response.raw().getOutputStream();
            if (!dbManager.writeToCsvFile(outputStream, gameSession)) {
                return getErrResponse("Invalid File.");
            }
            return getOkResponse("");
        });

        //upload a csv file to create new ladder
        post("/api/ladder/upload", (request, response) -> {
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            InputStream inputStream = request.raw().getPart("csv_file").getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            if (!dbManager.importLadderFromCsv(inputStreamReader)) {
                return getErrResponse("");
            }
            response.redirect("/ladder.html");
            return getOkResponse("");
        });

        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            response.status(SERVER_ERROR);
            response.body(getErrResponse(exception.getMessage()));
        });
    }

    private GameSession getRequestedGameSession(DBManager dbManager, String requestedGameSession) {
        if (requestedGameSession.equals(GAMESESSION_LATEST)) {
            return dbManager.getGameSessionLatest();
        } else if (requestedGameSession.equals(GAMESESSION_PREVIOUS)) {
            return dbManager.getGameSessionPrevious();
        } else {
            return null;
        }
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
