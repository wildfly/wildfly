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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Radoslav Husar
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
     * @param cause the cause of the error
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Error adding metrics.")
    void errorAddingMetrics(@Cause Throwable cause);

//    /**
//     * Logs an error message indicating a start failure.
//     *
//     * @param cause the cause of the error
//     * @param name  the name for the service that failed to start
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 2, value = "%s failed to start.")
//    void startFailure(@Cause Throwable cause, String name);

//    /**
//     * Logs an error message indicating a stop failure.
//     *
//     * @param cause the cause of the error
//     * @param name  the name for the service that failed to stop
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 3, value = "%s failed to stop.")
//    void stopFailure(@Cause Throwable cause, String name);

    /**
     * Logs an error message indicating ModCluster requires advertise, but no multi-cast interface is available.
     */
    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Mod_cluster requires Advertise but Multicast interface is not available")
    void multicastInterfaceNotAvailable();

    @LogMessage(level = WARN)
    @Message(id = 5, value = "No mod_cluster load balance factor provider specified for proxy '%s'! Using load balance factor provider with constant factor of '1'.")
    void usingSimpleLoadProvider(String proxyName);

    /**
     * Logs an error message indicating that metric properties could not be applied on a custom load metric.
     *
     * @param cause       exception caught when applying properties
     * @param metricClass FQCN of the custom metric
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Error applying properties to load metric class '%s'. Metric will not be loaded.")
    void errorApplyingMetricProperties(@Cause Throwable cause, String metricClass);

    /**
     * Logs a warning message that this metric type is no longer supported.
     *
     * @param metricType name of the unsupported metric
     */
    @LogMessage(level = WARN)
    @Message(id = 7, value = "Metric of type '%s' is no longer supported and will be ignored.")
    void unsupportedMetric(String metricType);

//    /**
//     * A message indicating a class attribute is needed for the attribute represented by the {@code attributeName}
//     * parameter.
//     *
//     * @param attributeName the name of the required attribute
//     * @return the message
//     */
//    @Message(id = 8, value = "A class attribute is needed for %s")
//    String classAttributeRequired(String attributeName);

//    /**
//     * A message indicating a type attribute is needed for the attribute represented by the {@code attributeName}
//     * parameter.
//     *
//     * @param attributeName the name of the required attribute
//     * @return the message
//     */
//    @Message(id = 10, value = "A type attribute is needed for %s")
//    String typeAttributeRequired(String attributeName);

    /**
     * A message indicating that the virtual host or the context can't be found by modcluster.
     *
     * @param host    name of the virtual host
     * @param context name of the context
     * @return the message
     */
    @Message(id = 11, value = "Virtual host '%s' or context '%s' not found.")
    String contextOrHostNotFound(String host, String context);

//    @Message(id = 12, value = "'capacity' is either an expression, is not an integer value, or has a bigger value than Integer.MAX_VALUE: %s")
//    String capacityIsExpressionOrGreaterThanIntegerMaxValue(ModelNode value);

//    @Message(id = 13, value = "'property' can not have more than one entry")
//    String propertyCanOnlyHaveOneEntry();

    /**
     * A message indicating a valid port and host are needed.
     *
     * @return the message
     */
    @Message(id = 14, value = "Need valid host and port in the form host:port, %s is not valid")
    String needHostAndPort(String value);

//    @Message(id = 15, value = "session-draining-strategy must either be undefined or have the value \"DEFAULT\"")
//    String sessionDrainingStrategyMustBeUndefinedOrDefault();

    /**
     * A message indicating the host of the reverse proxy server could not be resolved.
     *
     * @return the message.
     */
    @Message(id = 16, value = "No IP address could be resolved for the specified host of the proxy.")
    String couldNotResolveProxyIpAddress();

//    /**
//     * A message explaining that 'proxy-list' attribute has been deprecated and that 'proxies' attribute which is a list
//     * of references to outbound-socket-binding(s) should be used instead.
//     *
//     * @return the message
//     */
//    @Message(id = 17, value = "'proxy-list' usage not allowed in the current model, can only be used to support older slaves")
//    String proxyListNotAllowedInCurrentModel();

//    /**
//     * Message indicating that only one of 'proxy-list' or 'proxies' attributes is allowed and the former one only
//     * to support older EAP 6.x slaves.
//     *
//     * @return the message
//     */
//    @Message(id = 18, value = "Usage of only one 'proxy-list' (only to support EAP 6.x slaves) or 'proxies' attributes allowed")
//    String proxyListAttributeUsage();

    /**
     * Logs a error message when excluded contexts are in a wrong format.
     *
     * @param trimmedContexts value which is in the wrong format
     */
    @Message(id = 19, value = "'%s' is not a valid value for excluded-contexts.")
    IllegalArgumentException excludedContextsWrongFormat(String trimmedContexts);

    /**
     * Exception thrown when user configures both 'ssl-context' attribute reference and the mod-cluster-config=configuration/ssl=configuration.
     */
    @Message(id = 20, value = "Only one of 'ssl-context' attribute or 'ssl' resource can be defined!")
    IllegalStateException bothElytronAndLegacySslContextDefined();

    @LogMessage(level = WARN)
    @Message(id = 21, value = "Value 'ROOT' for excluded-contexts is deprecated, to exclude the root context use '/' instead.")
    void excludedContextsUseSlashInsteadROOT();

    @Message(id = 22, value = "Legacy operations cannot be used with multiple proxy configurations. Use non-deprecated operations at the correct proxy address.")
    String legacyOperationsWithMultipleProxies();

    @LogMessage(level = ERROR)
    @Message(id = 23, value = "Error loading module '%s' to load custom metric from.")
    void errorLoadingModuleForCustomMetric(String moduleName, @Cause Throwable cause);
}
