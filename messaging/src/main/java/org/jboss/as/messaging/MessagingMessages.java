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

package org.jboss.as.messaging;

import static org.jboss.logging.Message.INHERIT;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Set;

/**
 * Date: 10.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface MessagingMessages {
    /**
     * The default messages.
     */
    MessagingMessages MESSAGES = Messages.getBundle(MessagingMessages.class);

    /**
     * A message indicating the alternative attribute represented by the {@code name} parameter is already defined.
     *
     * @param name the attribute name.
     *
     * @return the message.
     */
    @Message(id = 11630, value = "Alternative attribute of (%s) is already defined.")
    String altAttributeAlreadyDefined(String name);

    /**
     * Creates an exception indicating that all attribute definitions must have the same XML name.
     *
     * @param nameFound    the name found.
     * @param nameRequired the name that is required.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11631, value = "All attribute definitions must have the same xml name -- found %s but already had %s")
    IllegalArgumentException attributeDefinitionsMustMatch(String nameFound, String nameRequired);

    /**
     * Creates an exception indicating that all attribute definitions must have unique names.
     *
     * @param nameFound the name found.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11632, value = "All attribute definitions must have unique names -- already found %s")
    IllegalArgumentException attributeDefinitionsNotUnique(String nameFound);

    /**
     * Creates an exception indicating a {@code null} or empty JNDI name cannot be bound.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11633, value = "Cannot bind a null or empty string as jndi name")
    IllegalArgumentException cannotBindJndiName();

    /**
     * A message indicating the operation cannot include the parameter represented by {@code paramName1} and the
     * parameter represented by the {@code paramName2} parameter.
     *
     * @param paramName1 the name of the parameter.
     * @param paramName2 the name of the parameter.
     *
     * @return the message.
     */
    @Message(id = 11634, value = "Operation cannot include both parameter %s and parameter %s")
    String cannotIncludeOperationParameters(String paramName1, String paramName2);

    /**
     * Creates an exception indicating the attribute, represented by the {@code name} parameter, cannot be marshalled
     * and to use the {@code marshalAsElement} method.
     *
     * @param name the name of the attribute.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 11635, value = "%s cannot be marshalled as an attribute; use marshallAsElement")
    UnsupportedOperationException cannotMarshalAttribute(String name);

    /**
     * Creates an exception indicating a {@code null} or empty JNDI name cannot be unbound.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11636, value = "Cannot unbind a null or empty string as jndi name")
    IllegalArgumentException cannotUnbindJndiName();

    /**
     * A message indicating a child resource of the type, represented by the {@code type} parameter already exists.
     *
     * @param type the type that already exists.
     *
     * @return the message.
     */
    @Message(id = 11637, value = "A child resource of type %1$s already exists; the messaging subsystem only allows a single resource of type %1$s")
    String childResourceAlreadyExists(String type);

    /**
     * Creates an exception indicating the connector is not defined.
     *
     * @param connectorName the name of the connector.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11638, value = "Connector %s not defined")
    IllegalStateException connectorNotDefined(String connectorName);

    /**
     * A message indicating the {@code name} failed to get created.
     *
     * @param name the name.
     *
     * @return the message.
     */
    @Message(id = 11639, value = "Failed to create %s")
    String failedToCreate(String name);

    /**
     * Creates an exception indicating a failure to find the SocketBinding for the broadcast binding.
     *
     * @param name the name of the connector.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11640, value = "Failed to find SocketBinding for broadcast binding: %s")
    StartException failedToFindBroadcastSocketBinding(String name);

    /**
     * Creates an exception indicating a failure to find the SocketBinding for the connector.
     *
     * @param name the name of the connector.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11641, value = "Failed to find SocketBinding for connector: %s")
    StartException failedToFindConnectorSocketBinding(String name);

    /**
     * Creates an exception indicating a failure to find the SocketBinding for the discovery binding.
     *
     * @param name the name of the connector.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11642, value = "Failed to find SocketBinding for discovery binding: %s")
    StartException failedToFindDiscoverySocketBinding(String name);

    /**
     * Creates an exception indicating a server failed to shutdown.
     *
     * @param cause  the cause of the error.
     * @param server the server that failed to shutdown.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11643, value = "Failed to shutdown %s server")
    RuntimeException failedToShutdownServer(@Cause Throwable cause, String server);

    /**
     * Creates an exception indicating the service failed to start.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11644, value = "Failed to start service")
    StartException failedToStartService(@Cause Throwable cause);

    /**
     * Creates an exception indicating an unhandled element is being ignored.
     *
     * @param element  the element that's being ignored.
     * @param location the location of the element.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 11645, value = "Ignoring unhandled element: %s, at: %s")
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
    @Message(id = 11646, value = "Illegal element %s: cannot be used when %s is used")
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
    @Message(id = 11647, value = "Illegal value %s for element %s")
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

    /**
     * Creates an exception indicating a resource is immutable.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 11648, value = "Resource is immutable")
    UnsupportedOperationException immutableResource();

    /**
     * A message indicating the object, represented by the {@code obj} parameter, is invalid.
     *
     * @param obj the invalid object.
     *
     * @return the message.
     */
    @Message(id = 11649, value = "%s is invalid")
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
    @Message(id = 11650, value = "Attribute %s has unexpected type %s")
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
    @Message(id = 11651, value = "Operation must include parameter %s or parameter %s")
    String invalidOperationParameters(String paramName1, String paramName2);

    /**
     * A message indicating the value, represented by the {@code value} parameter, is invalid for the parameter,
     * represented by the {@code name} parameter. The value must must be one of the values defined in the
     * {@code allowedValues} parameter.
     *
     * @param value         the invalid value.
     * @param name          the name of the parameter.
     * @param allowedValues the values that are allowed.
     *
     * @return the message.
     */
    @Message(id = 11652, value = "%s is an invalid value for parameter %s. Values must be one of: %s")
    String invalidParameterValue(Object value, String name, Collection<?> allowedValues);

    /**
     * Creates an exception indicating the service, represented by the {@code service} parameter, is in an invalid
     * state.
     *
     * @param service      the service.
     * @param validState   the valid state.
     * @param currentState the current state of the service.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11653, value = "Service %s is not in state %s, it is in state %s")
    IllegalStateException invalidServiceState(ServiceName service, State validState, State currentState);

    /**
     * A message indicating the JNDI name has already been registered.
     *
     * @param jndiName the JNDI name.
     *
     * @return the message.
     */
    @Message(id = 11654, value = "JNDI name %s is already registered")
    String jndiNameAlreadyRegistered(String jndiName);

    /**
     * Creates an exception indicating multiple children, represented by the {@code element} parameter, were found, but
     * not allowed.
     *
     * @param element the element
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11655, value = "Multiple %s children found; only one is allowed")
    IllegalStateException multipleChildrenFound(String element);

    /**
     * A message the object, represented by the {@code obj} parameter, is required.
     *
     * @param obj the object that is required.
     *
     * @return the message.
     */
    @Message(id = 11656, value = "%s is required")
    String required(Object obj);

    /**
     * A message indicating either {@code obj1} or {@code obj2} is required.
     *
     * @param obj1 the first option.
     * @param obj2 the second option.
     *
     * @return the message.
     */
    @Message(id = 11657, value = "Either %s or %s is required")
    String required(Object obj1, Object obj2);

    /**
     * Creates an exception indicating the variable cannot be {@code null}
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11658, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * A message indicating the parameter, represented by the {@code parameter} parameter, is not defined.
     *
     * @param parameter the parameter.
     *
     * @return the message.
     */
    @Message(id = 11659, value = "Parameter not defined: %s")
    String parameterNotDefined(Object parameter);

    /**
     * A message indicating there is no such attribute.
     *
     * @param name the name of the attribute.
     *
     * @return the message.
     */
    @Message(id = 11660, value = "No such attribute (%s)")
    String unknownAttribute(String name);

    /**
     * Creates an exception indicating the read support for the attribute represented by the {@code name} parameter was
     * not properly implemented.
     *
     * @param name the name of the attribute.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11661, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException unsupportedAttribute(String name);

    /**
     * Creates an exception indicating that no support for the element, represented by the {@code name} parameter, has
     * been implemented.
     *
     * @param name the name of the element.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 11662, value = "Implement support for element %s")
    UnsupportedOperationException unsupportedElement(String name);

    /**
     * Creates an exception indicating the read support for the operation represented by the {@code name} parameter was
     * not properly implemented.
     *
     * @param name the operation name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11663, value = "Support for operation %s was not properly implemented")
    IllegalStateException unsupportedOperation(String name);

    /**
     * Creates an exception indicating the runtime handling of the attribute represented by the {@code name} parameter
     * is not implemented.
     *
     * @param name the name of the attribute.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 11664, value = "Runtime handling for %s is not implemented")
    UnsupportedOperationException unsupportedRuntimeAttribute(String name);

    /**
     * Creates an exception indicating the HornetQService for the server with the given name is either not installed
     * or not started.
     *
     * @param name the name of the Hornet Q server.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 11665, value = "No HornetQ Server is available under name %s")
    OperationFailedException hornetQServerNotInstalled(String name);
}
