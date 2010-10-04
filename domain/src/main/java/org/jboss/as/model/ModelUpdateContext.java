/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model;

import org.jboss.as.deployment.client.api.server.AbstractServerUpdateActionResult;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;

/**
 * Context in which the execution of {@link AbstractModelUpdate model updates}
 * is occuring.
 *
 * @author Brian Stansberry
 */
public interface ModelUpdateContext {

    /** The status of the overall model update */
    enum Status {
        /** The update is active and updates can be applied */
        ACTIVE,
        /** An error has occurred with one or more updates */
        MARKED_ROLLBACK,
        /** Updates are being rolled back */
        ROLLING_BACK,
        /**
         * All updates have completed or been rolled back; the
         * overall update process is finishing.
         */
        COMMITING,
        /**
         * The overall update process has finished following a rollback of
         * attempted updates.
         */
        ROLLED_BACK,
        COMMITTED
    }

    /**
     * Gets the current status of the overall update.
     * @return the status. Will not be <code>null</code>
     */
    Status getStatus();

    /**
     * Gets whether the overall update process supports rollback of failed
     * updates.
     *
     * @return <code>true</code> if rollbacks are allowed
     */
    boolean isRollbackAllowed();

    /**
     * Gets whether the overall update process allows changes to the server's
     * running services.
     * @return
     */
    boolean isRuntimeUpdateAllowed();

    /**
     * Get the current batch builder.
     *
     * @return the current batch builder or {@code null} if there is no active builder
     */
    BatchBuilder getBatchBuilder();

    /**
     * Get the service container.
     *
     * @return the service container
     */
    ServiceContainer getServiceContainer();

    /**
     * Records the result of an individual update action.
     *
     * @param result the result. Cannot be <code>null</code>
     */
    void recordUpdateResult(AbstractServerUpdateActionResult<?> result);

    /**
     * Records the result of rolling back an individual update action.
     *
     * @param result the result. Cannot be <code>null</code>
     */
    void recordRollbackResult(AbstractServerUpdateActionResult<?> result);
}
