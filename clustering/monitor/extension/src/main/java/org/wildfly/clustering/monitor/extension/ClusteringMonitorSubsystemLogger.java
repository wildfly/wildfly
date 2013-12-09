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

package org.wildfly.clustering.monitor.extension;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * ClusterSubsystemLogger
 *
 * logging id range: 20700 - 20749
 *
 * @author Richard Achmatowicz (c) Red Hat Inc 2013
 */
@MessageLogger(projectCode = "JBAS")
public interface ClusteringMonitorSubsystemLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = ClusteringMonitorSubsystemLogger.class.getPackage().getName();

    /**
     * A logger with the category of the default clustering package.
     */
    ClusteringMonitorSubsystemLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringMonitorSubsystemLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an info message indicating startup of the cluster subsystem..
     */
    @LogMessage(level = INFO)
    @Message(id = 20700, value = "Activating cluster subsystem.")
    void activatingSubsystem();

    @LogMessage(level = ERROR)
    @Message(id = 20701, value = "Exception calling deployment added listener")
    void deploymentAddListenerException(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 20702, value = "Exception calling deployment removal listener")
    void deploymentRemoveListenerException(@Cause Throwable cause);

    @LogMessage(level = TRACE)
    @Message(id = 20703, value = "Adding %s deployment %s to ClusteringDeploymentRepository")
    void addDeploymentToClusteredDeploymentRepository(String deploymentType, String deploymentName);

    @LogMessage(level = TRACE)
    @Message(id = 20704, value = "Removing deployment %s from ClusteringDeploymentRepository")
    void removeDeploymentFromClusteredDeploymentRepository(String deploymentName);

    @LogMessage(level = ERROR)
    @Message(id = 20705, value = "NoSuchFieldException handled for field %s in class %s")
    void noSuchFieldExceptionHandled(String field, String clazz);

    @LogMessage(level = ERROR)
    @Message(id = 20706, value = "IllegalAccessException handled for field %s in class %s")
    void illegalAccessExceptionHandled(String field, String clazz);

    @LogMessage(level = TRACE)
    @Message(id = 20707, value = "Processing cluster metrics request: channel = %s, attribute = %s")
    void processingClusterMetricsRequest(String channel, String attr);

    @LogMessage(level = TRACE)
    @Message(id = 20708, value = "Processing deployment metrics request: channel = %s, deployment = %s, attribute = %s")
    void processingDeploymentMetricsRequest(String channel, String deployment, String attr);

    @LogMessage(level = TRACE)
    @Message(id = 20709, value = "Processing bean metrics request: channel = %s, deployment = %s, attribute = %s")
    void processingWebMetricsRequest(String channel, String deployment, String attr);

    @LogMessage(level = TRACE)
    @Message(id = 20710, value = "Processing bean metrics request: channel = %s, deployment = %s, bean = %s, attribute = %s")
    void processingBeanMetricsRequest(String channel, String deployment, String bean, String attr);

    @LogMessage(level = TRACE)
    @Message(id = 20711, value = "Processed request: result = %s")
    void processedRequestResult(String result);

    @LogMessage(level = TRACE)
    @Message(id = 20712, value = "Executing channel state command: channel = %s")
    void executingChannelStateCommand(String channel);

}
