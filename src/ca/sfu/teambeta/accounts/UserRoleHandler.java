package ca.sfu.teambeta.accounts;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles user roles
 *
 */

public class UserRoleHandler {
    private List<String> administrators;
    private AccountDatabaseHandler accountDBHandler;

    // MARK: Constructor
    public UserRoleHandler(AccountDatabaseHandler accountDBHandler) {
        this.accountDBHandler = accountDBHandler;
        populateAdministratorList();
    }


    // MARK: The Core Role Handler Method(s)
    public UserRole getUserClearanceLevel(String email) {
        if (administrators.contains(email)) {
            return UserRole.ADMINISTRATOR;
        } else {
            return UserRole.REGULAR;
        }

    }

    public void addAdministrator(String email) {
        // Add the email to the database for persistence and
        //  cache the administrator locally.

        accountDBHandler.addAdministrator(email);
        administrators.add(email);
    }

    public void removeAdministrator(String email) {
        accountDBHandler.removeAdministrator(email);
        administrators.remove(email);
    }


    // MARK: Helper Method(s)
        private void populateAdministratorList() {
        // In the future this method can be changed to fetch
        //  a list of admin emails from the database, or a file, etc.

        List<String> admins = new ArrayList<>();

        final String DEMO_ADMIN_1 = "admin_billy@vrc.ca";
        final String DEMO_ADMIN_2 = "admin_zong@vrc.ca";

        admins.add(DEMO_ADMIN_1);
        admins.add(DEMO_ADMIN_2);

        administrators = admins;
    }

}
