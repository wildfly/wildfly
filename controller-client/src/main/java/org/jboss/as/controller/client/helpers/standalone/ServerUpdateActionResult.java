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

package org.jboss.as.controller.client.helpers.standalone;

import java.util.UUID;

/**
 * Encapsulates the results of performing a configuration modification on an individual
 * server.
 *
 * @author Brian Stansberry
 */
public interface ServerUpdateActionResult {

    public enum Result {
        /**
         * Indicates the action was not executed because some problem with
         * the overall update plan halted plan execution before this
         * action was attempted.
         */
        NOT_EXECUTED,
        /**
         * The action was successfully executed; server configuration was
         * modified and any deployment changes were initiated
         * in a running server.
         */
        EXECUTED,
        /**
         * Indicates a successful outcome for an action that is part of an
         * update plan that is organized around a server restart. The
         * server configuration was successfully modified but the ability to
         * effect changes in a running server is unknown, as completing the action
         * requires restarting the server.
         */
        CONFIGURATION_MODIFIED_REQUIRES_RESTART,
        /**
         * The action failed to complete successfully. See
         * {@link ServerUpdateActionResult#getDeploymentException()} for possible details.
         */
        FAILED,
        /**
         * The action completed successfully, but was rolled back due to a
         * problem with some other action in the overall deployment plan.
         */
        ROLLED_BACK;
    }

    /**
     * Gets the unique ID of the deployment action.
     *
     * @return the ID. Will not be <code>null</code>
     */
    UUID getUpdateActionId();

    /**
     * Gets the result of the action's modification to the server's configuration.
     * @return the result. Will not be <code>null</code>
     */
    Result getResult();

    /**
     * Gets the exception, if any, that occurred while executing this action.
     *
     * @return the exception, or <code>null</code> if no exception occurred
     */
    Throwable getDeploymentException();

    /**
     * If the {@link #getResult() result was {@link Result#ROLLED_BACK} or
     * {@link Result#FAILED} and the update plan allows rollback,
     * gets the result of rolling back the action.
     *
     * @return the result of rolling back the action, or {@code null} if the
     *          action was not rolled back.
     */
    ServerUpdateActionResult getRollbackResult();

}
