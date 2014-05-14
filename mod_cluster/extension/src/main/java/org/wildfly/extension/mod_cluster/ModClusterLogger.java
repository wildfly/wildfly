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

import org.jboss.dmr.ModelNode;
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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYMODCLS", length = 4)
interface ModClusterLogger extends BasicLogger {
    /**
     * The root logger with a category of the package name.
     */
    ModClusterLogger ROOT_LOGGER = Logger.getMessageLogger(ModClusterLogger.class, "org.wildfly.extension.mod_cluster");

    /**
     * Logs an error message indicating an error when adding metrics.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Error adding metrics.")
    void errorAddingMetrics(@Cause Throwable cause);

    /**
     * Logs an error message indicating a start failure.
     *
     * @param cause the cause of the error.
     * @param name  the name for the service that failed to start.
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "%s failed to start.")
    void startFailure(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating a stop failure.
     *
     * @param cause the cause of the error.
     * @param name  the name for the service that failed to stop.
     */
    @LogMessage(level = ERROR)
    @Message(id = 3, value = "%s failed to stop.")
    void stopFailure(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating ModCluster requires advertise, but no multi-cast interface is available.
     */
    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Mod_cluster requires Advertise but Multicast interface is not available")
    void multicastInterfaceNotAvailable();

    /**
     * Logs an informational message indicating the default load balancer provider is being used.
     */
    @LogMessage(level = INFO)
    @Message(id = 5, value = "Mod_cluster uses default load balancer provider")
    void useDefaultLoadBalancer();

    /**
     * Logs an error message indicating that metric properties could not be applied on a custom load metric.
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Error applying properties to load metric class '%s'. Metric will not be loaded.")
    void errorApplyingMetricProperties(@Cause Throwable cause, String metricClass);

    /**
     * Logs a warning message that this metric type is no longer supported.
     */
    @LogMessage(level = WARN)
    @Message(id = 7, value = "Metric of type '%s' is no longer supported and will be ignored.")
    void unsupportedMetric(String metricType);

    /**
     * A message indicating a class attribute is needed for the attribute represented by the {@code attributeName}
     * parameter.
     *
     * @param attributeName the name of the required attribute.
     * @return the message.
     */
    @Message(id = 8, value = "A class attribute is needed for %s")
    String classAttributeRequired(String attributeName);

    /**
     * A message indicating the context and host are needed.
     *
     * @return the message.
     */
    @Message(id = 9, value = "need context and host")
    String needContextAndHost();

    /**
     * A message indicating a type attribute is needed for the attribute represented by the {@code attributeName}
     * parameter.
     *
     * @param attributeName the name of the required attribute.
     * @return the message.
     */
    @Message(id = 10, value = "A type attribute is needed for %s")
    String typeAttributeRequired(String attributeName);

    /**
     * A message indicating that the virtualhost or the context can't be found by modcluster.
     * @param Host
     * @param Context
     * @return the message.
     */
    @Message(id = 11, value = "virtualhost: %s or context %s not found")
    String ContextOrHostNotFound(String Host, String Context);

    @Message(id = 12, value = "'capacity' is either an expression, is not an integer value, or has a bigger value than Integer.MAX_VALUE: %s")
    String capacityIsExpressionOrGreaterThanIntegerMaxValue(ModelNode value);

    @Message(id = 13, value = "'property' can not have more than one entry")
    String propertyCanOnlyHaveOneEntry();

    /**
     * A message indicating a valid port and host are needed.
    *
    * @return the message.
    */
   @Message(id = 14, value = "need valid host and port")
   String needHostAndPort();

   @Message(id = 15, value = "session-draining-strategy must either be undefined or have the value \"DEFAULT\"")
   String sessionDrainingStrategyMustBeUndefinedOrDefault();

}
