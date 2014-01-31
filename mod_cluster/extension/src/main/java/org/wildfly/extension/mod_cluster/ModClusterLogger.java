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

package org.wildfly.extension.mod_cluster;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 17.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface ModClusterLogger extends BasicLogger {
    /**
     * The root logger with a category of the package name.
     */
    ModClusterLogger ROOT_LOGGER = Logger.getMessageLogger(ModClusterLogger.class, ModClusterLogger.class.getPackage().getName());

    /**
     * Logs an error message indicating an error when adding metrics.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11700, value = "Error adding metrics.")
    void errorAddingMetrics(@Cause Throwable cause);

    /**
     * Logs an error message indicating a start failure.
     *
     * @param cause the cause of the error.
     * @param name  the name for the service that failed to start.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11701, value = "%s failed to start.")
    void startFailure(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating a stop failure.
     *
     * @param cause the cause of the error.
     * @param name  the name for the service that failed to stop.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11702, value = "%s failed to stop.")
    void stopFailure(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating ModCluster requires advertise, but no multi-cast interface is available.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11703, value = "Mod_cluster requires Advertise but Multicast interface is not available")
    void multicastInterfaceNotAvailable();

    /**
     * Logs an informational message indicating the default load balancer provider is being used.
     */
    @LogMessage(level = INFO)
    @Message(id = 11704, value = "Mod_cluster uses default load balancer provider")
    void useDefaultLoadBalancer();

    /**
     * Logs an error message indicating that metric properties could not be applied on a custom load metric.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11705, value = "Error applying properties to load metric class '%s'. Metric will not be loaded.")
    void errorApplyingMetricProperties(@Cause Throwable cause, String metricClass);

    /**
     * Logs a warning message that this metric type is no longer supported.
     */
    @LogMessage(level = WARN)
    @Message(id = 11706, value = "Metric of type '%s' is no longer supported and will be ignored.")
    void unsupportedMetric(String metricType);

}
