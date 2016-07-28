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

/**
 * Represents a web session.
 * @author Paul Ferraro
 */
public interface Session<L> extends ImmutableSession, AutoCloseable {
    /**
     * {@inheritDoc}
     */
    @Override
    SessionMetaData getMetaData();

    /**
     * Indicates whether or not this session is valid.
     * @return true, if this session is valid, false otherwise
     */
    boolean isValid();

    /**
     * Invalidates this session.
     * @throws IllegalStateException if this session was already invalidated.
     */
    void invalidate();

    /**
     * {@inheritDoc}
     */
    @Override
    SessionAttributes getAttributes();

    /**
     * Indicates that the application thread is finished with this session.
     * This method is intended to be invoked within the context of a batch.
     */
    @Override
    void close();

    /**
     * Returns the local context of this session.
     * The local context is *not* replicated to other nodes in the cluster.
     * @return a local context
     */
    L getLocalContext();
}
