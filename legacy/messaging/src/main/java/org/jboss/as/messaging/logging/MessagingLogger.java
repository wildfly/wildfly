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

package org.jboss.as.messaging.logging;

import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.annotations.Message.INHERIT;

import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.messaging.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 10.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("DefaultAnnotationParam")
@MessageLogger(projectCode = "WFLYMSG", length = 4)
public interface MessagingLogger extends BasicLogger {
    /**
     * The logger with the category of the package.
     */
    MessagingLogger ROOT_LOGGER = Logger.getMessageLogger(MessagingLogger.class, "org.jboss.as.messaging");

//    /**
//     * Logs a warning message indicating AIO was not found.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 1, value = "AIO wasn't located on this platform, it will fall back to using pure Java NIO.")
//    void aioWarning();
//
//    /**
//     * Logs an informational message indicating a messaging object was bound to the JNDI name represented by the
//     * {@code jndiName} parameter.
//     *
//     * @param jndiName the name the messaging object was bound to.
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 2, value = "Bound messaging object to jndi name %s")
//    void boundJndiName(String jndiName);
//
//    /**
//     * Logs an error message indicating an exception occurred while stopping the JMS server.
//     *
//     * @param cause the cause of the error.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 3, value = "Exception while stopping JMS server")
//    void errorStoppingJmsServer(@Cause Throwable cause);
//
//    /**
//     * Logs a warning message indicating the connection factory was not destroyed.
//     *
//     * @param cause       the cause of the error.
//     * @param description the description of what failed to get destroyed.
//     * @param name        the name of the factory.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 4, value = "Failed to destroy %s: %s")
//    void failedToDestroy(@Cause Throwable cause, String description, String name);
//
//    /**
//     * Logs a warning message indicating the connection factory was not destroyed.
//     *
//     * @param description the description of what failed to get destroyed.
//     * @param name        the name of the factory.
//     */
//    @LogMessage(level = WARN)
//    void failedToDestroy(String description, String name);
//
//    /**
//     * Logs an error message indicating the class, represented by the {@code className} parameter, caught an exception
//     * attempting to revert the operation, represented by the {@code operation} parameter, at the address, represented
//     * by the {@code address} parameter.
//     *
//     * @param cause     the cause of the error.
//     * @param className the name of the class that caused the error.
//     * @param operation the operation.
//     * @param address   the address.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 5, value = "%s caught exception attempting to revert operation %s at address %s")
//    void revertOperationFailed(@Cause Throwable cause, String className, String operation, PathAddress address);
//
//    /**
//     * Logs an informational message indicating a messaging object was unbound from the JNDI name represented by the
//     * {@code jndiName} parameter.
//     *
//     * @param jndiName the name the messaging object was bound to.
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 6, value = "Unbound messaging object to jndi name %s")
//    void unboundJndiName(String jndiName);
//
//    /**
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 7, value = "Could not close file %s")
//    void couldNotCloseFile(String file, @Cause Throwable cause);
//
//    /**
//     * Logs a warning message indicating the messaging object bound to the JNDI name represented by
//     * the {@code jndiName) has not be unbound in a timely fashion.
//     *
//     * @param jndiName the name the messaging object was bound to.
//     * @param timeout  the timeout value
//     * @param timeUnit the timeout time unit
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 8, value = "Failed to unbind messaging object bound to jndi name %s in %d %s")
//    void failedToUnbindJndiName(String jndiName, long timeout, String timeUnit);

    /**
     * Logs a warning message indicating the XML element with the given {@code name}
     * is deprecated and will not be used anymore.
     *
     * @param name the name of the deprecated XML element
     */
    @LogMessage(level = WARN)
    @Message(id = 9, value = "Element %s is deprecated and will not be taken into account")
    void deprecatedXMLElement(String name);

    /**
     * Logs a warning message indicating the XML attribute with the given {@code name}
     * is deprecated and will not be used anymore.
     *
     * @param name the name of the deprecated XML attribute
     */
    @LogMessage(level = WARN)
    @Message(id = 10, value = "Attribute %s is deprecated and will not be taken into account")
    void deprecatedXMLAttribute(String name);

//    /**
//     * Logs an info message when a service for the given {@code type} and {@code name} is <em>started</em>.
//     *
//     * @param type the type of the service
//     * @param name the name of the service
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 11, value = "Started %s %s")
//    void startedService(String type, String name);
//
//    /**
//     * Logs an info message when a service for the given {@code type} and {@code name} is <em>stopped</em>.
//     *
//     * @param type the type of the service
//     * @param name the name of the service
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 12, value = "Stopped %s %s")
//    void stoppedService(String type, String name);

    /**
     * Logs a warning message indicating the management attribute with the given {@code name}
     * is deprecated and will not be used anymore.
     *
     * @param name the name of the deprecated XML attribute
     */
    @LogMessage(level = WARN)
    @Message(id = 13, value = "Attribute %s of the resource at %s is deprecated and setting its value will not be taken into account")
    void deprecatedAttribute(String name, PathAddress address);

    @Message(id = 14, value = "Can not change the clustered attribute to false: The hornetq-server resource at %s has cluster-connection children resources and will remain clustered.")
    String canNotChangeClusteredAttribute(PathAddress address);

//    @LogMessage(level = WARN)
//    @Message(id = 15, value = "Ignoring %s property that is not a known property for pooled connection factory.")
//    void unknownPooledConnectionFactoryAttribute(String name);
//
//    /**
//     * Logs an info message when a HTTP Upgrade handler is registered for the given {@code protocol}.
//     *
//     * @param name the name of the protocol that is handled
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 16, value = "Registered HTTP upgrade for %s protocol handled by %s acceptor")
//    void registeredHTTPUpgradeHandler(String name, String acceptor);

    /**
     * Logs a warning message indicating the XML element with the given {@code name}
     * is deprecated and instead a different attribute will be used.
     *
     * @param name the name of the deprecated XML element
     */
    @LogMessage(level = WARN)
    @Message(id = 17, value = "Element %s is deprecated and %s will be used in its place")
    void deprecatedXMLElement(String name, String replacement);

//    /**
//     * Logs a warn message when no connectors were specified for a connection factory definition
//     * and one connector was picked up to be used.
//     *
//     * @param name the name of the connection factory definition
//     * @param connectorName the name of the connector that was picked
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 18, value = "No connectors were explicitly defined for the pooled connection factory %s. Using %s as the connector.")
//    void connectorForPooledConnectionFactory(String name, String connectorName);

    /**
     * A message indicating the alternative attribute represented by the {@code name} parameter is already defined.
     *
     * @param name the attribute name.
     *
     * @return the message.
     */
    @Message(id = 19, value = "Alternative attribute of (%s) is already defined.")
    String altAttributeAlreadyDefined(String name);

    /**
     * Creates an exception indicating that all attribute definitions must have the same XML name.
     *
     * @param nameFound    the name found.
     * @param nameRequired the name that is required.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 20, value = "All attribute definitions must have the same xml name -- found %s but already had %s")
    IllegalArgumentException attributeDefinitionsMustMatch(String nameFound, String nameRequired);

    /**
     * Creates an exception indicating that all attribute definitions must have unique names.
     *
     * @param nameFound the name found.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 21, value = "All attribute definitions must have unique names -- already found %s")
    IllegalArgumentException attributeDefinitionsNotUnique(String nameFound);

//    /**
//     * Creates an exception indicating a {@code null} or empty JNDI name cannot be bound.
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 22, value = "Cannot bind a null or empty string as jndi name")
//    IllegalArgumentException cannotBindJndiName();

    /**
     * A message indicating the operation cannot include the parameter represented by {@code paramName1} and the
     * parameter represented by the {@code paramName2} parameter.
     *
     * @param paramName1 the name of the parameter.
     * @param paramName2 the name of the parameter.
     *
     * @return the message.
     */
    @Message(id = 23, value = "Operation cannot include both parameter %s and parameter %s")
    String cannotIncludeOperationParameters(String paramName1, String paramName2);

//    /**
//     * Creates an exception indicating the attribute, represented by the {@code name} parameter, cannot be marshalled
//     * and to use the {@code marshalAsElement} method.
//     *
//     * @param name the name of the attribute.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 24, value = "%s cannot be marshalled as an attribute; use marshallAsElement")
//    UnsupportedOperationException cannotMarshalAttribute(String name);
//
//    /**
//     * Creates an exception indicating a {@code null} or empty JNDI name cannot be unbound.
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 25, value = "Cannot unbind a null or empty string as jndi name")
//    IllegalArgumentException cannotUnbindJndiName();
//
//    /**
//     * A message indicating a child resource of the type, represented by the {@code type} parameter already exists.
//     *
//     * @param type the type that already exists.
//     *
//     * @return the message.
//     */
//    @Message(id = 26, value = "A child resource of type %1$s already exists; the messaging subsystem only allows a single resource of type %1$s")
//    String childResourceAlreadyExists(String type);
//
//    /**
//     * Creates an exception indicating the connector is not defined.
//     *
//     * @param connectorName the name of the connector.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 27, value = "Connector %s not defined")
//    IllegalStateException connectorNotDefined(String connectorName);
//
//    /**
//     * Create an exception indicating that a messaging resource has failed
//     * to be created
//     *
//     * @param cause  the cause of the error.
//     * @param name the name that failed to be created.
//     *
//     * @return the message.
//     */
//    @Message(id = 28, value = "Failed to create %s")
//    StartException failedToCreate(@Cause Throwable cause, String name);
//
//    /**
//     * Creates an exception indicating a failure to find the SocketBinding for the broadcast binding.
//     *
//     * @param name the name of the connector.
//     *
//     * @return a {@link StartException} for the error.
//     */
//    @Message(id = 29, value = "Failed to find SocketBinding for broadcast binding: %s")
//    StartException failedToFindBroadcastSocketBinding(String name);
//
//    /**
//     * Creates an exception indicating a failure to find the SocketBinding for the connector.
//     *
//     * @param name the name of the connector.
//     *
//     * @return a {@link StartException} for the error.
//     */
//    @Message(id = 30, value = "Failed to find SocketBinding for connector: %s")
//    StartException failedToFindConnectorSocketBinding(String name);
//
//    /**
//     * Creates an exception indicating a failure to find the SocketBinding for the discovery binding.
//     *
//     * @param name the name of the connector.
//     *
//     * @return a {@link StartException} for the error.
//     */
//    @Message(id = 31, value = "Failed to find SocketBinding for discovery binding: %s")
//    StartException failedToFindDiscoverySocketBinding(String name);
//
//    /**
//     * Creates an exception indicating a server failed to shutdown.
//     *
//     * @param cause  the cause of the error.
//     * @param server the server that failed to shutdown.
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
//    @Message(id = 32, value = "Failed to shutdown %s server")
//    RuntimeException failedToShutdownServer(@Cause Throwable cause, String server);
//
//    /**
//     * Creates an exception indicating the service failed to start.
//     *
//     * @param cause the cause of the error.
//     *
//     * @return a {@link StartException} for the error.
//     */
//    @Message(id = 33, value = "Failed to start service")
//    StartException failedToStartService(@Cause Throwable cause);

    /**
     * Creates an exception indicating an unhandled element is being ignored.
     *
     * @param element  the element that's being ignored.
     * @param location the location of the element.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 34, value = "Ignoring unhandled element: %s, at: %s")
    XMLStreamException ignoringUnhandledElement(Element element, String location);

    /**
     * A message indicating an illegal element, represented by the {@code illegalElement} parameter, cannot be used
     * when the element, represented by the {@code element} parameter, is used.
     *
     * @param illegalElement the illegal element.
     * @param element        the element the {@code illegalElement} cannot be used with.
     *
     * @return the message.
     */
    @Message(id = 35, value = "Illegal element %s: cannot be used when %s is used")
    String illegalElement(String illegalElement, String element);

    /**
     * A message indicating an illegal value, represented by the {@code value} parameter, for the element, represented
     * by the {@code element} parameter.
     *
     * @param value   the illegal value.
     * @param element the element.
     *
     * @return the message.
     */
    @Message(id = 36, value = "Illegal value %s for element %s")
    String illegalValue(Object value, String element);

    /**
     * A message indicating an illegal value, represented by the {@code value} parameter, for the element, represented
     * by the {@code element} parameter, as it could not be converted to the required type, represented by the
     * {@code expectedType} parameter.
     *
     * @param value        the illegal value.
     * @param element      the element.
     * @param expectedType the required type.
     *
     * @return the message.
     */
    @Message(id = INHERIT, value = "Illegal value %s for element %s as it could not be converted to required type %s")
    String illegalValue(Object value, String element, ModelType expectedType);

//    /**
//     * Creates an exception indicating a resource is immutable.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 37, value = "Resource is immutable")
//    UnsupportedOperationException immutableResource();

    /**
     * A message indicating the object, represented by the {@code obj} parameter, is invalid.
     *
     * @param obj the invalid object.
     *
     * @return the message.
     */
    @Message(id = 38, value = "%s is invalid")
    String invalid(Object obj);

    /**
     * Creates an exception indicating the attribute, represented by the {@code name} parameter, has an unexpected type,
     * represented by the {@code type} parameter.
     *
     * @param name the name of the attribute.
     * @param type the type of the attribute.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 39, value = "Attribute %s has unexpected type %s")
    IllegalStateException invalidAttributeType(String name, ModelType type);

    /**
     * A message indicating the operation must include the parameter represented by {@code paramName1} or the parameter
     * represented by the {@code paramName2} parameter.
     *
     * @param paramName1 the name of the parameter.
     * @param paramName2 the name of the parameter.
     *
     * @return the message.
     */
    @Message(id = 40, value = "Operation must include parameter %s or parameter %s")
    String invalidOperationParameters(String paramName1, String paramName2);

//    /**
//     * A message indicating the value, represented by the {@code value} parameter, is invalid for the parameter,
//     * represented by the {@code name} parameter. The value must must be one of the values defined in the
//     * {@code allowedValues} parameter.
//     *
//     * @param value         the invalid value.
//     * @param name          the name of the parameter.
//     * @param allowedValues the values that are allowed.
//     *
//     * @return the message.
//     */
//    @Message(id = 41, value = "%s is an invalid value for parameter %s. Values must be one of: %s")
//    String invalidParameterValue(Object value, String name, Collection<?> allowedValues);
//
//    /**
//     * Creates an exception indicating the service, represented by the {@code service} parameter, is in an invalid
//     * state.
//     *
//     * @param service      the service.
//     * @param validState   the valid state.
//     * @param currentState the current state of the service.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 42, value = "Service %s is not in state %s, it is in state %s")
//    IllegalStateException invalidServiceState(ServiceName service, ServiceController.State validState, ServiceController.State currentState);
//
//    /**
//     * A message indicating the JNDI name has already been registered.
//     *
//     * @param jndiName the JNDI name.
//     *
//     * @return the message.
//     */
//    @Message(id = 43, value = "JNDI name %s is already registered")
//    String jndiNameAlreadyRegistered(String jndiName);

    /**
     * Creates an exception indicating multiple children, represented by the {@code element} parameter, were found, but
     * not allowed.
     *
     * @param element the element
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 44, value = "Multiple %s children found; only one is allowed")
    IllegalStateException multipleChildrenFound(String element);

    /**
     * A message the object, represented by the {@code obj} parameter, is required.
     *
     * @param obj the object that is required.
     *
     * @return the message.
     */
    @Message(id = 45, value = "%s is required")
    String required(Object obj);

    /**
     * A message indicating either {@code obj1} or {@code obj2} is required.
     *
     * @param obj1 the first option.
     * @param obj2 the second option.
     *
     * @return the message.
     */
    @Message(id = 46, value = "Either %s or %s is required")
    String required(Object obj1, Object obj2);

//    /**
//     * Creates an exception indicating the variable cannot be {@code null}
//     *
//     * @param varName the variable name.
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 47, value = "%s is null")
//    IllegalArgumentException nullVar(String varName);
//
//    /**
//     * A message indicating the parameter, represented by the {@code parameter} parameter, is not defined.
//     *
//     * @param parameter the parameter.
//     *
//     * @return the message.
//     */
//    @Message(id = 48, value = "Parameter not defined: %s")
//    String parameterNotDefined(Object parameter);
//
//    /**
//     * A message indicating there is no such attribute.
//     *
//     * @param name the name of the attribute.
//     *
//     * @return the message.
//     */
//    @Message(id = 49, value = "No such attribute (%s)")
//    String unknownAttribute(String name);
//
//    /**
//     * Creates an exception indicating the read support for the attribute represented by the {@code name} parameter was
//     * not properly implemented.
//     *
//     * @param name the name of the attribute.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 50, value = "Read support for attribute %s was not properly implemented")
//    IllegalStateException unsupportedAttribute(String name);

    /**
     * Creates an exception indicating that no support for the element, represented by the {@code name} parameter, has
     * been implemented.
     *
     * @param name the name of the element.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 51, value = "Implement support for element %s")
    UnsupportedOperationException unsupportedElement(String name);

//    /**
//     * Creates an exception indicating the read support for the operation represented by the {@code name} parameter was
//     * not properly implemented.
//     *
//     * @param name the operation name.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 52, value = "Support for operation %s was not properly implemented")
//    IllegalStateException unsupportedOperation(String name);
//
//    /**
//     * Creates an exception indicating the runtime handling of the attribute represented by the {@code name} parameter
//     * is not implemented.
//     *
//     * @param name the name of the attribute.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 53, value = "Runtime handling for %s is not implemented")
//    UnsupportedOperationException unsupportedRuntimeAttribute(String name);
//
//    /**
//     * Creates an exception indicating the HornetQService for the server with the given name is either not installed
//     * or not started.
//     *
//     * @param name the name of the Hornet Q server.
//     *
//     * @return an {@link OperationFailedException} for the error.
//     */
//    @Message(id = 54, value = "No HornetQ Server is available under name %s")
//    OperationFailedException hornetQServerNotInstalled(String name);
//
//
//    @Message(id = 55, value = "Could not parse file %s")
//    DeploymentUnitProcessingException couldNotParseDeployment(final String file, @Cause Throwable cause);
//
//    @Message(id = 56, value = "Handler cannot handle operation %s")
//    IllegalStateException operationNotValid(final String operation);
//
//    @Message(id = 57, value = "No message destination registered at address %s")
//    String noDestinationRegisteredForAddress(final PathAddress address);
//
//    @Message(id = 58, value = "SecurityDomainContext has not been set")
//    IllegalStateException securityDomainContextNotSet();

    /**
     * A message indicating only of of {@code obj1} or {@code obj2} is allowed.
     *
     * @param obj1 the first option.
     * @param obj2 the second option.
     *
     * @return the message.
     */
    @Message(id = 59, value = "Only one of %s or %s is required")
    String onlyOneRequired(Object obj1, Object obj2);


//    /**
//     * Create an exception indicating that a messaging resource has failed
//     * to be recovered
//     *
//     * @param cause  the cause of the error.
//     * @param name the name that failed to be recovered.
//     *
//     * @return the message.
//     */
//    @Message(id = 60, value = "Failed to recover %s")
//    OperationFailedException failedToRecover(@Cause Throwable cause, String name);
//
//    /**
//     * Create a failure description message indicating that an attribute is not supported by a given model version.
//     *
//     * @param attributes the name(s) of the unsupported attribute(s)
//     * @param version the model version that does not support the attribute
//     *
//     * @return the message.
//     */
//    @Message(id = 61, value = "Attribute(s) %s are not supported by messaging management model %s")
//    String unsupportedAttributeInVersion(String attributes, ModelVersion version);
//
//    /**
//     * Create a failure description message indicating that the clustered attribute is deprecated.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 62, value = "The clustered attribute is deprecated. To create a clustered HornetQ server, define at least one cluster-connection")
//    UnsupportedOperationException canNotWriteClusteredAttribute();
//
//    /**
//     * Create a failure description message indicating that the resource of given type can not be registered.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 63, value = "Resources of type %s cannot be registered")
//    UnsupportedOperationException canNotRegisterResourceOfType(String childType);
//
//    /**
//     * Create a failure description message indicating that the resource of given type can not be removed.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 64, value = "Resources of type %s cannot be removed")
//    UnsupportedOperationException canNotRemoveResourceOfType(String childType);
//
//    /**
//     * Logs an error message indicating the given {@code address} does not match any known
//     * resource. Meant for use with runtime resources available via {@link org.hornetq.core.server.HornetQServer#getManagementService()}
//     *
//     * @param address    the address.
//     */
//    @Message(id = 65, value = "No resource exists at address %s")
//    String hqServerManagementServiceResourceNotFound(PathAddress address);
//
//    @Message(id = 66, value = "Resource at the address %s can not be managed, the hornetq-server is in backup mode")
//    String hqServerInBackupMode(PathAddress address);

    /**
     * Create a failure description message indicating that the given broadcast-group's connector reference is not present in the listed connectors.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 67, value = "The broadcast group '%s' defines reference to nonexistent connector '%s'. Available connectors '%s'.")
    OperationFailedException wrongConnectorRefInBroadCastGroup(final String bgName, final String connectorRef, final Collection<String> presentConnectors);


//    /**
//     * Create an exception when calling a method not allowed on injected JMSContext.
//     *
//     * @return an {@link IllegalStateRuntimeException} for the error.
//     */
//    @Message(id = 68, value = "It is not permitted to call this method on injected JMSContext (see JMS 2.0 spec, §12.4.5).")
//    IllegalStateRuntimeException callNotPermittedOnInjectedJMSContext();

    /**
     * A message indicating the alternative attribute represented by the {@code name} parameter can not be undefined as the resource
     * has not defined any other alternative .
     *
     * @param name the attribute name.
     *
     * @return the message.
     */
    @Message(id = 69, value = "Attribute (%s) can not been undefined as the resource does not define any alternative to this attribute.")
    String undefineAttributeWithoutAlternative(String name);

    @Message(id = 70, value = "Attributes %s is an alias for attribute %s; both cannot be set with conflicting values.")
    OperationFailedException inconsistentStatisticsSettings(String attrOne, String attrTwo);

    /**
     * Logs a warn message when there is no resource matching the address-settings' expiry-address.
     *
     * @param address the name of the address-settings' missing expiry-address
     * @param addressSettings the name of the address-settings
     */
    @LogMessage(level = WARN)
    @Message(id = 71, value = "There is no resource matching the expiry-address %s for the address-settings %s, expired messages from destinations matching this address-setting will be lost!")
    void noMatchingExpiryAddress(String address, String addressSettings);

    /**
     * Logs a warn message when there is no resource matching the address-settings' dead-letter-address.
     *
     * @param address the name of the address-settings' missing dead-letter-address
     * @param addressSettings the name of the address-settings
     */
    @LogMessage(level = WARN)
    @Message(id = 72, value = "There is no resource matching the dead-letter-address %s for the address-settings %s, undelivered messages from destinations matching this address-setting will be lost!")
    void noMatchingDeadLetterAddress(String address, String addressSettings);

//    /**
//     * A message indicating the resource must have at least one JNDI name.
//     *
//     * @param jndiName the JNDI name.
//     *
//     * @return the message.
//     */
//    @Message(id = 73, value = "Can not remove JNDI name %s. The resource must have at least one JNDI name")
//    String canNotRemoveLastJNDIName(String jndiName);
//
//    @Message(id = 74, value = "Invalid parameter key: %s, the allowed keys are %s.")
//    OperationFailedException invalidParameterName(String parameterName, Set<String> allowedKeys);

//    /**
//     * Logs a warning message indicating AIO was not found, ask to install LibAIO to enable the AIO on Linux systems.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 75, value = "AIO wasn't located on this platform, it will fall back to using pure Java NIO. Your platform is Linux, install LibAIO to enable the AIO journal.")
//    void aioWarningLinux();

    @Message(id = 76, value = "Parameter %s contains duplicate elements [%s]")
    OperationFailedException duplicateElements(String parameterName, ModelNode elements);

//    @Message(id = 77, value = "Can not remove unknown entry %s")
//    OperationFailedException canNotRemoveUnknownEntry(String entry);
//
//    @Message(id = 78, value = "Indexed child resources can only be registered if the parent resource supports ordered children. The parent of '%s' is not indexed")
//    IllegalStateException indexedChildResourceRegistrationNotAvailable(PathElement address);

    @Message(id = 79, value = "The migrate operation can not be performed: the server must be in admin-only mode")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @Message(id = 80, value = "Can not migrate attribute %s to resource %s. Use instead the socket-binding attribute to configure this broadcast-group.")
    String couldNotMigrateBroadcastGroupAttribute(String attribute, PathAddress address);

    @Message(id = 81, value = "Migration failed, see results for more details.")
    String migrationFailed();

    @Message(id = 82, value = "Classes providing the %s are discarded during the migration. To use them in the new messaging-activemq subsystem, you will have to extend the Artemis-based Interceptor.")
    String couldNotMigrateInterceptors(String legacyInterceptorsAttributeName);

    @Message(id = 83, value = "Can not migrate the HA configuration of %s. Its shared-store and backup attributes holds expressions and it is not possible to determine unambiguously how to create the corresponding ha-policy for the messaging-activemq's server.")
    String couldNotMigrateHA(PathAddress address);

    @Message(id = 84, value = "Can not migrate attribute %s to resource %s. Use instead the socket-binding attribute to configure this discovery-group.")
    String couldNotMigrateDiscoveryGroupAttribute(String attribute, PathAddress address);

    @Message(id = 85, value = "Can not create a legacy-connection-factory based on connection-factory %s. It uses a HornetQ in-vm connector that is not compatible with Artemis in-vm connector ")
    String couldNotCreateLegacyConnectionFactoryUsingInVMConnector(PathAddress address);

    @Message(id = 86, value = "Can not migrate attribute %s to resource %s. The attribute uses an expression that can be resolved differently depending on system properties. After migration, this attribute must be added back with an actual value instead of the expression.")
    String couldNotMigrateResourceAttributeWithExpression(String attribute, PathAddress address);

    @Message(id = 87, value = "Can not migrate attribute %s to resource %s. This attribute is not supported by the new messaging-activemq subsystem.")
    String couldNotMigrateUnsupportedAttribute(String attribute, PathAddress address);

    @Message(id = 88, value = "Can not migrate attribute failback-delay to resource %s. Artemis detects failback deterministically and it no longer requires to specify a delay for failback to occur.")
    String couldNotMigrateFailbackDelayAttribute(PathAddress address);

    @Message(id = 89, value = "Changed the cluster-connection-address attribute to \"\" for resource %s. Artemis no longer uses jms as a prefix for addresses corresponding to JMS destinations).")
    String changingClusterConnectionAddress(PathAddress address);
}
