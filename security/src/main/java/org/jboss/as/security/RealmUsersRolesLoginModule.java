package org.jboss.as.security;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.jboss.sasl.util.UsernamePasswordHashUtil;
import org.jboss.security.auth.spi.UsersRolesLoginModule;

/**
 * @author Jason T. Greene
 */
public class RealmUsersRolesLoginModule extends UsersRolesLoginModule{
    private UsernamePasswordHashUtil usernamePasswordHashUtil;
    private String realm;

    public RealmUsersRolesLoginModule() {
        try {
            usernamePasswordHashUtil = new UsernamePasswordHashUtil();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.realm = (String) options.get("realm");
        HashMap map = new HashMap(options);
        map.putAll(options);
        map.put("hashAlgorithm", "REALM");
        map.put("hashStorePassword", "false");
        super.initialize(subject, callbackHandler, sharedState, map);
    }

    @Override
    protected String createPasswordHash(String username, String password, String digestOption) throws LoginException {
        return usernamePasswordHashUtil.generateHashedHexURP(username, realm, password.toCharArray());
    }
}
