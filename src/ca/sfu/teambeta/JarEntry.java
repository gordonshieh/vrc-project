package ca.sfu.teambeta;

import org.hibernate.SessionFactory;

import ca.sfu.teambeta.accounts.AccountDatabaseHandler;
import ca.sfu.teambeta.accounts.CredentialsManager;
import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.logic.GameSession;
import ca.sfu.teambeta.persistence.CSVReader;
import ca.sfu.teambeta.persistence.DBManager;

// Entry Point for Program when executing via a jar file
// Do not call me via the IDE!
public class JarEntry {
    public static void main(String[] args) {
        SessionFactory sessionFactory = DBManager.getProductionSession();
        DBManager dbManager = new DBManager(sessionFactory);

        Ladder newLadder = null;
        try {
            newLadder = CSVReader.setupLadder();
        } catch (Exception e) {
            System.out.println("INVALID CSV FILE");
        }

        GameSession gameSession = new GameSession(newLadder);
        dbManager.persistEntity(gameSession);

        AccountDatabaseHandler accountDatabaseHandler = new AccountDatabaseHandler(dbManager);
        CredentialsManager credentialsManager = new CredentialsManager(accountDatabaseHandler);

        AppController appController = new AppController(dbManager, credentialsManager, 8080,
                AppController.JAR_STATIC_HTML_PATH);
    }
}
