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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.sql.Driver;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYJCA", length = 4)
public interface ConnectorLogger extends BasicLogger {

    /**
     * The root logger with a category of the default package.
     */
    ConnectorLogger ROOT_LOGGER = Logger.getMessageLogger(ConnectorLogger.class, "org.jboss.as.connector");

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
    @Message(id = 1, value = "Bound data source [%s]")
    void boundDataSource(String jndiName);

    /**
     * Logs an informational message indicating the JCA bound the object represented by the {@code description}
     * parameter.
     *
     * @param description the description of what was bound.
     * @param jndiName    the JNDI name.
     */
    @LogMessage(level = INFO)
    @Message(id = 2, value = "Bound JCA %s [%s]")
    void boundJca(String description, String jndiName);

    /**
     * Logs a warning message indicating inability to instantiate the driver class.
     *
     * @param driverClassName the driver class name.
     * @param reason          the reason the the driver could not be instantiated.
     */
    @LogMessage(level = WARN)
    @Message(id = 3, value = "Unable to instantiate driver class \"%s\": %s")
    void cannotInstantiateDriverClass(String driverClassName, Throwable reason);

    /**
     * Logs an informational message indicating the JDBC driver is compliant.
     *
     * @param driver       the JDBC driver class.
     * @param majorVersion the major version of the driver.
     * @param minorVersion the minor version of the driver.
     */
    @LogMessage(level = INFO)
    @Message(id = 4, value = "Deploying JDBC-compliant driver %s (version %d.%d)")
    void deployingCompliantJdbcDriver(Class<? extends Driver> driver, int majorVersion, int minorVersion);

    /**
     * Logs an informational message indicating the JDBC driver is non-compliant.
     *
     * @param driver       the non-compliant JDBC driver class.
     * @param majorVersion the major version of the driver.
     * @param minorVersion the minor version of the driver.
     */
    @LogMessage(level = INFO)
    @Message(id = 5, value = "Deploying non-JDBC-compliant driver %s (version %d.%d)")
    void deployingNonCompliantJdbcDriver(Class<? extends Driver> driver, int majorVersion, int minorVersion);

    /**
     * Logs an informational message indicating an admin object was registered.
     *
     * @param jndiName the JNDI name.
     */
    @LogMessage(level = INFO)
    @Message(id = 6, value = "Registered admin object at %s")
    void registeredAdminObject(String jndiName);

    /**
     * Logs an informational message indicating the JNDI connection factory was registered.
     *
     * @param jndiName the JNDI connection factory.
     */
    @LogMessage(level = INFO)
    @Message(id = 7, value = "Registered connection factory %s")
    void registeredConnectionFactory(String jndiName);

//    /**
//     * Logs an informational message indicating the service, represented by the {@code serviceName} parameter, is
//     * starting.
//     *
//     * @param serviceName the name of the service that is starting.
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 8, value = "Starting service %s")
//    void startingService(ServiceName serviceName);

    /**
     * Logs an informational message indicating the subsystem, represented by the {@code subsystem} parameter, is
     * starting.
     *
     * @param subsystem the subsystem that is starting.
     * @param version   the version of the subsystem.
     */
    @LogMessage(level = INFO)
    @Message(id = 9, value = "Starting %s Subsystem (%s)")
    void startingSubsystem(String subsystem, String version);

    /**
     * Logs an informational message indicating the data source has been unbound.
     *
     * @param jndiName the JNDI name
     */
    @LogMessage(level = INFO)
    @Message(id = 10, value = "Unbound data source [%s]")
    void unboundDataSource(String jndiName);

    /**
     * Logs an informational message indicating the JCA inbound the object represented by the {@code description}
     * parameter.
     *
     * @param description the description of what was unbound.
     * @param jndiName    the JNDI name.
     */
    @LogMessage(level = INFO)
    @Message(id = 11, value = "Unbound JCA %s [%s]")
    void unboundJca(String description, String jndiName);

    @LogMessage(level = WARN)
    @Message(id = 12, value = "<drivers/> in standalone -ds.xml deployments aren't supported: Ignoring %s")
    void driversElementNotSupported(String deploymentName);

    //    @Message(id = 13, value = "the attribute class-name cannot be null for more than one connection-definition")
//    OperationFailedException classNameNullForMoreCD();
//
//    @Message(id = 14, value = "the attribute class-name cannot be null for more than one admin-object")
//    OperationFailedException classNameNullForMoreAO();
//
    @Message(id = 15, value = "the attribute driver-name (%s) cannot be different from driver resource name (%s)")
    OperationFailedException driverNameAndResourceNameNotEquals(String driverName, String resourceName);

    @LogMessage(level = WARN)
    @Message(id = 16, value = "Method %s on DataSource class %s not found. Ignoring")
    void methodNotFoundOnDataSource(final String method, final Class<?> clazz);

    @LogMessage(level = DEBUG)
    @Message(id = 17, value = "Forcing ironjacamar.xml descriptor to null")
    void forceIJToNull();

    @LogMessage(level = INFO)
    @Message(id = 18, value = "Started Driver service with driver-name = %s")
    void startedDriverService(String driverName);

    @LogMessage(level = INFO)
    @Message(id = 19, value = "Stopped Driver service with driver-name = %s")
    void stoppedDriverService(String driverName);

    @LogMessage(level = WARN)
    @Message(id = 20, value = "Unsupported selector's option: %s")
    void unsupportedSelectorOption(String name);

    @LogMessage(level = WARN)
    @Message(id = 21, value = "Unsupported policy's option: %s")
    void unsupportedPolicyOption(String name);


    /**
     * Creates an exception indicating a failure to start JGroup channel for a Distributed Work Manager
     *
     * @param channelName the name of the channel
     * @param wmName      the name of the workmanager
     * @return a {@link StartException} for the error.
     */
    @Message(id = 22, value = "Failed to start JGroups channel %s for distributed workmanager %s")
    StartException failedToStartJGroupsChannel(String channelName, String wmName);

    @Message(id = 23, value = "Cannot find WorkManager %s or it isn't a distributed workmanager. Only DWM can override configurations")
    OperationFailedException failedToFindDistributedWorkManager(String wmName);

    @Message(id = 24, value = "Failed to start JGroups transport for distributed workmanager %s")
    StartException failedToStartDWMTransport(String wmName);

    @Message(id = 25, value = "Unsupported selector's option: %s")
    OperationFailedException unsupportedSelector(String name);

    @Message(id = 26, value = "Unsupported policy's option: %s")
    OperationFailedException unsupportedPolicy(String name);

    @LogMessage(level = WARN)
    @Message(id = 27, value = "No ironjacamar.security defined for %s")
    void noSecurityDefined(String jndiName);

    @LogMessage(level = WARN)
    @Message(id = 28, value = "@ConnectionFactoryDefinition will have limited management: %s")
    void connectionFactoryAnnotation(String jndiName);

    @LogMessage(level = WARN)
    @Message(id = 29, value = "@AdministeredObjectDefinition will have limited management: %s")
    void adminObjectAnnotation(String jndiName);

    /**
     * Creates an exception indicating the inability to complete the deployment.
     *
     * @param cause the cause of the error.
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 30, value = "unable to deploy")
    DeployException cannotDeploy(@Cause Throwable cause);

    /**
     * Creates an exception indicating the inability to deploy and validate a datasource or an XA datasource.
     *
     * @param cause the cause of the error.
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 31, value = "unable to validate and deploy ds or xads")
    DeployException cannotDeployAndValidate(@Cause Throwable cause);

    /**
     * Creates an exception indicating the data source was unable to start because it create more than one connection
     * factory.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 32, value = "Unable to start the ds because it generated more than one cf")
    StartException cannotStartDs();

    /**
     * Creates an exception indicating an error occurred during deployment.
     *
     * @param cause the cause of the error.
     * @param name  the name of the deployment in error.
     * @return a {@link StartException} for the error.
     */
    @Message(id = 33, value = "Error during the deployment of %s")
    StartException deploymentError(@Cause Throwable cause, String name);

    /**
     * A message indicating inability to instantiate the driver class.
     *
     * @param driverClassName the driver class name.
     * @return the message.
     */
    @Message(id = 34, value = "Unable to instantiate driver class \"%s\". See log (WARN) for more details")
    String cannotInstantiateDriverClass(String driverClassName);

    /**
     * Creates an exception indicating the specified driver version does not match the actual driver version.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 35, value = "Specified driver version doesn't match with actual driver version")
    IllegalStateException driverVersionMismatch();

    /**
     * A message indicating the type, represented by the {@code type} parameter, failed to be created for the operation
     * represented by the {@code operation} message.
     *
     * @param type          the type that failed to create.
     * @param operation     the operation.
     * @param reasonMessage the reason.
     * @return the message.
     */
    @Message(id = 36, value = "Failed to create %s instance for [%s]%n reason: %s")
    String failedToCreate(String type, ModelNode operation, String reasonMessage);

    /**
     * A message indicating a failure to get the metrics.
     *
     * @param message a message to append.
     * @return the message.
     */
    @Message(id = 37, value = "failed to get metrics: %s")
    String failedToGetMetrics(String message);

//    /**
//     * Creates an exception indicating a failure to get the module attachment for the deployment unit represented by
//     * the {@code deploymentUnit} parameter.
//     *
//     * @param deploymentUnit the deployment.
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
    //@Message(id = 38, value = "Failed to get module attachment for %s")
    //DeploymentUnitProcessingException failedToGetModuleAttachment(DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating a failure to get the URL delimiter.
     *
     * @param cause the cause of the error.
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 39, value = "failed to get url delimiter")
    DeployException failedToGetUrlDelimiter(@Cause Throwable cause);

    /**
     * A message indicating a failure to invoke an operation.
     *
     * @param message the message to append.
     * @return th message.
     */
    @Message(id = 40, value = "failed to invoke operation: %s")
    String failedToInvokeOperation(String message);

    /**
     * A message indicating a failure to load the module for a driver.
     *
     * @param moduleName the module name.
     * @return the message.
     */
    @Message(id = 41, value = "Failed to load module for driver [%s]")
    String failedToLoadModuleDriver(String moduleName);

    /**
     * Creates an exception indicating a failure to match the pool.
     *
     * @param jndiName the JNDI name.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 42, value = "failed to match pool. Check JndiName: %s")
    IllegalArgumentException failedToMatchPool(String jndiName);

    /**
     * Creates an exception indicating a failure to parse the service XML.
     *
     * @param xmlFile the service XML file.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 43, value = "Failed to parse service xml [%s]")
    DeploymentUnitProcessingException failedToParseServiceXml(VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to parse the service XML.
     *
     * @param cause   the cause of the error.
     * @param xmlFile the service XML file.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    DeploymentUnitProcessingException failedToParseServiceXml(@Cause Throwable cause, VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to process the resource adapter child archives for the deployment root
     * represented by the {@code deploymentRoot} parameter.
     *
     * @param cause          the cause of the error.
     * @param deploymentRoot the deployment root.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 44, value = "Failed to process RA child archives for [%s]")
    DeploymentUnitProcessingException failedToProcessRaChild(@Cause Throwable cause, VirtualFile deploymentRoot);

    /**
     * A message indicating a failure to set an attribute.
     *
     * @param message the message to append.
     * @return the message.
     */
    @Message(id = 45, value = "failed to set attribute: %s")
    String failedToSetAttribute(String message);

    /**
     * Creates an exception indicating the deployment, represented by the {@code deploymentName} parameter, failed to
     * start.
     *
     * @param cause          the cause of the error.
     * @param deploymentName the deployment name.
     * @return a {@link StartException} for the error.
     */
    @Message(id = 46, value = "Failed to start RA deployment [%s]")
    StartException failedToStartRaDeployment(@Cause Throwable cause, String deploymentName);

    /**
     * Creates an exception indicating the connection is not valid.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 47, value = "Connection is not valid")
    IllegalStateException invalidConnection();

//    /**
//     * A message indicating the parameter name is invalid.
//     *
//     * @param parameterName the invalid parameter name.
//     * @return the message.
//     */
//    @Message(id = 48, value = "Invalid parameter name: %s")
//    String invalidParameterName(String parameterName);

    /**
     * Creates an exception indicating non-explicit JNDI bindings are not supported.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 49, value = "Non-explicit JNDI bindings not supported")
    IllegalStateException jndiBindingsNotSupported();

    /**
     * A message indicating there are no metrics available.
     *
     * @return the message.
     */
    @Message(id = 50, value = "no metrics available")
    String noMetricsAvailable();

    /**
     * Creates an exception indicating the class, represented by the {@code clazz} parameter, should be an annotation.
     *
     * @param clazz the invalid class.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 51, value = "%s should be an annotation")
    IllegalArgumentException notAnAnnotation(Class<?> clazz);

    /**
     * A message indicating the variable is {@code null}.
     *
     * @param name the name of the variable.
     * @return the message
     */
    @Message(id = 52, value = "%s is null")
    String nullVar(String name);

    /**
     * A message indicating the service, represented by the {@code serviceType} parameter, is already started on the
     * object represented by the {@code obj} parameter.
     *
     * @param serviceType the service type.
     * @param obj         the object.
     * @return the message.
     */
    @Message(id = 53, value = "%s service [%s] is already started")
    String serviceAlreadyStarted(String serviceType, Object obj);

    /**
     * A message indicating the service, represented by the {@code serviceType} parameter, is not available on th object
     * represented by the {@code obj} parameter.
     *
     * @param serviceType the service type.
     * @param obj         the object.
     * @return the message.
     */
    @Message(id = 54, value = "%s service [%s] is not available")
    String serviceNotAvailable(String serviceType, Object obj);

//    /**
//     * A message indicating the service, represented by the {@code serviceType} parameter, is not enabled on th object
//     * represented by the {@code obj} parameter.
//     *
//     * @param serviceType the service type.
//     * @param obj         the object.
//     * @return the message.
//     */
//    @Message(id = 55, value = "%s service [%s] is not enabled")
//    String serviceNotEnabled(String serviceType, Object obj);

    /**
     * Creates an exception indicating the service is not started.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 56, value = "Service not started")
    IllegalStateException serviceNotStarted();

//    /**
//     * Creates an exception indicating the property type is unknown.
//     *
//     * @param propertyType the unknown property type.
//     * @param propertyName the name of the property.
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 57, value = "Unknown property type: %s for property %s")
//    IllegalArgumentException unknownPropertyType(String propertyType, String propertyName);

    /**
     * Creates an exception indicating a variable is undefined.
     *
     * @param name the name of the variable.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 58, value = "%s is undefined")
    IllegalArgumentException undefinedVar(String name);

//    /**
//     * Creates an exception indicating that a service is already registered
//     *
//     * @param name the name of the service.
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 59, value = "Service '%s' already registered")
//    IllegalStateException serviceAlreadyRegistered(String name);

//    /**
//     * Creates an exception indicating that a service isn't registered
//     *
//     * @param name the name of the service.
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 60, value = "Service '%s' isn't registered")
//    IllegalStateException serviceIsntRegistered(String name);

    /**
     * Failed to load native libraries
     *
     * @param cause the exception.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 61, value = "Failed to load native libraries")
    DeploymentUnitProcessingException failedToLoadNativeLibraries(@Cause Throwable cause);

//    /**
//     * Creates an exception indicating that the ServiceName doesn't belong to a resource adapter service
//     *
//     * @param serviceName The service name
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 62, value = "%s isn't a resource adapter service")
//    IllegalArgumentException notResourceAdapterService(ServiceName serviceName);

//    /**
//     * Creates and returns an exception indicating that the param named <code>paramName</code> cannot be null
//     * or empty string.
//     *
//     * @param paramName The param name
//     * @return an {@link IllegalArgumentException} for the exception
//     */
//    @Message(id = 63, value = "%s cannot be null or empty")
//    IllegalArgumentException stringParamCannotBeNullOrEmpty(final String paramName);

    @Message(id = 64, value = "Exception deploying datasource %s")
    DeploymentUnitProcessingException exceptionDeployingDatasource(@Cause Throwable cause, String datasource);

    /**
     * No datasource exists at the deployment address
     */
    @Message(id = 65, value = "No DataSource exists at address %s")
    String noDataSourceRegisteredForAddress(PathAddress address);

    /**
     * Creates an exception indicating unknown attribute
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 66, value = "Unknown attribute %s")
    IllegalStateException unknownAttribute(String attributeName);


    /**
     * Creates an exception indicating unknown operation
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 67, value = "Unknown operation %s")
    IllegalStateException unknownOperation(String attributeName);

//    /**
//     * A message indicating the driver is not installed
//     *
//     * @param driverName the driver name.
//     * @return the message.
//     */
//    @Message(id = 68, value = "Driver named \"%s\" is not installed.")
//    String driverNotPresent(String driverName);

    /**
     * A message indicating that at least on xa-datasource-property is required
     *
     * @return the message.
     */
    @Message(id = 69, value = "At least one xa-datasource-property is required for an xa-datasource")
    OperationFailedException xaDataSourcePropertiesNotPresent();

    /**
     * A message indicating that jndi-name is missing and it's a required attribute
     *
     * @return the message.
     */
    @Message(id = 70, value = "Jndi name is required")
    OperationFailedException jndiNameRequired();


    /**
     * A message indicating that jndi-name has an invalid format
     *
     * @return the message.
     */
    @Message(id = 71, value = "Jndi name have to start with java:/ or java:jboss/")
    OperationFailedException jndiNameInvalidFormat();

    /**
     * Creates an exception indicating the deployment failed.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that failed.
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 72, value = "Deployment %s failed")
    DeployException deploymentFailed(@Cause Throwable cause, String className);

    /**
     * A message indicating a failure to load the module for a RA deployed as module.
     *
     * @param moduleName the module name.
     * @return the message.
     */
    @Message(id = 73, value = "Failed to load module for RA [%s]")
    String failedToLoadModuleRA(String moduleName);

    /**
     * Creates an exception indicating a method is undefined.
     *
     * @param name the name of the method.
     * @return an {@link NoSuchMethodException} for the error.
     */
    @Message(id = 74, value = "Method %s not found")
    NoSuchMethodException noSuchMethod(String name);

    /**
     * Creates an exception indicating a field is undefined.
     *
     * @param name the name of the field.
     * @return an {@link NoSuchMethodException} for the error.
     */
    @Message(id = 75, value = "Field %s not found")
    NoSuchMethodException noSuchField(String name);

    /**
     * Creates an exception indicating a property can't be resolved
     *
     * @param name the name of the property.
     * @return an {@link NoSuchMethodException} for the error.
     */
    @Message(id = 76, value = "Unknown property resolution for property %s")
    IllegalArgumentException noPropertyResolution(String name);

    /**
     * A message indicating that at least one of archive or module attributes
     * gave to be defined
     *
     * @return the message.
     */
    @Message(id = 77, value = "At least one of ARCHIVE or MODULE is required")
    OperationFailedException archiveOrModuleRequired();

    /**
     * A message indicating a failure to load the module for a RA deployed as module.
     * The cause of this failure ius the use of unsupported compressed form for the rar
     *
     * @param moduleName the module name.
     * @return the message.
     */
    @Message(id = 78, value = "Rar are supported only in uncompressed form. Failed to load module for RA [%s]")
    String compressedRarNotSupportedInModuleRA(String moduleName);

    /**
     * Creates an exception indicating a failure to deploy the datasource because driver is not specified
     *
     * @param dsName the datasource to be deployed.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 79, value = "Failed to deploy datasource %s because driver is not specified")
    DeploymentUnitProcessingException FailedDeployDriverNotSpecified(String dsName);

    /**
     * Creates an exception indicating missing rar.
     *
     * @param raName - name.
     * @return a {@link OperationFailedException} for the error.
     */
    @Message(id = 80, value = "RAR '%s' not yet deployed.")
    OperationFailedException RARNotYetDeployed(String raName);

//    /**
//     * MDR empty during deployment of deployment annotation
//     *
//     * @param jndiName The JNDI name
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 81, value = "Empty MDR while deploying %s")
//    DeploymentUnitProcessingException emptyMdr(String jndiName);

//    /**
//     * Resource adapter not found during deployment of deployment annotation
//     *
//     * @param ra       The resource adapter
//     * @param jndiName The JNDI name
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 82, value = "Resource adapter (%s) not found while deploying %s")
//    DeploymentUnitProcessingException raNotFound(String ra, String jndiName);

    /**
     * Invalid connection factory interface defined
     *
     * @param cf       The connection factory
     * @param ra       The resource adapter
     * @param jndiName The JNDI name
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 83, value = "Connection factory interface (%s) is incorrect for resource adapter '%s' while deploying %s")
    DeploymentUnitProcessingException invalidConnectionFactory(String cf, String ra, String jndiName);

    /**
     * Admin object declared for JCA 1.0 archive
     *
     * @param ra       The resource adapter
     * @param jndiName The JNDI name
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 84, value = "Admin object declared for JCA 1.0 resource adapter '%s' while deploying %s")
    DeploymentUnitProcessingException adminObjectForJCA10(String ra, String jndiName);

    /**
     * Invalid admin object class defined
     *
     * @param ao       The admin object
     * @param ra       The resource adapter
     * @param jndiName The JNDI name
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 85, value = "Admin object class (%s) is incorrect for resource adapter '%s' while deploying %s")
    DeploymentUnitProcessingException invalidAdminObject(String ao, String ra, String jndiName);

    /**
     * Logs a warning message indicating can't find the driver class name.
     *
     * @param driverName the driver jar.
     */
    @LogMessage(level = WARN)
    @Message(id = 86, value = "Unable to find driver class name in \"%s\" jar")
    void cannotFindDriverClassName(String driverName);

    @LogMessage(level = ERROR)
    @Message(id = 87, value = "Unable to register recovery: %s (%s)")
    void unableToRegisterRecovery(String key, boolean isXa);

    @Message(id = 88, value = "Attributes %s rejected. Must be true")
    String rejectAttributesMustBeTrue(Set<String> key);

    @LogMessage(level = WARN)
    @Message(id = 89, value = "Exception during unregistering deployment")
    void exceptionDuringUnregistering(@Cause org.jboss.jca.core.spi.rar.NotFoundException nfe);

    @Message(id = 90, value = "Jndi name shouldn't include '//' or end with '/'")
    OperationFailedException jndiNameShouldValidate();

    @LogMessage(level = WARN)
    @Message(id = 91, value = "-ds.xml file deployments are deprecated. Support may be removed in a future version.")
    void deprecated();

    @Message(id = 92, value = "Indexed child resources can only be registered if the parent resource supports ordered children. The parent of '%s' is not indexed")
    IllegalStateException indexedChildResourceRegistrationNotAvailable(PathElement address);

    @LogMessage(level = INFO)
    @Message(id = 93, value = "The '%s' operation is deprecated. Use of the 'add' or 'remove' operations is preferred, or if required the 'write-attribute' operation can used to set the deprecated 'enabled' attribute")
    void legacyDisableEnableOperation(String name);

//    @Message(id = 94, value = "Driver %s should be defined in a profile named 'default' activated on server where deploying *-ds.xml")
//    IllegalStateException driverNotDefinedInDefaultProfile(String driverName);

//    @Message(id = 95, value = "At least one driver should be defined in a profile named 'default' activated on server where deploying *-ds.xml")
//    IllegalStateException noDriverDefinedInDefaultProfile();

    @LogMessage(level = ERROR)
    @Message(id = 96, value = "Error during recovery shutdown")
    void errorDuringRecoveryShutdown(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 97, value = "Exception while stopping resource adapter")
    void errorStoppingRA(@Cause Throwable cause);

    @LogMessage(level = INFO)
    @Message(id = 98, value = "Bound non-transactional data source: %s")
    void boundNonJTADataSource(String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 99, value = "Unbound non-transactional data source: %s")
    void unBoundNonJTADataSource(String jndiName);

    @Message(id = 100, value = "Operation %s is not supported")
    UnsupportedOperationException noSupportedOperation(String operation);

    @Message(id = 101, value = "Thread pool: %s(type: %s) can not be added for workmanager: %s, only one thread pool is allowed for each type.")
    OperationFailedException oneThreadPoolWorkManager(String threadPoolName, String threadPoolType, String workManagerName);

    /**
     * A message indicating that an attribute can only be set if another attribute is set as {@code true}.
     *
     * @param attribute          attribute that is invalid: it is defined but requires another attribute to be set as {@code true}
     * @param requiredAttribute  attribute that is required to be defined as {@code true}
     * @return the message.
     */
    @Message(id = 102, value = "Attribute %s can only be defined if %s is true")
    OperationFailedException attributeRequiresTrueAttribute(String attribute, String requiredAttribute);

    /**
     * A message indicating that an attribute can only be set if another attribute is undefined or set as {@code false}.
     *
     * @param attribute               attribute that is invalid: it is defined but requires another attribute to be set as {@code false} or to be undefined
     * @param requiredFalseAttribute  attribute that is required to be undefined or defined as {@code false}
     * @return the message.
     */
    @Message(id = 103, value = "Attribute %s can only be defined if %s is undefined or false")
    OperationFailedException attributeRequiresFalseOrUndefinedAttribute(String attribute, String requiredFalseAttribute);

    @Message(id = 104, value = "Subject=%s\nSubject identity=%s")
    String subject(Subject subject, String identity);

    @LogMessage(level = INFO)
    @Message(id = 106, value = "Elytron handler handle: %s")
    void elytronHandlerHandle(String callbacks);

    @Message(id = 107, value = "Execution subject was not provided to the callback handler")
    SecurityException executionSubjectNotSetInHandler();

    @Message(id = 108, value = "Supplied callback doesn't contain a security domain reference")
    IllegalArgumentException invalidCallbackSecurityDomain();

    @Message(id = 109, value = "Callback with security domain is required - use createCallbackHandler(Callback callback) instead")
    UnsupportedOperationException unsupportedCreateCallbackHandlerMethod();

    @Message(id = 110, value = "CredentialSourceSupplier is invalid for DSSecurity")
    IllegalStateException invalidCredentialSourceSupplier(@Cause Throwable cause);

    @Message(id = 111, value = "WorkManager hasn't elytron-enabled flag set accordingly with RA one")
    IllegalStateException invalidElytronWorkManagerSetting();

    @Message(id = 112, value = "Datasource %s is disabled")
    IllegalArgumentException datasourceIsDisabled(String jndiName);

    @LogMessage(level = ERROR)
    @Message(id =113, value = "Unexcepted error during worker execution : %s")
    void unexceptedWorkerCompletionError(String errorMessage, @Cause Throwable t);

    @Message(id = 114, value = "Failed to load datasource class: %s")
    OperationFailedException failedToLoadDataSourceClass(String clsName, @Cause Throwable t);

    @Message(id = 115, value = "Module for driver [%s] or one of it dependencies is missing: [%s]")
    String missingDependencyInModuleDriver(String moduleName, String missingModule);

    @Message(id = 116, value = "Failed to load module for RA [%s] - the module or one of its dependencies is missing [%s]")
    String raModuleNotFound(String moduleName, String missingModule);

    @Message(id = 117, value = "%s is not a valid %s implementation")
    OperationFailedException notAValidDataSourceClass(String clz, String dsClz);

}
