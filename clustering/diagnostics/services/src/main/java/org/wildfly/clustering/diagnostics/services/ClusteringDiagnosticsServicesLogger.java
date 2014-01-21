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

package org.wildfly.clustering.diagnostics.services;

import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * ClusteringMinitorServicesLogger
 *
 * logging id range: 20800 - 20849
 *
 * @author Richard Achmatowicz (c) Red Hat Inc 2013
 */
@MessageLogger(projectCode = "JBAS")
public interface ClusteringDiagnosticsServicesLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = ClusteringDiagnosticsServicesLogger.class.getPackage().getName();

    /**
     * A logger with the category of the default clustering package.
     */
    ClusteringDiagnosticsServicesLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringDiagnosticsServicesLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = TRACE)
    @Message(id = 20800, value = "Starting CacheManagementControllerService: container name = %s")
    void startingCacheManagementControllerService(String containerName);

    @LogMessage(level = TRACE)
    @Message(id = 20801, value = "Stopping CacheManagementControllerService: container name = %s")
    void stoppingCacheManagementControllerService(String containerName);

    @LogMessage(level = TRACE)
    @Message(id = 20802, value = "Starting CacheManagementService: container name = %s")
    void startingCacheManagementService(String containerName);

    @LogMessage(level = TRACE)
    @Message(id = 20803, value = "Stopping CacheManagementService: container name = %s")
    void stoppingCacheManagementService(String containerName);

    @LogMessage(level = TRACE)
    @Message(id = 20804, value = "Getting cache state: cache name = %s")
    void gettingCacheState(String cacheName);

    @LogMessage(level = TRACE)
    @Message(id = 20805, value = "Got cache state: result = %s")
    void gotCacheState(String result);

    @LogMessage(level = TRACE)
    @Message(id = 20806, value = "Executing CacheStateCommand: cache name = %s")
    void executingCacheStateCommand(String cacheName);

    @LogMessage(level = TRACE)
    @Message(id = 20807, value = "Executed CacheStateCommand: cache name = %s")
    void executedCacheStateCommand(String cacheName);

    @LogMessage(level = WARN)
    @Message(id = 20808, value = "Exception when processing cache state response")
    void exceptionProcessingCacheState(@Cause Throwable cause);

    @LogMessage(level = TRACE)
    @Message(id = 20809, value = "Executing ChannelStateCommand: channel name = %s")
    void executingChannelStateCommand(String channelName);

    @LogMessage(level = TRACE)
    @Message(id = 20810, value = "Executed ChannelStateCommand: channel name = %s")
    void executedChannelStateCommand(String channelName);

    @LogMessage(level = WARN)
    @Message(id = 20811, value = "Exception when processing channel state response")
    void exceptionProcessingChannelState(@Cause Throwable cause);

}
