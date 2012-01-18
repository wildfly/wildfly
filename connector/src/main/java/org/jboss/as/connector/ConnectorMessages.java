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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

/**
 * Date: 01.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ConnectorMessages {

    /**
     * The messages
     */
    ConnectorMessages MESSAGES = Messages.getBundle(ConnectorMessages.class);

    /**
     * Creates an exception indicating the inability to complete the deployment.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 10430, value = "unable to deploy")
    DeployException cannotDeploy(@Cause Throwable cause);

    /**
     * Creates an exception indicating the inability to deploy and validate a datasource or an XA datasource.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 10431, value = "unable to validate and deploy ds or xads")
    DeployException cannotDeployAndValidate(@Cause Throwable cause);

    /**
     * Creates an exception indicating the data source was unable to start because it create more than one connection
     * factory.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 10432, value = "Unable to start the ds because it generated more than one cf")
    StartException cannotStartDs();

    /**
     * Creates an exception indicating an error occurred during deployment.
     *
     * @param cause the cause of the error.
     * @param name  the name of the deployment in error.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 10433, value = "Error during the deployment of %s")
    StartException deploymentError(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the deployment failed.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class that failed.
     *
     * @return a {@link DeployException} for the error.
     */
    @Message("Deployment %s failed")
    DeployException deploymentFailed(@Cause Throwable cause, String className);

    /**
     * A message indicating inability to instantiate the driver class.
     *
     * @param driverClassName the driver class name.
     *
     * @return the message.
     */
    @Message(id = 10434, value = "Unable to instantiate driver class \"%s\". See log (WARN) for more details")
    String cannotInstantiateDriverClass(String driverClassName);

    /**
     * Creates an exception indicating the specified driver version does not match the actual driver version.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10435, value = "Specified driver version doesn't match with actual driver version")
    IllegalStateException driverVersionMismatch();

    /**
     * A message indicating the type, represented by the {@code type} parameter, failed to be created for the operation
     * represented by the {@code operation} message.
     *
     * @param type          the type that failed to create.
     * @param operation     the operation.
     * @param reasonMessage the reason.
     *
     * @return the message.
     */
    @Message(id = 10436, value = "Failed to create %s instance for [%s]%n reason: %s")
    String failedToCreate(String type, ModelNode operation, String reasonMessage);

    /**
     * A message indicating a failure to get the metrics.
     *
     * @param message a message to append.
     *
     * @return the message.
     */
    @Message(id = 10437, value = "failed to get metrics: %s")
    String failedToGetMetrics(String message);

    /**
     * Creates an exception indicating a failure to get the module attachment for the deployment unit represented by
     * the {@code deploymentUnit} parameter.
     *
     * @param deploymentUnit the deployment.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 10438, value = "Failed to get module attachment for %s")
    DeploymentUnitProcessingException failedToGetModuleAttachment(DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating a failure to get the URL delimiter.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link DeployException} for the error.
     */
    @Message(id = 10439, value = "failed to get url delimiter")
    DeployException failedToGetUrlDelimiter(@Cause Throwable cause);

    /**
     * A message indicating a failure to invoke an operation.
     *
     * @param message the message to append.
     *
     * @return th message.
     */
    @Message(id = 10440, value = "failed to invoke operation: %s")
    String failedToInvokeOperation(String message);

    /**
     * A message indicating a failure to load the module for a driver.
     *
     * @param moduleName the module name.
     *
     * @return the message.
     */
    @Message(id = 10441, value = "Failed to load module for driver [%s]")
    String failedToLoadModuleDriver(String moduleName);

    /**
     * Creates an exception indicating a failure to match the pool.
     *
     * @param jndiName the JNDI name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10442, value = "failed to match pool. Check JndiName: %s")
    IllegalArgumentException failedToMatchPool(String jndiName);

    /**
     * Creates an exception indicating a failure to parse the service XML.
     *
     * @param xmlFile the service XML file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 10443, value = "Failed to parse service xml [%s]")
    DeploymentUnitProcessingException failedToParseServiceXml(VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to parse the service XML.
     *
     * @param cause   the cause of the error.
     * @param xmlFile the service XML file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    DeploymentUnitProcessingException failedToParseServiceXml(@Cause Throwable cause, VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to process the resource adapter child archives for the deployment root
     * represented by the {@code deploymentRoot} parameter.
     *
     * @param cause          the cause of the error.
     * @param deploymentRoot the deployment root.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 10444, value = "Failed to process RA child archives for [%s]")
    DeploymentUnitProcessingException failedToProcessRaChild(@Cause Throwable cause, VirtualFile deploymentRoot);

    /**
     * A message indicating a failure to set an attribute.
     *
     * @param message the message to append.
     *
     * @return the message.
     */
    @Message(id = 10445, value = "failed to set attribute: %s")
    String failedToSetAttribute(String message);

    /**
     * Creates an exception indicating the deployment, represented by the {@code deploymentName} parameter, failed to
     * start.
     *
     * @param cause          the cause of the error.
     * @param deploymentName the deployment name.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 10446, value = "Failed to start RA deployment [%s]")
    StartException failedToStartRaDeployment(@Cause Throwable cause, String deploymentName);

    /**
     * Creates an exception indicating the connection is not valid.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10447, value = "Connection is not valid")
    IllegalStateException invalidConnection();

    /**
     * A message indicating the parameter name is invalid.
     *
     * @param parameterName the invalid parameter name.
     *
     * @return the message.
     */
    @Message(id = 10448, value = "Invalid parameter name: %s")
    String invalidParameterName(String parameterName);

    /**
     * Creates an exception indicating non-explicit JNDI bindings are not supported.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10449, value = "Non-explicit JNDI bindings not supported")
    IllegalStateException jndiBindingsNotSupported();

    /**
     * A message indicating there are no metrics available.
     *
     * @return the message.
     */
    @Message(id = 10450, value = "no metrics available")
    String noMetricsAvailable();

    /**
     * Creates an exception indicating the class, represented by the {@code clazz} parameter, should be an annotation.
     *
     * @param clazz the invalid class.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10451, value = "%s should be an annotation")
    IllegalArgumentException notAnAnnotation(Class<?> clazz);

    /**
     * A message indicating the variable is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return the message
     */
    @Message(id = 10452, value = "%s is null")
    String nullVar(String name);

    /**
     * A message indicating the service, represented by the {@code serviceType} parameter, is already started on the
     * object represented by the {@code obj} parameter.
     *
     * @param serviceType the service type.
     * @param obj         the object.
     *
     * @return the message.
     */
    @Message(id = 10453, value = "%s service [%s] is already started")
    String serviceAlreadyStarted(String serviceType, Object obj);

    /**
     * A message indicating the service, represented by the {@code serviceType} parameter, is not available on th object
     * represented by the {@code obj} parameter.
     *
     * @param serviceType the service type.
     * @param obj         the object.
     *
     * @return the message.
     */
    @Message(id = 10454, value = "%s service [%s] is not available")
    String serviceNotAvailable(String serviceType, Object obj);

    /**
     * A message indicating the service, represented by the {@code serviceType} parameter, is not enabled on th object
     * represented by the {@code obj} parameter.
     *
     * @param serviceType the service type.
     * @param obj         the object.
     *
     * @return the message.
     */
    @Message(id = 10455, value = "%s service [%s] is not enabled")
    String serviceNotEnabled(String serviceType, Object obj);

    /**
     * Creates an exception indicating the service is not started.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10456, value = "Service not started")
    IllegalStateException serviceNotStarted();

    /**
     * Creates an exception indicating the property type is unknown.
     *
     * @param propertyType the unknown property type.
     * @param propertyName the name of the property.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10457, value = "Unknown property type: %s for property %s")
    IllegalArgumentException unknownPropertyType(String propertyType, String propertyName);

    /**
     * Creates an exception indicating a variable is undefined.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10458, value = "%s is undefined")
    IllegalArgumentException undefinedVar(String name);

    /**
     * Creates an exception indicating that a service is already registered
     *
     * @param name the name of the service.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10459, value = "Service '%s' already registered")
    IllegalStateException serviceAlreadyRegistered(String name);

    /**
     * Creates an exception indicating that a service isn't registered
     *
     * @param name the name of the service.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10460, value = "Service '%s' isn't registered")
    IllegalStateException serviceIsntRegistered(String name);

    /**
     * Failed to load native libraries
     * @param e the exception.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 10461, value = "Failed to load native libraries")
    DeploymentUnitProcessingException failedToLoadNativeLibraries(@Cause Throwable cause);

    /**
     * Creates an exception indicating that the ServiceName doesn't belong to a resource adapter service
     *
     * @param serviceName The service name
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10462, value = "%s isn't a resource adapter service")
    IllegalArgumentException notResourceAdapterService(ServiceName serviceName);

    /**
     * Creates and returns an exception indicating that the param named <code>paramName</code> cannot be null
     * or empty string.
     *
     * @param paramName The param name
     * @return an {@link IllegalArgumentException} for the exception
     */
    @Message(id = 10463, value = "%s cannot be null or empty")
    IllegalArgumentException stringParamCannotBeNullOrEmpty(final String paramName);

    @Message(id = 10464, value = "Exception deploying datasource %s")
    DeploymentUnitProcessingException exceptionDeployingDatasource(@Cause Throwable cause, String datasource);

}
