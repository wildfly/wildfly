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

import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.RealmConfigurationConstants.VERIFY_PASSWORD_CALLBACK_SUPPORTED;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.security.LdapSearcherCache.AttachmentKey;
import org.jboss.as.domain.management.security.LdapSearcherCache.DirContextFactory;
import org.jboss.as.domain.management.security.LdapSearcherCache.SearchResult;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.sasl.callback.VerifyPasswordCallback;

/**
 * A CallbackHandler for users within an LDAP directory.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UserLdapCallbackHandler implements Service<CallbackHandlerService>, CallbackHandlerService {

    private static final AttachmentKey<PasswordCredential> PASSWORD_KEY = AttachmentKey.create(PasswordCredential.class);

    private static final String SERVICE_SUFFIX = "ldap";

    public static final String DEFAULT_USER_DN = "dn";

    private final InjectedValue<ConnectionManager> connectionManager = new InjectedValue<ConnectionManager>();
    private final InjectedValue<LdapSearcherCache<LdapEntry, String>> userSearcherInjector = new InjectedValue<LdapSearcherCache<LdapEntry, String>>();

    private final boolean allowEmptyPassword;
    private final boolean shareConnection;
    protected final int searchTimeLimit = 10000; // TODO - Maybe make configurable.

    public UserLdapCallbackHandler(boolean allowEmptyPassword, boolean shareConnection) {
        this.allowEmptyPassword = allowEmptyPassword;
        this.shareConnection = shareConnection;
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthMechanism getPreferredMechanism() {
        return AuthMechanism.PLAIN;
    }

    public Set<AuthMechanism> getSupplementaryMechanisms() {
        return Collections.emptySet();
    }

    public Map<String, String> getConfigurationOptions() {
        return Collections.singletonMap(VERIFY_PASSWORD_CALLBACK_SUPPORTED, Boolean.TRUE.toString());
    }

    public boolean isReady() {
        return true;
    }

    public CallbackHandler getCallbackHandler(Map<String, Object> sharedState) {
        return new LdapCallbackHandler(sharedState);
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

    public Injector<LdapSearcherCache<LdapEntry, String>> getLdapUserSearcherInjector() {
        return userSearcherInjector;
    }


    /*
     *  CallbackHandler Method
     */

    private class LdapCallbackHandler implements CallbackHandler {

        private final Map<String, Object> sharedState;

        private LdapCallbackHandler(final Map<String, Object> sharedState) {
            this.sharedState = sharedState;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            if (callbacks.length == 1 && callbacks[0] instanceof AuthorizeCallback) {
                AuthorizeCallback acb = (AuthorizeCallback) callbacks[0];
                String authenticationId = acb.getAuthenticationID();
                String authorizationId = acb.getAuthorizationID();
                boolean authorized = authenticationId.equals(authorizationId);
                if (authorized == false) {
                    SECURITY_LOGGER.tracef(
                            "Checking 'AuthorizeCallback', authorized=false, authenticationID=%s, authorizationID=%s.",
                            authenticationId, authorizationId);
                }
                acb.setAuthorized(authorized);

                return;
            }

            final ConnectionManager connectionManager = UserLdapCallbackHandler.this.connectionManager.getValue();
            VerifyPasswordCallback verifyPasswordCallback = null;
            String username = null;

            for (Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    username = ((NameCallback) current).getDefaultName();
                } else if (current instanceof RealmCallback) {
                    // TODO - Nothing at the moment
                } else if (current instanceof VerifyPasswordCallback) {
                    verifyPasswordCallback = (VerifyPasswordCallback) current;
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }

            if (username == null || username.length() == 0) {
                SECURITY_LOGGER.trace("No username or 0 length username supplied.");
                throw MESSAGES.noUsername();
            }
            if (verifyPasswordCallback == null) {
                SECURITY_LOGGER.trace("No password supplied.");
                throw MESSAGES.noPassword();
            }
            String password = verifyPasswordCallback.getPassword();
            if (password == null || (allowEmptyPassword == false && password.length() == 0)) {
                SECURITY_LOGGER.trace("No password or 0 length password supplied.");
                throw MESSAGES.noPassword();
            }

            final VerifyPasswordCallback theVpc = verifyPasswordCallback;
            DirContextFactory dcf = new DirContextFactory() {

                private DirContext context;

                @Override
                public DirContext getDirContext() throws IOException {
                    if (context != null) {
                        return context;
                    }

                    try {
                        return context = (DirContext) connectionManager.getConnection();
                    } catch (Exception e) {
                        if (e instanceof IOException) {
                            throw (IOException) e;
                        }
                        throw new IOException(e);
                    }
                }

                @Override
                public void close() {
                    safeClose(theVpc, context);
                }
            };

            DirContext userContext = null;
            try {
                // 2 - Search to identify the DN of the user connecting
                SearchResult<LdapEntry> searchResult = userSearcherInjector.getValue().search(dcf, username);
                LdapEntry ldapEntry = searchResult.getResult();

                // 3 - Connect as user once their DN is identified
                final PasswordCredential cachedCredential = searchResult.getAttachment(PASSWORD_KEY);
                if (cachedCredential != null) {
                    if (cachedCredential.verify(password)) {
                        SECURITY_LOGGER.tracef("Password verified for user '%s' (using cached password)", username);
                        verifyPasswordCallback.setVerified(true);
                        sharedState.put(LdapEntry.class.getName(), ldapEntry);
                    } else {
                        SECURITY_LOGGER.tracef("Password verification failed for user (using cached password) '%s'", username);
                        verifyPasswordCallback.setVerified(false);
                    }
                } else {
                    try {
                        userContext = (DirContext) connectionManager.getConnection(ldapEntry.getDistinguishedName(), password);
                        if (userContext != null) {
                            SECURITY_LOGGER.tracef("Password verified for user '%s' (using connection attempt)", username);
                            verifyPasswordCallback.setVerified(true);
                            searchResult.attach(PASSWORD_KEY, new PasswordCredential(password));
                            sharedState.put(LdapEntry.class.getName(), ldapEntry);
                        }
                    } catch (Exception e) {
                        SECURITY_LOGGER.tracef("Password verification failed for user (using connection attempt) '%s'",
                                username);
                        verifyPasswordCallback.setVerified(false);
                    }
                }
            } catch (Exception e) {
                SECURITY_LOGGER.trace("Unable to verify identity.", e);
                throw MESSAGES.cannotPerformVerification(e);
            } finally {
                dcf.close();
                UserLdapCallbackHandler.this.safeClose(userContext);
            }
        }

        private void safeClose(final VerifyPasswordCallback vpc, final DirContext context) {
            if (shareConnection && context != null && vpc != null && vpc.isVerified()) {
                sharedState.put(DirContext.class.getName(), context);
            } else {
                UserLdapCallbackHandler.this.safeClose(context);
            }
        }

    }

    private void safeClose(Context context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void safeClose(NamingEnumeration ne) {
        if (ne != null) {
            try {
                ne.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }

    }

    private static final class PasswordCredential {

        private final String password;

        private PasswordCredential(final String password) {
            this.password = password;
        }

        private boolean verify(final String password) {
            return this.password.equals(password);
        }
    }

}
