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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_ATTRIBUTE;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.RealmConfigurationConstants.VERIFY_PASSWORD_CALLBACK_SUPPORTED;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.msc.service.Service;
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
public class UserLdapCallbackHandler implements Service<CallbackHandlerService>, CallbackHandlerService, CallbackHandler {

    public static final String SERVICE_SUFFIX = "ldap";

    public static final String DEFAULT_USER_DN = "dn";

    private final InjectedValue<ConnectionManager> connectionManager = new InjectedValue<ConnectionManager>();

    private final String baseDn;
    private final String usernameAttribute;
    private final String advancedFilter;
    private final boolean recursive;
    private final String userDn;
    protected final int searchTimeLimit = 10000; // TODO - Maybe make configurable.

    public UserLdapCallbackHandler(String baseDn, String userNameAttribute, String advancedFilter, boolean recursive, String userDn) {
        this.baseDn = baseDn;
        if (userNameAttribute == null && advancedFilter == null) {
            throw MESSAGES.oneOfRequired(USERNAME_ATTRIBUTE, ADVANCED_FILTER);
        }
        this.usernameAttribute = userNameAttribute;
        this.advancedFilter = advancedFilter;
        this.recursive = recursive;
        this.userDn = userDn;
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthenticationMechanism getPreferredMechanism() {
        return AuthenticationMechanism.PLAIN;
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
        if (callbacks.length == 1 && callbacks[0] instanceof AuthorizeCallback) {
            AuthorizeCallback acb = (AuthorizeCallback) callbacks[0];
            String authenticationId = acb.getAuthenticationID();
            String authorizationId = acb.getAuthorizationID();

            acb.setAuthorized(authenticationId.equals(authorizationId));

            return;
        }

        ConnectionManager connectionManager = this.connectionManager.getValue();
        String username = null;
        VerifyPasswordCallback verifyPasswordCallback = null;

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
            throw MESSAGES.noUsername();
        }
        if (verifyPasswordCallback == null) {
            throw MESSAGES.noPassword();
        }

        InitialDirContext searchContext = null;
        InitialDirContext userContext = null;
        NamingEnumeration<SearchResult> searchEnumeration = null;
        try {
            // 1 - Obtain Connection to LDAP
            searchContext = (InitialDirContext) connectionManager.getConnection();
            // 2 - Search to identify the DN of the user connecting
            SearchControls searchControls = new SearchControls();
            if (recursive) {
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else {
                searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            }
            searchControls.setReturningAttributes(new String[]{userDn});
            searchControls.setTimeLimit(searchTimeLimit);

            Object[] filterArguments = new Object[]{username};
            String filter = usernameAttribute != null ? "(" + usernameAttribute + "={0})" : advancedFilter;

            searchEnumeration = searchContext.search(baseDn, filter, filterArguments, searchControls);
            if (searchEnumeration.hasMore() == false) {
                throw MESSAGES.userNotFoundInDirectory(username);
            }

            String distinguishedUserDN = null;

            SearchResult result = searchEnumeration.next();
            Attributes attributes = result.getAttributes();
            if (attributes != null) {
                Attribute dn = attributes.get(userDn);
                if (dn != null) {
                    distinguishedUserDN = (String) dn.get();
                }
            }
            if (distinguishedUserDN == null) {
                if (result.isRelative() == true)
                    distinguishedUserDN = result.getName() + ("".equals(baseDn) ? "" : "," + baseDn);
                else
                    throw MESSAGES.nameNotFound(result.getName());
            }

            // 3 - Connect as user once their DN is identified
            userContext = (InitialDirContext) connectionManager.getConnection(distinguishedUserDN, verifyPasswordCallback.getPassword());
            if (userContext != null) {
                verifyPasswordCallback.setVerified(true);
            }

        } catch (Exception e) {
            throw MESSAGES.cannotPerformVerification(e);
        } finally {
            safeClose(searchEnumeration);
            safeClose(searchContext);
            safeClose(userContext);
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

}
