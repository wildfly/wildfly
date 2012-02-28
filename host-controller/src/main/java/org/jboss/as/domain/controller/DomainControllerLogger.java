/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import java.util.Set;
import javax.xml.stream.Location;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 10800-10999. This file is using the subset 10800-10829 for domain
 * controller logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 * <p/>
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface DomainControllerLogger extends BasicLogger {

    /**
     * A logger with the category of the package.
     */
    DomainControllerLogger ROOT_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, DomainControllerLogger.class.getPackage().getName());

    /**
     * A logger with the category of {@code org.jboss.as.controller}.
     */
    DomainControllerLogger CONTROLLER_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.controller");

    /**
     * A logger with the category of {@code org.jboss.as.deployment}.
     */
    DomainControllerLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.deployment");

    /**
     * A logger with the category of {@code org.jboss.as.domain.deployment}.
     */
    DomainControllerLogger DOMAIN_DEPLOYMENT_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.domain.deployment");

    /**
     * A logger with the category of {@code org.jboss.as.host.controller}.
     */
    DomainControllerLogger HOST_CONTROLLER_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.host.controller");

    @LogMessage(level = Level.WARN)
    @Message(id = 10800, value = "Ignoring 'include' child of 'socket-binding-group' %s")
    void warnIgnoringSocketBindingGroupInclude(Location location);

    @LogMessage(level = Level.WARN)
    @Message(id = 10801, value = "Ignoring 'include' child of 'profile' %s")
    void warnIgnoringProfileInclude(Location location);

    /**
     * Logs a warning message indicating an interruption awaiting the final response from the server, represented by the
     * {@code serverName} parameter, on the host, represented by the {@code hostName} parameter.
     *
     * @param serverName the name of the server.
     * @param hostName   the name of the host.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10802, value = "Interrupted awaiting final response from server %s on host %s")
    void interruptedAwaitingFinalResponse(String serverName, String hostName);

    /**
     * Logs a warning message indicating an exception was caught awaiting the final response from the server,
     * represented by the {@code serverName} parameter, on the host, represented by the {@code hostName} parameter.
     *
     * @param cause      the cause of the error.
     * @param serverName the name of the server.
     * @param hostName   the name of the host.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10803, value = "Caught exception awaiting final response from server %s on host %s")
    void caughtExceptionAwaitingFinalResponse(@Cause Throwable cause, String serverName, String hostName);

    /**
     * Logs a warning message indicating an interruption awaiting the final response from the host, represented by the
     * {@code hostName} parameter.
     *
     * @param hostName the name of the host.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10804, value = "Interrupted awaiting final response from host %s")
    void interruptedAwaitingFinalResponse(String hostName);

    /**
     * Logs a warning message indicating an exception was caught awaiting the final response from the host, represented
     * by the {@code hostName} parameter.
     *
     * @param cause    the cause of the error.
     * @param hostName the name of the host.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10805, value = "Caught exception awaiting final response from host %s")
    void caughtExceptionAwaitingFinalResponse(@Cause Throwable cause, String hostName);

    /**
     * Logs a warning message indicating an exception was caught closing the input stream.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 10806, value = "Caught exception closing input stream")
    void caughtExceptionClosingInputStream(@Cause Throwable cause);

    /**
     * Logs a warning message indicating the domain model has changed on re-connect and the servers need to be restarted
     * for the changes to take affect.
     *
     * @param servers the servers that need to restart.
     */
    @LogMessage(level = Level.INFO)
    @Message(id = 10807, value = "Domain model has changed on re-connect. The following servers will need to be restarted for changes to take affect: %s")
    void domainModelChangedOnReConnect(Set<ServerIdentity> servers);

    /**
     * Logs an error message indicating the class, represented by the {@code className} parameter, caught an exception
     * waiting for the task.
     *
     * @param className     the class name.
     * @param exceptionName the name of the exception caught.
     * @param task          the task.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10808, value = "%s caught %s waiting for task %s")
    void caughtExceptionWaitingForTask(String className, String exceptionName, String task);

    /**
     * Logs an error message indicating the class, represented by the {@code className} parameter, caught an exception
     * waiting for the task and is returning.
     *
     * @param className     the class name.
     * @param exceptionName the name of the exception caught.
     * @param task          the task.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10809, value = "%s caught %s waiting for task %s; returning")
    void caughtExceptionWaitingForTaskReturning(String className, String exceptionName, String task);

    /**
     * Logs an error message indicating the content for a configured deployment was unavailable at boot but boot
     * was allowed to proceed because the HC is in admin-only mode.
     *
     * @param contentHash    the content hash that could not be found.
     * @param deploymentName the deployment name.
     */
    @LogMessage(level = Level.ERROR)
    @Message(id = 10810, value = "No deployment content with hash %s is available in the deployment content repository for deployment %s. Because this Host Controller is booting in ADMIN-ONLY mode, boot will be allowed to proceed to provide administrators an opportunity to correct this problem. If this Host Controller were not in ADMIN-ONLY mode this would be a fatal boot failure.")
    void reportAdminOnlyMissingDeploymentContent(String contentHash, String deploymentName);
}
