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
package org.wildfly.clustering.web.session;

import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.Batcher;

public interface SessionManager<L> extends SessionIdentifierFactory, RouteLocator {
    /**
     * Invoked prior to applicaion deployment.
     */
    void start();

    /**
     * Invoked prior to application undeployment.
     */
    void stop();

    boolean containsSession(String id);

    /**
     * Returns the session with the specified identifier, or null if none exists
     * @param id a session identifier
     * @return an existing web session, or null if none exists
     */
    Session<L> findSession(String id);

    /**
     * Returns the session with the specified identifier, creating one if necessary
     * @param id a session identifier
     * @return a new or existing web session
     */
    Session<L> createSession(String id);

    /**
     * Returns the estimated number of session currently being handled by this session manager.
     * @return
     */
    int size();

    /**
     * Returns the default maximum inactive interval, using the specified unit, for all sessions created by this session manager.
     * @param unit a time unit
     * @return a time interval
     */
    long getDefaultMaxInactiveInterval(TimeUnit unit);

    /**
     * Set the default maximum inactive interval, using the specified unit, for all sessions created by this session manager.
     * @return value a time interval
     * @param unit a time unit
     */
    void setDefaultMaxInactiveInterval(long value, TimeUnit unit);

    /**
     * Exposes the batching mechanism used by this session manager.
     * @return a batcher.
     */
    Batcher getBatcher();
}
