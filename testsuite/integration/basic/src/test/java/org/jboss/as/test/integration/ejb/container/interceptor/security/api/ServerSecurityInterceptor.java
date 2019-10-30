/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor.security.api;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

import javax.ejb.EJBAccessException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.resource.spi.IllegalStateException;

import org.jboss.as.core.security.api.UserPrincipal;
import org.jboss.as.security.api.ConnectionSecurityContext;
import org.jboss.as.security.api.ContextStateCache;
import org.jboss.as.test.integration.ejb.container.interceptor.security.CurrentUserCredential;
import org.jboss.logging.Logger;
import org.jboss.security.SimplePrincipal;

/**
 * The server side security interceptor responsible for handling any security identity propagated from the client.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Josef Cacek
 */
public class ServerSecurityInterceptor {

    private static final Logger LOGGER = Logger.getLogger(ServerSecurityInterceptor.class);

    static final String DELEGATED_USER_KEY = ServerSecurityInterceptor.class.getName() + ".DelegationUser";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        Principal desiredUser = null;
        UserPrincipal connectionUser = null;

        Map<String, Object> contextData = invocationContext.getContextData();
        if (contextData.containsKey(DELEGATED_USER_KEY)) {
            desiredUser = new SimplePrincipal((String) contextData.get(DELEGATED_USER_KEY));

            Collection<Principal> principals = ConnectionSecurityContext.getConnectionPrincipals();

            if (principals != null) {
                for (Principal current : principals) {
                    if (current instanceof UserPrincipal) {
                        connectionUser = (UserPrincipal) current;
                        break;
                    }
                }
            } else {
                throw new IllegalStateException("Delegation user requested but no user on connection found.");
            }
        }


        ContextStateCache stateCache = null;

        try {
            if (desiredUser != null && connectionUser != null
                    && (desiredUser.getName().equals(connectionUser.getName()) == false)) {
                try {
                    // The final part of this check is to verify that the change does actually indicate a change in user.

                    // We have been requested to switch user and have successfully identified the user from the connection
                    // so now we attempt the switch.
                     stateCache = ConnectionSecurityContext.pushIdentity(desiredUser, new CurrentUserCredential(connectionUser.getName()));
                } catch (Exception e) {
                    LOGGER.error("Failed to switch security context for user", e);
                    // Don't propagate the exception stacktrace back to the client for security reasons
                    throw new EJBAccessException("Unable to attempt switching of user.");
                }
            }

            return invocationContext.proceed();
        } finally {
            // switch back to original security context
            if (stateCache != null) {
                ConnectionSecurityContext.popIdentity(stateCache);
            }
        }
    }

}
