/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.RealmConfigurationConstants.DIGEST_PLAIN_TEXT;
import static org.jboss.as.domain.management.RealmConfigurationConstants.VERIFY_PASSWORD_CALLBACK_SUPPORTED;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.jboss.sasl.util.UsernamePasswordHashUtil;

/**
 * A CallbackHandler obtaining the users and their passwords from a properties file.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesCallbackHandler extends UserPropertiesFileLoader implements Service<CallbackHandlerService>,
        CallbackHandlerService, CallbackHandler {

    private static final String SERVICE_SUFFIX = "properties_authentication";

    private static UsernamePasswordHashUtil hashUtil = null;

    private final String realm;
    private final boolean plainText;

    public PropertiesCallbackHandler(String realm, String path, boolean plainText) {
        super(path);
        this.realm = realm;
        this.plainText = plainText;
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthMechanism getPreferredMechanism() {
        return AuthMechanism.DIGEST;
    }

    public Set<AuthMechanism> getSupplementaryMechanisms() {
        return Collections.singleton(AuthMechanism.PLAIN);
    }

    public Map<String, String> getConfigurationOptions() {
        Map<String, String> response = new HashMap<String, String>(2);
        response.put(DIGEST_PLAIN_TEXT, Boolean.toString(plainText));
        response.put(VERIFY_PASSWORD_CALLBACK_SUPPORTED, Boolean.TRUE.toString());
        return response;
    }

    public boolean isReady() {
        Properties users;
        try {
            users = getProperties();
        } catch (IOException e) {
            return false;
        }
        return (users.size() > 0);
    }

    public CallbackHandler getCallbackHandler(Map<String, Object> sharedState) {
        return this;
    }

    /*
     * Service Methods
     */

    @Override
    protected void verifyProperties(Properties properties) throws IOException {
        final String admin = "admin";
        if (properties.contains(admin) && admin.equals(properties.get(admin))) {
            ROOT_LOGGER.userAndPasswordWarning();
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        try {
            String fileRealm = getRealmName();
            if (fileRealm != null && realm.equals(getRealmName()) == false) {
                ROOT_LOGGER.realmMisMatch(realm, fileRealm);
            }
        } catch (IOException e) {
            throw MESSAGES.unableToLoadProperties(e);
        }
    }

    public CallbackHandlerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /*
     * CallbackHandler Methods
     */

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        List<Callback> toRespondTo = new LinkedList<Callback>();

        String userName = null;
        boolean userFound = false;

        Properties users = getProperties();

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
                userFound = users.containsKey(userName);
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
                AuthorizeCallback acb = (AuthorizeCallback) current;
                boolean authorized = acb.getAuthenticationID().equals(acb.getAuthorizationID());
                if (authorized == false) {
                    SECURITY_LOGGER.tracef(
                            "Checking 'AuthorizeCallback', authorized=false, authenticationID=%s, authorizationID=%s.",
                            acb.getAuthenticationID(), acb.getAuthorizationID());
                }
                acb.setAuthorized(authorized);
            } else if (current instanceof PasswordCallback) {
                if (userFound == false) {
                    SECURITY_LOGGER.tracef("User '%s' not found in properties file.", userName);
                    throw new UserNotFoundException(userName);
                }
                String password = users.get(userName).toString();
                ((PasswordCallback) current).setPassword(password.toCharArray());
            } else if (current instanceof DigestHashCallback) {
                if (userFound == false) {
                    SECURITY_LOGGER.tracef("User '%s' not found in properties file.", userName);
                    throw new UserNotFoundException(userName);
                }
                String hash = users.get(userName).toString();
                ((DigestHashCallback) current).setHexHash(hash);
            } else if (current instanceof VerifyPasswordCallback) {
                if (userFound == false) {
                    SECURITY_LOGGER.tracef("User '%s' not found in properties file.", userName);
                    throw new UserNotFoundException(userName);
                }
                VerifyPasswordCallback vpc = (VerifyPasswordCallback) current;
                if (plainText) {
                    String password = users.get(userName).toString();
                    boolean verified = password.equals(vpc.getPassword());
                    if (verified == false) {
                        SECURITY_LOGGER.tracef("Password verification failed for user '%s'", userName);
                    }
                    vpc.setVerified(verified);
                } else {
                    UsernamePasswordHashUtil hashUtil = getHashUtil();
                    String hash;
                    synchronized (hashUtil) {
                        hash = hashUtil.generateHashedHexURP(userName, realm, vpc.getPassword().toCharArray());
                    }
                    String expected = users.get(userName).toString();
                    boolean verified = expected.equals(hash);
                    if (verified == false) {
                        SECURITY_LOGGER.tracef("Digest verification failed for user '%s'", userName);
                    }
                    vpc.setVerified(verified);
                }
            }
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

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }
    }

}
