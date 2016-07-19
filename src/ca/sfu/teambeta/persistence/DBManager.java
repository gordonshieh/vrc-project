package ca.sfu.teambeta.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Scorecard;
import ca.sfu.teambeta.core.User;
import ca.sfu.teambeta.core.exceptions.AccountRegistrationException;
import ca.sfu.teambeta.logic.GameSession;
import ca.sfu.teambeta.logic.VrcLadderReorderer;
import ca.sfu.teambeta.logic.VrcScorecardGenerator;

/**
 * Utility class that reads and writes data to the database
 */
public class DBManager {
    private static final String LOCAL_TESTING_CFG_XML = "hibernate.testing.cfg.xml";
    private static final String HIBERNATE_CLASSES_XML = "hibernate.classes.xml";
    private static final String PRODUCTION_CFG_XML = "hibernate.production.cfg.xml";
    private static final String DOCKER_CFG_XML = "hibernate.docker.cfg.xml";
    private static final String H2_CFG_XML = "hibernate.h2.cfg.xml";
    private static String TESTING_ENV_VAR = "TESTING";
    private Session session;

    public DBManager(SessionFactory factory) {
        this.session = factory.openSession();
    }

    // Use me if the database is down
    public static SessionFactory getHSQLSession() {
        Configuration config = new Configuration();
        config.configure(H2_CFG_XML);
        config.configure(HIBERNATE_CLASSES_XML);
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getMySQLSession(boolean create) {
        Configuration config = new Configuration();
        config.configure(LOCAL_TESTING_CFG_XML);
        config.configure(HIBERNATE_CLASSES_XML);
        if (create) {
            config.setProperty("hibernate.hbm2ddl.auto", "create");
        } else {
            config.setProperty("hibernate.hbm2ddl.auto", "update");
        }
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getProductionSession() {
        Configuration config = new Configuration();
        config.configure(PRODUCTION_CFG_XML);
        config.configure(HIBERNATE_CLASSES_XML);
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getTestingSession(boolean create) {
        boolean isTesting = System.getenv(TESTING_ENV_VAR) != null;
        Configuration config = new Configuration();
        if (isTesting) {
            config.configure(DOCKER_CFG_XML);
        } else {
            config.configure(LOCAL_TESTING_CFG_XML);
        }
        config.configure(HIBERNATE_CLASSES_XML);
        if (create) {
            config.setProperty("hibernate.hbm2ddl.auto", "create");
        } else {
            config.setProperty("hibernate.hbm2ddl.auto", "update");
        }
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public synchronized int persistEntity(Persistable entity) {
        Transaction tx = null;
        int key = 0;
        try {
            tx = session.beginTransaction();
            key = (int) session.save(entity);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        }
        return key;
    }

    private Persistable getEntityFromID(Class persistable, int id) throws HibernateException {
        Transaction tx = null;
        Persistable entity = null;
        try {
            tx = session.beginTransaction();
            entity = (Persistable) session.get(persistable, id);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return entity;
    }

    public synchronized Player getPlayerFromID(int id) {
        Player player = null;
        try {
            player = (Player) getEntityFromID(Player.class, id);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return player;
    }

    public synchronized Pair getPairFromID(int id) {
        Pair pair = null;
        try {
            pair = (Pair) getEntityFromID(Pair.class, id);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return pair;
    }

    public synchronized Ladder getLatestLadder() {
        Transaction tx = null;
        Ladder ladder = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria maxId = DetachedCriteria.forClass(Ladder.class)
                    .setProjection(Projections.max("id"));
            ladder = (Ladder) session.createCriteria(Ladder.class)
                    .add(Property.forName("id").eq(maxId))
                    .uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return ladder;
    }

    private synchronized GameSession getGameSessionByWeek(GameSessionWeek week, GameSessionVersion version) {
        List<Long> timestamps = getMinMaxTimestamps(week);
        long maxTimestamp = timestamps.get(0);
        long minTimestamp = timestamps.get(1);

        Transaction tx = null;
        List gameSessions = null;
        GameSession gameSession = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria weekCriteria = DetachedCriteria.forClass(GameSession.class)
                    .add(Restrictions.ge("timestamp", minTimestamp))
                    .add(Restrictions.lt("timestamp", maxTimestamp))
                    .setProjection(Projections.id());
            gameSessions = session.createCriteria(GameSession.class)
                    .add(Property.forName("id").in(weekCriteria))
                    .addOrder(Order.desc("timestamp"))
                    .list();
            tx.commit();

            int gameSessionIndex = -1;

            if (version == null || version == GameSessionVersion.CURRENT) {
                gameSessionIndex = 0;
            } else if (version == GameSessionVersion.PREVIOUS) {
                gameSessionIndex = 1;
            }

            gameSession = (GameSession) gameSessions.get(gameSessionIndex);

        } catch (HibernateException e) {
            tx.rollback();
        }
        return gameSession;
    }

    private List<Long> getMinMaxTimestamps(GameSessionWeek week) {
        List<Long> timestamps = new ArrayList<>();

        LocalDateTime dateTime = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        dateTime = dateTime.withHour(17);
        dateTime = dateTime.withMinute(0);
        dateTime = dateTime.withSecond(0);
        dateTime = dateTime.withNano(0);

        if (week == GameSessionWeek.LAST_WEEK) {
            dateTime = dateTime.minusWeeks(1);
        }

        timestamps.add(dateTime.toEpochSecond(ZoneOffset.ofTotalSeconds(0)));
        dateTime = dateTime.minusWeeks(1);
        timestamps.add(dateTime.toEpochSecond(ZoneOffset.ofTotalSeconds(0)));

        return timestamps;
    }

    public synchronized GameSession getGameSessionLatest() {
        return getGameSessionByWeek(GameSessionWeek.THIS_WEEK, GameSessionVersion.CURRENT);
    }

    public synchronized GameSession getGameSessionLatest(GameSessionVersion version) {
        return getGameSessionByWeek(GameSessionWeek.THIS_WEEK, version);
    }

    public synchronized GameSession getGameSessionPrevious() {
        return getGameSessionByWeek(GameSessionWeek.LAST_WEEK, GameSessionVersion.CURRENT);
    }

    public void addPenaltyToPair(GameSession gameSession, int pairId, Penalty penalty) {
        Pair pair = getPairFromID(pairId);
        gameSession.setPenaltyToPair(pair, penalty);
    }

    public synchronized void addPairToLatestLadder(Pair pair) {
        Transaction tx = null;
        Ladder ladder = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria maxId = DetachedCriteria.forClass(Ladder.class)
                    .setProjection(Projections.max("id"));
            ladder = (Ladder) session.createCriteria(Ladder.class)
                    .add(Property.forName("id").eq(maxId))
                    .uniqueResult();
            ladder.insertAtEnd(pair);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized void inputMatchResults(GameSession gameSession, Scorecard s, String[][] results) {
        List<Pair> teams = s.getReorderedPairs();
        int rows = results.length;
        int cols = teams.size();

        Pair teamWon = null;
        Pair teamLost = null;
        int winCount = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (results[i][j].equals("W")) {
                    teamWon = teams.get(j);
                    winCount++;
                } else if (results[i][j].equals("L")) {
                    teamLost = teams.get(j);
                    winCount--;
                }
            }
            if (winCount == 0 && teamWon != null && teamLost != null) {
                s.setGameResults(teamWon,teamLost);
            }
            winCount = 0;
            teamLost = null;
            teamWon = null;
        }
        persistEntity(gameSession);
    }

    public synchronized void addPair(GameSession gameSession, Pair pair, int position) {
        gameSession.addNewPairAtIndex(pair, position);
        persistEntity(gameSession);
    }

    public synchronized void addPair(GameSession gameSession, Pair pair) {
        gameSession.addNewPairAtEnd(pair);
        persistEntity(gameSession);
    }

    public synchronized boolean removePair(int pairId) {
        Transaction tx = null;
        Pair pair = null;
        Ladder ladder = null;
        boolean removed = false;
        try {
            tx = session.beginTransaction();
            pair = session.get(Pair.class, pairId);
            DetachedCriteria maxId = DetachedCriteria.forClass(Ladder.class)
                    .setProjection(Projections.max("id"));
            ladder = (Ladder) session.createCriteria(Ladder.class)
                    .add(Property.forName("id").eq(maxId))
                    .uniqueResult();
            removed = ladder.removePair(pair);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return removed;
    }

    public synchronized boolean hasPairID(int id) {
        return getPairFromID(id) != null;
    }

    public synchronized void movePair(GameSession gameSession, int pairId, int newPosition) {
        Pair pair = getPairFromID(pairId);

        removePair(pairId);
        gameSession.addNewPairAtIndex(pair, newPosition);
        persistEntity(gameSession);
    }

    public synchronized Player getAlreadyActivePlayer(GameSession gameSession, int id) throws Exception {
        Pair pair = getPairFromID(id);
        Player player;
        try {
            player = gameSession.getAlreadyActivePlayer(pair);
        } catch (Exception e) {
            throw e;
        }
        return player;
    }

    public synchronized boolean setPairActive(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);
        boolean activated = gameSession.setPairActive(pair);
        gameSession.createGroups(new VrcScorecardGenerator());
        persistEntity(gameSession);
        return activated;
    }

    public synchronized void setPairInactive(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);
        gameSession.setPairInactive(pair);
        gameSession.createGroups(new VrcScorecardGenerator());
        persistEntity(gameSession);
    }

    public synchronized boolean isActivePair(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);

        boolean status = gameSession.isActivePair(pair);
        persistEntity(gameSession);
        return status;
    }

    public synchronized int getLadderSize(GameSession gameSession) {
        List<Pair> ladder = gameSession.getAllPairs();
        return ladder.size();
    }

    public synchronized String getJSONLadder(GameSession gameSession) {
        List<Pair> ladder = gameSession.getAllPairs();
        JSONSerializer serializer = new LadderJSONSerializer(ladder,
                gameSession.getActivePairSet());
        return serializer.toJson();
    }

    public synchronized String getJSONScorecards(GameSession gameSession) {
        List<Scorecard> scorecards = gameSession.getScorecards();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        String json = gson.toJson(scorecards);
        return json;
    }

    public synchronized void setGameResults(GameSession gameSession, int winningPairId, int losingPairId) {
        int sessionId = gameSession.getID();
        Scorecard scorecard = (Scorecard) session.createQuery(
                "from Scorecard sc \n"
                        + "join session_Scorecard s_sc on (s_sc.scorecards_id = sc.id) "
                        + "join Scorecard_Pair sc_pwin on (sc_pwin.Scorecard_id = sc.id) "
                        + "join Scorecard_Pair sc_plose on (sc_plose.Scorecard_id = sc.id) "
                        + "where sc_pwin.pairs_id = :winning_pair_id "
                        + "and sc_plose.pairs_id = :losing_pair_id "
                        + "and s_sc.session_id = :session_id")
                .setInteger("winning_pair_id", winningPairId)
                .setInteger("losing_pair_id", winningPairId)
                .setInteger("session_id", sessionId)
                .uniqueResult();
        Pair winningPair = session.load(Pair.class, winningPairId);
        Pair losingPair = session.load(Pair.class, losingPairId);
        scorecard.setGameResults(winningPair, losingPair);

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(scorecard);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    private GameSession getGameSession(int gameSessionId) {
        Transaction tx = null;
        GameSession gameSession = null;
        try {
            tx = session.beginTransaction();
            gameSession = (GameSession) session.createCriteria(GameSession.class)
                    .add(Property.forName("id").eq(gameSessionId))
                    .uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return gameSession;
    }

    public synchronized User getUser(String email) {
        Transaction tx = null;
        User user = null;
        try {
            tx = session.beginTransaction();
            user = (User) session.createCriteria(User.class)
                    .add(Restrictions.eq("email", email))
                    .uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return user;
    }

    public synchronized void addNewUser(User user) throws AccountRegistrationException {
        String email = user.getEmail();
        boolean uniqueEmail = (getUser(email) == null);
        if (!uniqueEmail) {
            throw new AccountRegistrationException("The email '" + email + "' is already in use");
        }

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(user);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized void addNewPlayer(Player player) throws AccountRegistrationException {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(player);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized Scorecard getScorecardFromGame(GameSession gameSession, int index) {
        persistEntity(gameSession);
        return gameSession.getScorecardByIndex(index);
    }

    public synchronized void reorderLadder(GameSession gameSession) {
        gameSession.reorderLadder(new VrcLadderReorderer());
    }

    public synchronized GameSession createNewGameSession(GameSession sourceGameSession) {
        Ladder nextWeekLadder = sourceGameSession.getReorderedLadder();
        return new GameSession(nextWeekLadder);
    }

    public synchronized void migrateLadderData(GameSession previousVersion, GameSession latestVersion) {
        List<Pair> activePairs = previousVersion.getActivePairs();
        List<Pair> newPairs = latestVersion.getAllPairs();

        for (Pair activePair : activePairs) {
            for (Pair newPair : newPairs) {
                if (activePair.getID() == newPair.getID()) {
                    latestVersion.setPairActive(newPair);
                }
            }
        }
    }

    public synchronized void saveGameSession(GameSession gameSession) {
        persistEntity(gameSession);
    }

    public enum GameSessionVersion {
        CURRENT,
        PREVIOUS
    }

    public enum GameSessionWeek {
        THIS_WEEK,
        LAST_WEEK
    }
}
