/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.clustering.web.sso;

/**
 * Interface implemented by the ClusteredSingleSignOn valve to allow callbacks by the {@link SSOClusterManager}.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public interface SSOLocalManager {
    /**
     * Callback from the SSOClusterManager notifying the local manager that an SSO has been deregistered on another node.
     *
     * @param ssoId Single sign on identifier to deregister
     */
    void deregister(String ssoId);

    /**
     * Callback from the SSOClusterManager notifying the local manager that an SSO has been deregistered on another node,
     * but that invalidation operations should not be propagated to remote nodes.
     *
     * @param ssoId Single sign on identifier to deregister
     */
    void deregisterLocal(String ssoId);

    /**
     * Callback from the SSOClusterManager notifying the local manager that the credentials associated with an SSO have been
     * modified on another node.
     *
     * @param ssoId the id of the SSO
     * @param credentials the updated credentials
     */
    void remoteUpdate(String ssoId, SSOCredentials credentials);

    /**
     * Callback from the SSOClusterManager when it detects an SSO without any active sessions across the cluster
     */
    void notifySSOEmpty(String ssoId);

    /**
     * Callback from the SSOClusterManager when it detects an SSO that has active sessions across the cluster
     */
    void notifySSONotEmpty(String ssoId);
}