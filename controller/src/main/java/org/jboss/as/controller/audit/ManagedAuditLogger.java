/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.controller.audit;


import org.jboss.as.controller.PathAddress;

/**
 * Abstract base class for {@link AuditLogger} implementations.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public interface ManagedAuditLogger extends AuditLogger {

    /**
     * Get whether this audit logger logs read-only operations
     *
     * @return whether this audit logger logs read-only operations
     */
    boolean isLogReadOnly();

    /**
     * Set whether to log read-only operations
     *
     * @param logReadOnly wheter to log read-only operations
     */
    void setLogReadOnly(boolean logReadOnly);

    /**
     * Get whether this audit logger logs operations on boot
     *
     * @return whether this audit logger logs operations on boot
     */
    boolean isLogBoot();

    /**
     * Set whether to log operations on boot
     *
     * @param logBoot whether to log operations on boot
     */
    void setLogBoot(boolean logBoot);

    /**
     * Gets the status of the audit logger
     *
     * @return the status
     */
    Status getLoggerStatus();

    /**
     * Sets the status of the audit logger
     *
     * @param  newStatus the status
     */
    void setLoggerStatus(Status newStatus);

    /**
     * Gets the handler updater used to schedule updates to the handlers
     *
     * @return the handler updater
     */
    AuditLogHandlerUpdater getUpdater();

    /**
     * Recycles a handler. This stops it, and resets the failure count so that the next time it is used
     * it will reinitialize.
     *
     * @param name the name of the handler
     */
    void recycleHandler(String name);

    /**
     * Create another audit logger configuration, e.g. for JMX which has its own global config of the audit logger
     *
     * @param manualCommit if {@code true} the caller is responsible for applying the changes themselves, if {@code false} the changes will be committed after the next log records has been written
     * @return the new configuration
     */
    ManagedAuditLogger createNewConfiguration(boolean manualCommit);

    /**
     * Add a formatter
     *
     * @param formatter the formatter
     */
    void addFormatter(AuditLogItemFormatter formatter);

    /**
     * Update the handler formatter. This will take effect immediately.
     *
     * @param name the name of the handler
     * @param formatterName the name of the formatter
     */
    void updateHandlerFormatter(String name, String formatterName);

    /**
     * Update the handler max failure count. This will take effect immediately
     *
     * @param name the name of the handler
     * @param count the max failure count
     */
    void updateHandlerMaxFailureCount(String name, int count);

    /**
     * Remove a formatter
     *
     * @param name the formatter name
     */
    void removeFormatter(String name);

    /**
     * Get the current failure count of a handler
     *
     * @param name the name of the handler
     * @return the failure count
     */
    int getHandlerFailureCount(String name);

    /**
     * Get whether a handler was disabled due to failures
     *
     * @param name the name of the handler
     * @return whether it is disabled
     */
    boolean getHandlerDisabledDueToFailure(String name);

    /**
     * Gets a formatter by its name
     *
     * @param name the name of the formatter
     * @return the formatter
     */
    JsonAuditLogItemFormatter getJsonFormatter(String name);

    /**
     * Callback for the controller to call before the controller is booted
     */
    void startBoot();

    /**
     * Callback for the controller to call when the controller has been booted
     */
    void bootDone();

    /**
     * <p>The audit log handler updater. Additive changes will be used for the audit log record as a result of
     * management operations causing updates here. Removals and updates will not take effect until the current audit log
     * record has been written.
     * </p>
     * <p>
     * This means that if a new handler is added and a reference is added, the new handler will be used to log the
     * operations causing that to happen.
     * </p>
     * <p>
     * If a handler is removed, the operations causing the removal will be logged to the handler, before removing it.
     * </p>
     * <p>
     * If an handler is changed, for example to change the location of a file handler or the protocol of a syslog
     * handler, the operations causing the change will be logged to the current handler location. The next incoming
     * log message will be written to the new handler location.
     * </p>
     */
    interface AuditLogHandlerUpdater {
        /**
         * Adds a new handler, this handler will be used when logging the current operation
         *
         * @param handler the handler
         */
        void addHandler(AuditLogHandler handler);

        /**
         * Update an handler. The update will only take place if the handler has actually been changed. The
         * changes to the handler will only take effect after the current operation has been logged.
         *
         * @param handler the updated handler
         */
        void updateHandler(AuditLogHandler handler);

        /**
         * Remove an handler. The removal will only take effect after the current operation has been logged.
         *
         * @param name the name of the handler to be removed
         * @throws IllegalStateException if the handler still has references to it
         */
        void removeHandler(String name);

        /**
         * Add an handler reference. This reference will take effect when logging the current operation
         *
         * @param referenceAddress the address of the handler reference (the value of the last element is the name of
         *                         the referenced handler)
         */
        void addHandlerReference(PathAddress referenceAddress);

        /**
         * Add an handler reference. This reference removal will only take effect after the current operation has been
         * logged.
         *
         * @param referenceAddress the address of the handler reference (the value of the last element is the name of
         *                         the referenced handler)
         */
        void removeHandlerReference(PathAddress referenceAddress);

        /**
         * Roll back changes made as part of the current operation.
         */
        void rollbackChanges();

        /**
         * Apply the changes. This is only allowed for update tasks for new audit log configurations which specify that manual commit
         * should be used.
         *
         * @throws IllegalStateException if manual commit should not be used
         */
        void applyChanges();

    }
}
