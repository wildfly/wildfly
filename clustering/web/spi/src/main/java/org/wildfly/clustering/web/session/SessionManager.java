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

import java.time.Duration;
import java.util.Set;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.IdentifierFactory;

public interface SessionManager<L, B extends Batch> extends IdentifierFactory<String>, ActiveSessionStatistics {

    /**
     * Returns the session with the specified identifier, or null if none exists.
     * Sessions returned by this method must be closed via {@link Session#close()}.
     * This method is intended to be invoked within the context of a batch.
     * @param id a session identifier
     * @return an existing web session, or null if none exists
     */
    Session<L> findSession(String id);

    /**
     * Creates a session using the specified identifier.
     * Sessions returned by this method must be closed via {@link Session#close()}.
     * This method is intended to be invoked within the context of a batch.
     * @param id a session identifier
     * @return a new web session, or null if a session with the specified identifier already exists.
     */
    Session<L> createSession(String id);

    /**
     * Returns the default maximum inactive interval, as a duration, for all sessions created by this session manager.
     * @return a duration
     */
    Duration getDefaultMaxInactiveInterval();

    /**
     * Set the default maximum inactive interval, using the specified time duration, for all sessions created by this session manager.
     * @param duration a time duration
     */
    void setDefaultMaxInactiveInterval(Duration duration);

    /**
     * Exposes the batching mechanism used by this session manager.
     * @return a batcher.
     */
    Batcher<B> getBatcher();

    /**
     * Returns the identifiers of those sessions that are active on this node.
     * @return a set of session identifiers.
     */
    Set<String> getActiveSessions();

    /**
     * Returns the identifiers of all sessions on this node, including both active and passive sessions.
     * @return a set of session identifiers.
     */
    Set<String> getLocalSessions();

    /**
     * Returns a read-only view of the session with the specified identifier.
     * This method is intended to be invoked within the context of a batch
     * @param id a unique session identifier
     * @return a read-only session or null if none exists
     */
    ImmutableSession viewSession(String id);
}
