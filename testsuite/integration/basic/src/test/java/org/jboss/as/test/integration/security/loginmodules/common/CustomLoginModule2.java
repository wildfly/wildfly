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
public class CustomLoginModule2 extends UsersRolesLoginModule {

    public static final String GOODUSER2_USERNAME = "gooduser2";
    public static final String GOODUSER2_PASSWORD = "gooduser2Pwd2!";
    public static final String BADUSER2_USERNAME = "baduser2";
    public static final String BADUSER2_PASSWORD = "baduser2Pwd2!";

    private static final Logger log = Logger.getLogger(CustomLoginModule2.class);

    @Override
    public boolean login() throws LoginException {
        boolean login = super.login();
        log.tracef("login = %s, principal = %s", login, getIdentity());
        return login;
    }

    @Override
    protected Properties createRoles(Map<String, ?> options) throws IOException {
        Properties result = new Properties();
        result.put(GOODUSER2_USERNAME, "gooduser");
        result.put(BADUSER2_USERNAME, "baduser");
        return result;
    }

    @Override
    protected Properties createUsers(Map<String, ?> options) throws IOException {
        Properties result = new Properties();
        result.put(GOODUSER2_USERNAME, GOODUSER2_PASSWORD);
        result.put(BADUSER2_USERNAME, BADUSER2_PASSWORD);
        return result;
    }

}