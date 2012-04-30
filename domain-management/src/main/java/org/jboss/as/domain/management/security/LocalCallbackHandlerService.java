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

import static org.jboss.as.domain.management.RealmConfigurationConstants.LOCAL_DEFAULT_USER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.CallbackHandlerServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The Service providing the LocalCallbackHandler implementation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class LocalCallbackHandlerService implements Service<CallbackHandlerService>, CallbackHandlerService, CallbackHandler {

    public static final String SERVICE_SUFFIX = "local";

    private final String defaultUser;
    private final String allowedUsers;
    private boolean allowAll;
    private final Set<String> allowedUsersSet = new HashSet<String>();
    private final CallbackHandlerServiceRegistry registry;

    LocalCallbackHandlerService(final String defaultUser, final String allowedUsers, final CallbackHandlerServiceRegistry registry) {
        this.defaultUser = defaultUser;
        this.allowedUsers = allowedUsers;
        this.registry = registry;
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthenticationMechanism getPreferredMechanism() {
        return AuthenticationMechanism.LOCAL;
    }

    public Set<AuthenticationMechanism> getSupplementaryMechanisms() {
        return Collections.emptySet();
    }

    public Map<String, String> getConfigurationOptions() {
        if (defaultUser != null) {
            return Collections.singletonMap(LOCAL_DEFAULT_USER, defaultUser);
        } else {
            return Collections.emptyMap();
        }
    }

    public boolean isReady() {
        return true;
    }

    public CallbackHandler getCallbackHandler(Map<String, Object> sharedState) {
        return this;
    }

    /*
     * Service Methods
     */


    public CallbackHandlerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
        if (defaultUser != null) {
            allowedUsersSet.add(defaultUser);
        }
        if (allowedUsers != null) {
            if ("*".equals(allowedUsers)) {
                allowAll = true;
            } else {
                String[] users = allowedUsers.split(",");
                for (String current : users) {
                    allowedUsersSet.add(current);
                }
            }
        }
        registry.register(getPreferredMechanism(), this);
    }

    public void stop(StopContext context) {
        registry.unregister(getPreferredMechanism(), this);
        allowAll = false;
        allowedUsersSet.clear(); // Effectively disables this CBH
    }

    /*
     * CallbackHandler Method
     */

    /**
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback current : callbacks) {
            if (current instanceof NameCallback) {
                NameCallback ncb = (NameCallback) current;
                String userName = ncb.getDefaultName();
                if ((allowAll || allowedUsersSet.contains(userName)) == false) {
                    throw MESSAGES.invalidLocalUser(userName);
                }
            } else if (current instanceof AuthorizeCallback) {
                AuthorizeCallback acb = (AuthorizeCallback) current;
                acb.setAuthorized(acb.getAuthenticationID().equals(acb.getAuthorizationID()));
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }
    }

}
