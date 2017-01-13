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
package org.wildfly.clustering.web.sso;

/**
 * Represents a single sign on entry for a user.
 * @author Paul Ferraro
 * @param the authentication identity type
 * @param the deployment identifier type
 * @param the local context type
 */
public interface SSO<A, D, S, L> {
    /**
     * A unique identifier for this SSO.
     * @return a unique identifier
     */
    String getId();

    /**
     * Returns the authentication for this SSO.
     * @return an authentication.
     */
    A getAuthentication();

    /**
     * Returns the session for which the user is authenticated.
     * @return
     */
    Sessions<D, S> getSessions();

    /**
     * Invalidates this SSO.
     */
    void invalidate();

    /**
     * The local context of this SSO.
     * The local context is *not* replicated to other nodes in the cluster.
     * @return a local context.
     */
    L getLocalContext();
}
