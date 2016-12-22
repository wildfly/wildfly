package org.jboss.as.test.integration.security.loginmodules.common;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.UsersRolesLoginModule;

/**
 * A simple custom {@link LoginModule} with two hard coded users.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CustomLoginModule1 extends UsersRolesLoginModule {

    public static final String GOODUSER1_USERNAME = "gooduser1";
    public static final String GOODUSER1_PASSWORD = "gooduser1Pwd1!";
    public static final String BADUSER1_USERNAME = "baduser1";
    public static final String BADUSER1_PASSWORD = "baduser1Pwd1!";

    private static final Logger log = Logger.getLogger(CustomLoginModule1.class);

    @Override
    public boolean login() throws LoginException {
        boolean login = super.login();
        log.infof("login = %s, principal = %s", login, getIdentity());
        return login;
    }

    @Override
    protected Properties createRoles(Map<String, ?> options) throws IOException {
        Properties result = new Properties();
        result.put(GOODUSER1_USERNAME, "gooduser");
        result.put(BADUSER1_USERNAME, "baduser");
        return result;
    }

    @Override
    protected Properties createUsers(Map<String, ?> options) throws IOException {
        Properties result = new Properties();
        result.put(GOODUSER1_USERNAME, GOODUSER1_PASSWORD);
        result.put(BADUSER1_USERNAME, BADUSER1_PASSWORD);
        return result;
    }

}