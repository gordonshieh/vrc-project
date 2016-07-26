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

import java.util.List;
import java.util.Map;

import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Penalty;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Scorecard;
import ca.sfu.teambeta.core.Time;
import ca.sfu.teambeta.core.User;
import ca.sfu.teambeta.core.exceptions.AccountRegistrationException;
import ca.sfu.teambeta.logic.GameSession;
import ca.sfu.teambeta.logic.VrcLadderReorderer;
import ca.sfu.teambeta.logic.VrcScorecardGenerator;
import ca.sfu.teambeta.logic.VrcTimeSelection;

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

    // Used for testing purposes where session needs to be closed
    public DBManager(Session session) {
        this.session = session;
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
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
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
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
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

    public synchronized void persistEntity(Persistable entity) {
        Transaction tx = session.beginTransaction();
        try {
            session.saveOrUpdate(entity);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            e.printStackTrace();
        }
    }

    private Persistable getEntityFromID(Class persistable, int id) throws HibernateException {
        Transaction tx = null;
        Persistable entity = null;
        try {
            tx = session.beginTransaction();
            entity = (Persistable) session.load(persistable, id);
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

    private synchronized GameSession getGameSessionByVersion(GameSessionVersion version) {
        Transaction tx = null;
        List gameSessions = null;
        GameSession gameSession = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria idCriteria = DetachedCriteria.forClass(GameSession.class)
                    .setProjection(Projections.id());
            gameSessions = session.createCriteria(GameSession.class)
                    .add(Property.forName("id").in(idCriteria))
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
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return gameSession;
    }

    public synchronized GameSession getGameSessionLatest() {
        return getGameSessionByVersion(GameSessionVersion.CURRENT);
    }

    public synchronized GameSession getGameSessionPrevious() {
        return getGameSessionByVersion(GameSessionVersion.PREVIOUS);
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
            pair.setTimeSlot(Time.NO_SLOT);
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

    public synchronized Player getAlreadyActivePlayer(
            GameSession gameSession, int id) throws Exception {
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
        pair.setTimeSlot(Time.NO_SLOT);
        boolean activated = gameSession.setPairActive(pair);
        gameSession.createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
        persistEntity(gameSession);
        return activated;
    }

    public synchronized void setPairInactive(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);
        gameSession.setPairInactive(pair);
        gameSession.createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
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
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Scorecard.class, new ScorecardSerializer())
                .create();

        return gson.toJson(scorecards);
    }

    public synchronized String getJSONSession(String sessionToken) {
        //ex: {"email":"test@gmail.com","admin":true}
        JSONSerializer serializer = new SessionJSONSerializer(sessionToken);
        return serializer.toJson();
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

    // Rankings are in a format of pairID -> position in scorecard
    public synchronized void setMatchResults(int scorecardId, Map<Integer, Integer> rankings) {
        Scorecard sc = (Scorecard) getEntityFromID(Scorecard.class, scorecardId);
        for (Map.Entry<Integer, Integer> entry : rankings.entrySet()) {
            Pair pair = getPairFromID(entry.getKey());
            sc.setGameResults(pair, entry.getValue());
        }
        persistEntity(sc);
    }

    public synchronized void reorderLadder(GameSession gameSession) {
        gameSession.reorderLadder(new VrcLadderReorderer(), new VrcTimeSelection());
    }

    public synchronized GameSession createNewGameSession(GameSession sourceGameSession) {
        Ladder nextWeekLadder = sourceGameSession.getReorderedLadder();
        return new GameSession(nextWeekLadder);
    }

    public synchronized void saveGameSession(GameSession gameSession) {
        persistEntity(gameSession);
    }

    public enum GameSessionVersion {
        CURRENT,
        PREVIOUS
    }

    public synchronized void setTimeSlot(int pairId, Time time) {
        GameSession gameSession = getGameSessionLatest();
        Pair pair = getPairFromID(pairId);
        gameSession.setTimeSlot(pair, time);
        persistEntity(gameSession);
    }

    public Time getTimeSlot(int pairId) {
        GameSession gameSession = getGameSessionLatest();
        Pair pair = getPairFromID(pairId);
        Time time = pair.getTimeSlot();
        persistEntity(gameSession);
        return time;
    }

    public synchronized boolean userHasScorecardAccess(int userId, int scorecardId) {
        try {
            Transaction tx = session.beginTransaction();
            Scorecard scorecard =
                    (Scorecard) session.createQuery("from Scorecard sc " +
                            "join Scorecard_Pair sp on sp.Scorecard_id = sc.id " +
                            "join User u on u.associatedPlayer_id = sp.pairs_id " +
                            "where u.id = :userId and sp.Scorecard_id = :scorecardId;")
                            .setInteger("userId", userId)
                            .setInteger("scorecardId", scorecardId)
                            .uniqueResult();
            return scorecard != null;
        } catch (HibernateException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
