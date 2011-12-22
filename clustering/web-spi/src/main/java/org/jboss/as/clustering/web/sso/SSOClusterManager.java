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

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * Provides communications support between a SingleSignOn valve and other such valves configured for the same hostname within a
 * server cluster.
 * <p/>
 * Implementations of this interface must declare a public no-arguments constructor.
 *
 * @author Brian E. Stansberry
 * @version $Revision: 45726 $ $Date: 2006-06-21 21:50:00 +0200 (mer., 21 juin 2006) $
 * @see SSOLocalManager
 */
public interface SSOClusterManager {
    /**
     * Notify the cluster of the addition of a Session to an SSO session.
     *
     * @param ssoId the id of the SSO session
     * @param sessionId id of the Session that has been added
     */
    void addSession(String ssoId, FullyQualifiedSessionId sessionId);

    /**
     * Gets the SSOLocalManager valve for which this object is handling cluster communications.
     *
     * @return the <code>SSOLocalManager</code> valve.
     */
    SSOLocalManager getSSOLocalManager();

    /**
     * Sets the SSOLocalManager valve for which this object is handling cluster communications.
     * <p>
     * <b>NOTE:</b> This method must be called before calls can be made to the other methods of this interface.
     *
     * @param localManager a <code>SSOLocalManager</code> valve.
     */
    void setSSOLocalManager(SSOLocalManager localManager);

    /**
     * Notifies the cluster that a single sign on session has been terminated due to a user logout.
     *
     * @param ssoId the id of the SSO session
     */
    void logout(String ssoId);

    /**
     * Queries the cluster for the existence of a SSO session with the given id, returning a <code>SSOCredentials</code> if one
     * is found.
     *
     * @param ssoId the id of the SSO session
     * @return a <code>SSOCredentials</code> created using information found on another cluster node, or <code>null</code> if no
     *         entry could be found.
     */
    SSOCredentials lookup(String ssoId);

    /**
     * Notifies the cluster of the creation of a new SSO entry.
     *
     * @param ssoId the id of the SSO session
     * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST or FORM) used to authenticate the SSO.
     * @param username the username (if any) used for the authentication
     * @param password the password (if any) used for the authentication
     */
    void register(String ssoId, String authType, String username, String password);

    /**
     * Notify the cluster of the removal of a Session from an SSO session.
     *
     * @param ssoId the id of the SSO session
     * @param sessionId id of the Session that has been removed
     */
    void removeSession(String ssoId, FullyQualifiedSessionId sessionId);

    /**
     * Notifies the cluster of an update of the security credentials associated with an SSO session.
     *
     * @param ssoId the id of the SSO session
     * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST or FORM) used to authenticate the SSO.
     * @param username the username (if any) used for the authentication
     * @param password the password (if any) used for the authentication
     */
    void updateCredentials(String ssoId, String authType, String username, String password);

    /**
     * Prepare for the beginning of active use of the public methods of this component. This method should be called before any
     * of the public methods of this component are utilized.
     *
     * @exception Exception if this component detects a fatal error that prevents this component from being used
     */
    void start() throws Exception;

    /**
     * Gracefully terminate the active use of the public methods of this component. This method should be the last one called on
     * a given instance of this component.
     *
     * @exception Exception if this component detects a fatal error that needs to be reported
     */
    void stop() throws Exception;

    void setCacheContainerName(String name);

    void setCacheName(String name);

    void addDependencies(ServiceTarget target, ServiceBuilder<?> builder);
}
