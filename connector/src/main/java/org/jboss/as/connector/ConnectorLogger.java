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

package org.jboss.as.connector;

import java.sql.Driver;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.msc.service.ServiceName;

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
    ConnectorLogger DEPLOYMENT_CONNECTOR_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.deployment.connector");

    /**
     * A logger with the category {@code org.jboss.as.deployment.connector.registry}.
     */
    ConnectorLogger DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.deployment.connector.registry");

    /**
     * A logger with the category {@code org.jboss.as.connector.deployer.dsdeployer}.
     */
    ConnectorLogger DS_DEPLOYER_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.deployer.dsdeployer");

    /**
     * A logger with the category {@code org.jboss.as.connector.mdr}.
     */
    ConnectorLogger MDR_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector.mdr");

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

    @Message(id = 10412, value = "the attribute class-name cannot be null for more than one connection-definition")
    OperationFailedException classNameNullForMoreCD();

    @Message(id = 10413, value = "the attribute class-name cannot be null for more than one admin-object")
    OperationFailedException classNameNullForMoreAO();

}
