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
package org.wildfly.clustering.ee;

/**
 * Exposes a mechanism to close a batch, and potentially discard it.
 * @author Paul Ferraro
 */
public interface Batch extends AutoCloseable {

    /**
     * The possible states of a batch.
     */
    enum State {
        /**
         * The initial state of a batch.
         * A batch remains active until it is discarded or closed.
         */
        ACTIVE,
        /**
         * Indicates that an active batch was discarded, but not yet closed.
         * An active batch moves to this state following {@link Batch#discard()}.
         */
        DISCARDED,
        /**
         * The terminal state of a batch.
         * A batch moves to this state following {@link Batch#close()}.
         */
        CLOSED
    }

    /**
     * Closes this batch.  Batch may or may not have been discarded.
     */
    @Override
    void close();

    /**
     * Discards this batch.  A discarded batch must still be closed.
     */
    void discard();

    /**
     * Returns the state of this batch.
     * @return the state of this batch.
     */
    State getState();
}
