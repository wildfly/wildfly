/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_TABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERNAME_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERS_PASSWORD_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SQL_SELECT_USERS;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.RealmConfigurationConstants.VERIFY_PASSWORD_CALLBACK_SUPPORTED;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.jboss.sasl.util.UsernamePasswordHashUtil;

/**
 * A CallbackHandler for users within an Database directory.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseCallbackHandler implements Service<CallbackHandlerService>, CallbackHandlerService, CallbackHandler {

    public static final String SERVICE_SUFFIX = "database_authentication";

    private final InjectedValue<ConnectionManager> connectionManager = new InjectedValue<ConnectionManager>();
    private final String realm;
    private final boolean plainText;
    private String sqlStatement;
    private static UsernamePasswordHashUtil hashUtil;


    public DatabaseCallbackHandler(final String realmName,ModelNode userDatabase) {
        this.realm = realmName;
        plainText = userDatabase.require(PLAIN_TEXT).asBoolean();
        if (userDatabase.hasDefined(SIMPLE_SELECT_USERS)) {
            String table = userDatabase.require(SIMPLE_SELECT_TABLE).asString() ;
            String userNameField = userDatabase.require(SIMPLE_SELECT_USERNAME_FIELD).asString() ;
            String passwordField = userDatabase.require(SIMPLE_SELECT_USERS_PASSWORD_FIELD).asString();
            sqlStatement = String.format("select %s, %s from %s where %s=?",userNameField,passwordField,table,userNameField);
        } else if (userDatabase.hasDefined(SQL_SELECT_USERS)) {
            sqlStatement = userDatabase.require(SQL_SELECT_USERS).asString();
        }
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthenticationMechanism getPreferredMechanism() {
        return plainText ? AuthenticationMechanism.PLAIN : AuthenticationMechanism.DIGEST;
    }

    public Set<AuthenticationMechanism> getSupplementaryMechanisms() {
        return Collections.emptySet();
    }

    public Map<String, String> getConfigurationOptions() {
        return Collections.singletonMap(VERIFY_PASSWORD_CALLBACK_SUPPORTED, Boolean.TRUE.toString());
    }

    public boolean isReady() {
        return true;
    }

    public CallbackHandler getCallbackHandler(Map<String, Object> sharedState) {
        return this;
    }

    /*
     *  Service Methods
     */

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    public CallbackHandlerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     *  Access to Injectors
     */

    public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
        return connectionManager;
    }


    /*
     *  CallbackHandler Method
     */

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        List<Callback> toRespondTo = new LinkedList<Callback>();
        String userName = null;

        // A single pass may be sufficient but by using a two pass approach the Callbackhandler will not
        // fail if an unexpected order is encountered.

        // First Pass - is to double check no unsupported callbacks and to retrieve
        // information from the callbacks passing in information.
        for (Callback current : callbacks) {
            if (current instanceof AuthorizeCallback) {
                toRespondTo.add(current);
            } else if (current instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) current;
                userName = nameCallback.getDefaultName();
            } else if (current instanceof PasswordCallback && plainText) {
                toRespondTo.add(current);
            } else if (current instanceof DigestHashCallback && plainText == false) {
                toRespondTo.add(current);
            } else if (current instanceof VerifyPasswordCallback) {
                toRespondTo.add(current);
            } else if (current instanceof RealmCallback) {
                String realm = ((RealmCallback) current).getDefaultText();
                if (this.realm.equals(realm) == false) {
                    throw MESSAGES.invalidRealm(realm, this.realm);
                }
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }

        // Second Pass - Now iterate the Callback(s) requiring a response.
        for (Callback current : toRespondTo) {
            if (current instanceof AuthorizeCallback) {
                AuthorizeCallback authorizeCallback = (AuthorizeCallback) current;
                // Don't support impersonating another identity
                authorizeCallback.setAuthorized(authorizeCallback.getAuthenticationID().equals(authorizeCallback.getAuthorizationID()));
            } else {
                ConnectionManager connectionManager = this.getConnectionManagerInjector().getValue();
                String userPassword = getUser(connectionManager, userName);
                if (current instanceof PasswordCallback) {
                    ((PasswordCallback) current).setPassword(userPassword.toCharArray());
                } else if (current instanceof DigestHashCallback) {
                    ((DigestHashCallback) current).setHexHash(userPassword);
                } else if (current instanceof VerifyPasswordCallback) {
                    VerifyPasswordCallback vpc = (VerifyPasswordCallback) current;
                    if (plainText) {
                        vpc.setVerified(userPassword.equals(vpc.getPassword()));
                    } else {
                        UsernamePasswordHashUtil hashUtil = getHashUtil();
                        String hash;
                        synchronized (hashUtil) {
                            hash = hashUtil.generateHashedHexURP(userName, realm, vpc.getPassword().toCharArray());
                        }
                        vpc.setVerified(userPassword.equals(hash));
                    }
                }
            }
        }
    }

    private String getUser(ConnectionManager connectionManager, String userName) throws IOException {
        String password = null;
        Connection dbc = null;
        ResultSet rs = null;
        try {
            dbc = (Connection) connectionManager.getConnection();

            PreparedStatement preparedStatement = dbc.prepareStatement(sqlStatement, ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1,userName);
            rs = preparedStatement.executeQuery();
            if (rs.first()) {
                password = rs.getString(2);
            }
            rs.last();
            int updateCount = rs.getRow();
            if (updateCount>1) {
                throw MESSAGES.noneUniqueUserId(userName,updateCount);
            } else if (updateCount <= 0) {
                throw new UserNotFoundException(userName);
            }
        } catch (Exception e) {
            throw MESSAGES.cannotPerformVerification(e);
        } finally {
            try {
                closeSafely(rs, dbc);
            } catch (SQLException e) {
                throw MESSAGES.closeSafelyException(e);
            }
        }
        return password;
    }

    private void closeSafely(ResultSet rs, Connection dbc) throws SQLException {
        if (rs != null) {
            rs.close();
        }

        if (dbc != null) {
            dbc.close();
        }
    }

    private static UsernamePasswordHashUtil getHashUtil() {
        if (hashUtil == null) {
            try {
                hashUtil = new UsernamePasswordHashUtil();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        return hashUtil;
    }


}
