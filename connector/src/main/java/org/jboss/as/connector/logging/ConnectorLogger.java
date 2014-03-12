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

package org.jboss.as.connector.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import java.sql.Driver;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 01.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ConnectorLogger extends BasicLogger {

    /**
     * The root logger with a category of the default package.
     */
    ConnectorLogger ROOT_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, ConnectorLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.as.connector.deployers.jdbc}.
     */
    ConnectorLogger DEPLOYER_JDBC_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.deployers.jdbc");

    /**
     * A logger with the category {@code org.jboss.as.deployment.connector}.
     */
    ConnectorLogger DEPLOYMENT_CONNECTOR_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.deployment");

    /**
     * A logger with the category {@code org.jboss.as.deployment.connector.registry}.
     */
    ConnectorLogger DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.deployment.registry");

    /**
     * A logger with the category {@code org.jboss.as.connector.deployer.dsdeployer}.
     */
    ConnectorLogger DS_DEPLOYER_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.deployer.dsdeployer");

    /**
     * A logger with the category {@code org.jboss.as.connector.services.mdr}.
     */
    ConnectorLogger MDR_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.services.mdr");

    /**
     * A logger with the category {@code org.jboss.as.connector.subsystems.datasources}.
     */
    ConnectorLogger SUBSYSTEM_DATASOURCES_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.subsystems.datasources");

    /**
     * A logger with the category {@code org.jboss.as.connector.subsystems.resourceadapters}.
     */
    ConnectorLogger SUBSYSTEM_RA_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.subsystems.resourceadapters");

    /**
     * Logs an informational message indicating the data source has been bound.
     *
     * @param jndiName the JNDI name
     */
    @LogMessage(level = INFO)
    @Message(id = 10400, value = "Bound data source [%s]")
    void boundDataSource(String jndiName);

    /**
     * Logs an informational message indicating the JCA bound the object represented by the {@code description}
     * parameter.
     *
     * @param description the description of what was bound.
     * @param jndiName    the JNDI name.
     */
    @LogMessage(level = INFO)
    @Message(id = 10401, value = "Bound JCA %s [%s]")
    void boundJca(String description, String jndiName);

    /**
     * Logs a warning message indicating inability to instantiate the driver class.
     *
     * @param driverClassName the driver class name.
     * @param reason          the reason the the driver could not be instantiated.
     */
    @LogMessage(level = WARN)
    @Message(id = 10402, value = "Unable to instantiate driver class \"%s\": %s")
    void cannotInstantiateDriverClass(String driverClassName, Throwable reason);

    /**
     * Logs an informational message indicating the JDBC driver is compliant.
     *
     * @param driver       the JDBC driver class.
     * @param majorVersion the major version of the driver.
     * @param minorVersion the minor version of the driver.
     */
    @LogMessage(level = INFO)
    @Message(id = 10403, value = "Deploying JDBC-compliant driver %s (version %d.%d)")
    void deployingCompliantJdbcDriver(Class<? extends Driver> driver, int majorVersion, int minorVersion);

    /**
     * Logs an informational message indicating the JDBC driver is non-compliant.
     *
     * @param driver       the non-compliant JDBC driver class.
     * @param majorVersion the major version of the driver.
     * @param minorVersion the minor version of the driver.
     */
    @LogMessage(level = INFO)
    @Message(id = 10404, value = "Deploying non-JDBC-compliant driver %s (version %d.%d)")
    void deployingNonCompliantJdbcDriver(Class<? extends Driver> driver, int majorVersion, int minorVersion);

    /**
     * Logs an informational message indicating an admin object was registered.
     *
     * @param jndiName the JNDI name.
     */
    @LogMessage(level = INFO)
    @Message(id = 10405, value = "Registered admin object at %s")
    void registeredAdminObject(String jndiName);

    /**
     * Logs an informational message indicating the JNDI connection factory was registered.
     *
     * @param jndiName the JNDI connection factory.
     */
    @LogMessage(level = INFO)
    @Message(id = 10406, value = "Registered connection factory %s")
    void registeredConnectionFactory(String jndiName);

    /**
     * Logs an informational message indicating the service, represented by the {@code serviceName} parameter, is
     * starting.
     *
     * @param serviceName the name of the service that is starting.
     */
    @LogMessage(level = INFO)
    @Message(id = 10407, value = "Starting service %s")
    void startingService(ServiceName serviceName);

    /**
     * Logs an informational message indicating the subsystem, represented by the {@code subsystem} parameter, is
     * starting.
     *
     * @param subsystem the subsystem that is starting.
     * @param version   the version of the subsystem.
     */
    @LogMessage(level = INFO)
    @Message(id = 10408, value = "Starting %s Subsystem (%s)")
    void startingSubsystem(String subsystem, String version);

    /**
     * Logs an informational message indicating the data source has been unbound.
     *
     * @param jndiName the JNDI name
     */
    @LogMessage(level = INFO)
    @Message(id = 10409, value = "Unbound data source [%s]")
    void unboundDataSource(String jndiName);

    /**
     * Logs an informational message indicating the JCA inbound the object represented by the {@code description}
     * parameter.
     *
     * @param description the description of what was unbound.
     * @param jndiName    the JNDI name.
     */
    @LogMessage(level = INFO)
    @Message(id = 10410, value = "Unbound JCA %s [%s]")
    void unboundJca(String description, String jndiName);

    @LogMessage(level = WARN)
    @Message(id = 10411, value = "<drivers/> in standalone -ds.xml deployments aren't supported: Ignoring %s")
    void driversElementNotSupported(String deploymentName);

    @LogMessage(level = WARN)
    @Message(id = 10415, value = "Method %s on DataSource class %s not found. Ignoring")
    void methodNotFoundOnDataSource(final String method, final Class<?> clazz);

    @LogMessage(level = DEBUG)
    @Message(id = 10416, value = "Forcing ironjacamar.xml descriptor to null")
    void forceIJToNull();

    @LogMessage(level = INFO)
    @Message(id = 10417, value = "Started Driver service with driver-name = %s")
    void startedDriverService(String driverName);

    @LogMessage(level = INFO)
    @Message(id = 10418, value = "Stopped Driver service with driver-name = %s")
    void stoppeddDriverService(String driverName);

    @LogMessage(level = WARN)
    @Message(id = 10419, value = "Unsupported selector's option: %s")
    void unsupportedSelectorOption(String name);

    @LogMessage(level = WARN)
    @Message(id = 10420, value = "Unsupported policy's option: %s")
    void unsupportedPolicyOption(String name);

    /**
     * Creates an exception indicating a failure to start JGroup channel for a Disributed Work Manager
     *
     * @param channelName the name of the channel
     * @param wmName the name of the workmanager
     * @return a {@link StartException} for the error.
     */
    @Message(id = 10421, value = "Failed to start JGroups channel %s for distributed workmanager %s")
    StartException failedToStartJGroupsChannel(String channelName, String wmName);

    @Message(id = 10422, value = "Cannot find WorkManager %s or it isn't a distributed workmanager. Only DWM can override configurations")
    OperationFailedException failedToFindDistributedWorkManager(String wmName);

    @Message(id = 10423, value = "Failed to start JGroups transport for distributed workmanager %s")
    StartException failedToStartDWMTransport(String wmName);

    @Message(id = 10424, value = "Unsupported selector's option: %s")
    OperationFailedException unsupportedSelector(String name);

    @Message(id = 10425, value = "Unsupported policy's option: %s")
    OperationFailedException unsupportedPolicy(String name);

    @LogMessage(level = WARN)
    @Message(id = 10426, value = "No ironjacamar.security defined for %s")
    void noSecurityDefined(String jndiName);

    @LogMessage(level = WARN)
    @Message(id = 10427, value = "@ConnectionFactoryDefinition will have limited management: %s")
    void connectionFactoryAnnotation(String jndiName);

    @LogMessage(level = WARN)
    @Message(id = 10428, value = "@AdministeredObjectDefinition will have limited management: %s")
    void adminObjectAnnotation(String jndiName);

    /**
     * Logs a warning message indicating can't find the driver class name.
     *
     * @param driverName the driver jar.
     */
    @LogMessage(level = WARN)
    @Message(id = 10429, value = "Unable to find driver class name in \"%s\" jar")
    void cannotFindDriverClassName(String driverName);
}
