/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.helpers.domain;

import java.util.UUID;

import org.jboss.dmr.ModelNode;

/**
 * Encapsulates the results of performing a configuration modification on an individual
 * server.
 *
 * @author Brian Stansberry
 */
public interface ServerUpdateResult {

    /**
     * Gets the unique ID of the deployment action.
     *
     * @return the ID. Will not be <code>null</code>
     */
    UUID getUpdateActionId();

    /**
     * Gets the id of the server on which this update was executed.
     *
     * @return the server identity. Will not be <code>null</code>
     */
    ServerIdentity getServerIdentity();

    /**
     * Gets the result of the action's modification to the server's configuration.
     * This will always be {@code null} if {@link #isServerRestarted()} is <code>true</code>.
     *
     * @return the result. May be <code>null</code>
     */
    ModelNode getSuccessResult();

    /**
     * Gets the exception, if any, that occurred while executing this update.
     *
     * @return the exception, or <code>null</code> if no exception occurred
     */
    Throwable getFailureResult();

    /**
     * Gets whether the application of this action on this server was
     * cancelled.
     *
     * @return <code>true</code> if the action was cancelled; <code>false</code>
     *         otherwise
     */
    boolean isCancelled();

    /**
     * Gets whether the application of this action on this server timed out.
     *
     * @return <code>true</code> if the action timed out; <code>false</code>
     *         otherwise
     */
    boolean isTimedOut();

    /**
     * Gets whether the application of this action on this server was
     * rolled back.
     *
     * @return <code>true</code> if the action was rolled back; <code>false</code>
     *         otherwise
     */
    boolean isRolledBack();

    /**
     * Gets any failure that occurred when rolling back this action on this
     * server.
     *
     * @return the exception, or <code>null</code> if no exception occurred
     */
    Throwable getRollbackFailure();

    /**
     * Gets whether the application of the update to the server's runtime
     * required a server restart.
     *
     * @return <code>true</code> if the server was restarted; <code>false</code> otherwise
     */
    boolean isServerRestarted();

}
