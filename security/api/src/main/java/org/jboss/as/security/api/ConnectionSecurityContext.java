/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.api;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

import javax.security.auth.Subject;

import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.security.remoting.RemotingContext;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.security.UserInfo;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;

/**
 * Utility class to allow inspection and replacement of identity associated with the Connection.
 *
 * As a connection is established to the application server the remote user is authenticated, this API allows the
 * {@link Collection} of {@link Principal}s for the remote user to be obtained, the API then allows for an alternative identity
 * to be pushed by interceptors for validation in the security interceptors for subsequent EJB invocations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConnectionSecurityContext {

    private ConnectionSecurityContext() {
    }

    /**
     * Obtain a {@link Collection} containing the {@link Principal} instances for the user associated with the connection.
     *
     * Note: This method should be called from within a {@link PrivilegedAction}.
     *
     * @return The Collection of Principals for the user authenticated with the connection. An empty Collection will be returned
     *         of no user is associated with the connection, {@code null} will be returned if no connection is associated with
     *         the {@link Thread}
     */
    public static Collection<Principal> getConnectionPrincipals() {
        Connection con = RemotingContext.getConnection();

        if (con != null) {
            UserInfo userInfo = con.getUserInfo();
            if (userInfo instanceof SubjectUserInfo) {
                SubjectUserInfo sinfo = (SubjectUserInfo) userInfo;
                return sinfo.getPrincipals();
            } else {
                return Collections.emptySet();
            }
        }

        return null;
    }

    /**
     * Push a new {@link Principal} and Credential pair.
     *
     * This method is to be called before an EJB invocation is passed through it's security interceptor, at that point the
     * Principal and Credential pair can be verified.
     *
     * Note: This method should be called from within a {@link PrivilegedAction}.
     *
     * @param principal - The alternative {@link Principal} to use in verification before the next EJB is called.
     * @param credential - The credential to verify with the {@linl Principal}
     * @return A {@link ContextStateCache} that can later be used to pop the identity pushed here and restore internal state to it's previous values.
     * @throws Exception If there is a problem associating the new {@link Principal} and Credential pair.
     */
    public static ContextStateCache pushIdentity(final Principal principal, final Object credential) throws Exception {
        SecurityContext current = SecurityContextAssociation.getSecurityContext();

        SecurityContext nextContext = SecurityContextFactory.createSecurityContext(principal, credential, new Subject(), "USER_DELEGATION");
        SecurityContextAssociation.setSecurityContext(nextContext);

        Connection con = RemotingContext.getConnection();
        RemotingContext.clear();

        return new ContextStateCache(con, current);
    }

    /**
     * Pop the identity previously associated and restore internal state to it's previous value.
     *
     * @param stateCache - The cache containing the state as it was when pushIdentity was called.
     */
    public static void popIdentity(final ContextStateCache stateCache) {
        RemotingContext.setConnection(stateCache.getConnection());
        SecurityContextAssociation.setSecurityContext(stateCache.getSecurityContext());
    }

}
