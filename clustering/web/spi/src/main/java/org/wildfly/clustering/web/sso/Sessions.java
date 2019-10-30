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

import java.util.Set;

/**
 * Represents the sessions per deployment for which a given user is authenticated.
 * @author Paul Ferraro
 */
public interface Sessions<D, S> {
    /**
     * Returns the set of web applications for which the current user is authenticated.
     * @return a set of web applications.
     */
    Set<D> getDeployments();

    /**
     * Returns the corresponding session identifier for the specified web application.
     * @param application
     * @return
     */
    S getSession(D deployment);

    /**
     * Removes the specified web application from the set of authenticated web applications.
     * @param application
     */
    S removeSession(D deployment);

    /**
     * Adds the specified web application and session identifier to the registry of authenticated web applications.
     * @param deployment a web application
     * @param session a session
     * @return true, if the session was added, false it already exists
     */
    boolean addSession(D deployment, S session);
}
