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

package org.jboss.as.jmx.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.MBeanException;
import javax.management.ObjectName;

import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelType;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYJMX", length = 4)
public interface JmxLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    JmxLogger ROOT_LOGGER = Logger.getMessageLogger(JmxLogger.class, "org.jboss.as.jmx");

    /**
     * Creates an exception indicating the inability to shutdown the RMI registry.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Could not shutdown rmi registry")
    void cannotShutdownRmiRegistry(@Cause Throwable cause);

    /**
     * Creates an exception indicating the JMX connector could not unbind from the registry.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Could not stop connector server")
    void cannotStopConnectorServer(@Cause Throwable cause);

    /**
     * Creates an exception indicating the JMX connector could not unbind from the registry.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Could not unbind jmx connector from registry")
    void cannotUnbindConnector(@Cause Throwable cause);

    /**
     * Logs a warning message indicating no {@link javax.management.ObjectName} is available to unregister.
     */
    @LogMessage(level = WARN)
    @Message(id = 4, value = "No ObjectName available to unregister")
    void cannotUnregisterObject();

    /**
     * Logs an error message indicating a failure to unregister the object name.
     *
     * @param cause the cause of the error.
     * @param name  the name of the object name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 5, value = "Failed to unregister [%s]")
    void unregistrationFailure(@Cause Throwable cause, ObjectName name);

    /**
     * The jmx-connector element is no longer supported.
     *
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "<jmx-connector/> is no longer supporting. <remoting-connector/> should be used instead to allow remote connections via JBoss Remoting.")
    void jmxConnectorNotSupported();


    @Message(id = Message.NONE, value = "entry")
    String compositeEntryTypeName();

    @Message(id = Message.NONE, value = "An entry")
    String compositeEntryTypeDescription();

    @Message (id = Message.NONE, value = "The key")
    String compositeEntryKeyDescription();

    @Message (id = Message.NONE, value = "The value")
    String compositeEntryValueDescription();

    @Message (id = Message.NONE, value = "A map")
    String compositeMapName();

    @Message (id = Message.NONE, value = "The map is indexed by 'key'")
    String compositeMapDescription();

    @Message(id = Message.NONE, value = "Complex type")
    String complexCompositeEntryTypeName();

    @Message(id = Message.NONE, value = "A complex type")
    String complexCompositeEntryTypeDescription();

    @Message(id = Message.NONE, value="This mbean does not support expressions for attributes or operation parameters, even when supported by the underlying model. Instead the resolved attribute is returned, and the real typed value must be used when writing attributes/invoking operations.")
    String descriptorMBeanExpressionSupportFalse();

    @Message(id = Message.NONE, value="This mbean supports raw expressions for attributes and operation parameters where supported by the underlying model. If no expression is used, the string representation is converted into the real attribute value.")
    String descriptorMBeanExpressionSupportTrue();

    @Message(id = Message.NONE, value="To be able to set and read expressions go to %s")
    String descriptorAlternateMBeanExpressions(ObjectName objectName);

    @Message(id = Message.NONE, value="To read resolved values and to write typed attributes and use typed operation parameters go to %s")
    String descriptorAlternateMBeanLegacy(ObjectName objectName);

    @Message(id = Message.NONE, value="This attribute supports expressions")
    String descriptorAttributeExpressionsAllowedTrue();

    @Message(id = Message.NONE, value="This attribute does not support expressions")
    String descriptorAttributeExpressionsAllowedFalse();

    @Message(id = Message.NONE, value="This parameter supports expressions")
    String descriptorParameterExpressionsAllowedTrue();

    @Message(id = Message.NONE, value="This parameter does not support expressions")
    String descriptorParameterExpressionsAllowedFalse();

    @Message(id = Message.NONE, value="A composite type representing a property")
    String propertyCompositeType();

    @Message(id = Message.NONE, value="The property name")
    String propertyName();

    @Message(id = Message.NONE, value="The property value")
    String propertyValue();


    /**
     * Creates an exception indicating no attribute could be found matching the name.
     *
     * @param name the attribute name.
     *
     * @return an {@link AttributeNotFoundException} for the error.
     */
    @Message(id = 7, value = "Could not find any attribute matching: %s")
    AttributeNotFoundException attributeNotFound(String name);

    /**
     * Creates an exception indicating the attribute is not writable.
     *
     * @param attribute the attribute that is not writable.
     *
     * @return an {@link AttributeNotFoundException} for the error.
     */
    @Message(id = 8, value = "Attribute %s is not writable")
    AttributeNotFoundException attributeNotWritable(javax.management.Attribute attribute);

    /**
     * Creates an exception indicating the {@link ObjectName} could not be created for the address.
     *
     *
     * @param cause   the cause of the error.
     * @param address the address.
     *
     * @param s
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 9, value = "Could not create ObjectName for address %s from string %s")
    RuntimeException cannotCreateObjectName(@Cause Throwable cause, PathAddress address, String s);

    /**
     * Creates an exception indicating the attribute could not be set.
     *
     * @param cause the cause of the error.
     * @param name  the name of the attribute.
     *
     * @return a {@link ReflectionException} for the error.
     */
    @Message(id = 10, value = "Could not set %s")
    ReflectionException cannotSetAttribute(@Cause Exception cause, String name);

    /**
     * Creates an exception indicating no description provider found for the address.
     *
     * @param address the address.
     *
     * @return an {@link InstanceNotFoundException} for the exception.
     */
    @Message(id = 11, value = "No description provider found for %s")
    InstanceNotFoundException descriptionProviderNotFound(PathAddress address);

    /**
     * Creates an exception indicating the {@code name1} has a different length than {@code name2}.
     *
     * @param name1 the first name.
     * @param name2 the second name.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 12, value = "%s and %s have different lengths")
    IllegalArgumentException differentLengths(String name1, String name2);

    /**
     * Creates an exception indicating the attribute type is invalid.
     *
     * @param cause the cause of the error.
     * @param name  the attribute name.
     *
     * @return an {@link InvalidAttributeValueException} for the error.
     */
    @Message(id = 13, value = "Bad type for '%s'")
    InvalidAttributeValueException invalidAttributeType(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the key is invalid.
     *
     * @param keys  the list of keys.
     * @param entry the entry.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14, value = "Invalid key %s for %s")
    IllegalArgumentException invalidKey(List<?> keys, Map.Entry<?, Object> entry);

    /**
     * Creates an exception indicating the {@link ObjectName} is invalid.
     *
     * @param name    the name of the object.
     * @param message a message to append.
     *
     * @return an {@link Error} for the error.
     */
    @Message(id = 15, value = "Invalid ObjectName: %s; %s")
    Error invalidObjectName(String name, String message);

    /**
     * Creates an exception indicating the {@link ObjectName} is invalid.
     *
     * @param domain  the object domain.
     * @param table   the table of name, value pairs.
     * @param message a message to append.
     *
     * @return an {@link Error} for the error.
     */
    @Message(id = Message.INHERIT, value = "Invalid ObjectName: %s,%s; %s")
    Error invalidObjectName(String domain, Hashtable<String, String> table, String message);

    /**
     * Creates an exception indicating the {@link ObjectName} is invalid.
     *
     * @param domain  the object domain.
     * @param key     the object key.
     * @param value   the object value.
     * @param message a message to append.
     *
     * @return an {@link Error} for the error.
     */
    @Message(id = Message.INHERIT, value = "Invalid ObjectName: %s,%s,%s; %s")
    Error invalidObjectName(String domain, String key, String value, String message);

    /**
     * Creates an exception indicating a request was received for the server socket, represented by the {@code name}
     * parameter, on the {@code port}, but the service socket is configured for the {@code configuredPort}.
     *
     * @param name           the name of the server socket.
     * @param port           the port.
     * @param configuredPort the configured port
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 16, value = "Received request for server socket %s on port [%d] but the service socket configured for port [%d]")
    IllegalStateException invalidServerSocketPort(String name, int port, int configuredPort);

    /**
     * Creates an exception indicating no MBean found with the name.
     *
     * @param name the object name.
     *
     * @return an {@link InstanceNotFoundException} for the error.
     */
    @Message(id = 17, value = "No MBean found with name %s")
    InstanceNotFoundException mbeanNotFound(ObjectName name);

    /**
     * Creates an exception indicating a failure to register the MBean.
     *
     * @param cause the cause of the error.
     * @param name  the name of the MBean.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 18, value = "Failed to register mbean [%s]")
    StartException mbeanRegistrationFailed(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating there is no operation called {@code operation}.
     *
     * @param operation the operation.
     *
     * @return a {@link InstanceNotFoundException} for the error.
     */
    @Message(id = 19, value = "No operation called '%s'")
    InstanceNotFoundException noOperationCalled(String operation);

    /**
     * Creates an exception indicating there is no operation called {@code operation} at the {@code address}.
     *
     * @param cause     the cause of the error.
     * @param operation the operation.
     * @param address   the address.
     *
     * @return a {@link MBeanException} for the error.
     */
    @Message(id = 20, value = "No operation called '%s' at %s")
    MBeanException noOperationCalled(@Cause Exception cause, String operation, PathAddress address);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 21, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating there is was no registration found for the path address.
     *
     * @param address the address.
     *
     * @return an {@link InstanceNotFoundException} for the error.
     */
    @Message(id = 22, value = "No registration found for path address %s")
    InstanceNotFoundException registrationNotFound(PathAddress address);

    /**
     * A message indicating you cannot create mbeans under the reserved domain.
     *
     * @param name the reserved name.
     *
     * @return the message.
     */
    @Message(id = 23, value = "You can't create mbeans under the reserved domain '%s'")
    String reservedMBeanDomain(String name);

    /**
     * Creates an exception indicating the type is unknown.
     *
     * @param type the unknown type.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 24, value = "Unknown type %s")
    RuntimeException unknownType(ModelType type);

    /**
     * Creates an exception indicating the value is unknown.
     *
     * @param value the unknown value.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 25, value = "Unknown value %s")
    IllegalArgumentException unknownValue(Object value);

    /**
     * Creates an exception indicating the need for a name parameter for wildcard add.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 26, value = "Need the name parameter for wildcard add")
    IllegalStateException wildcardNameParameterRequired();

    @Message(id = 27, value="An error happened creating a composite type for %s")
    IllegalStateException errorCreatingCompositeType(@Cause OpenDataException e, OpenType<?> type);

    @Message(id = 28, value="An error happened creating a composite data for %s")
    IllegalStateException errorCreatingCompositeData(@Cause OpenDataException e, OpenType<?> type);

    @Message(id = 29, value="Unknown domain: %s")
    IllegalArgumentException unknownDomain(String domain);

    @Message(id = 30, value="Expression can not be converted into target type %s")
    IllegalArgumentException expressionCannotBeConvertedIntoTargeteType(OpenType<?> type);

    @Message(id = 31, value = "Unknown child %s")
    IllegalArgumentException unknownChild(String child);

    @Message(id = 32, value = "ObjectName cannot be null")
    IllegalArgumentException objectNameCantBeNull();

    @Message(id = 33, value = "'domain-name' can only be 'jboss.as'")
    String domainNameMustBeJBossAs();


    @Message(id = 34, value = "'false' is the only acceptable value for 'proper-property-format'")
    String properPropertyFormatMustBeFalse();

    @Message(id = 35, value = "The 'enabled' attribute of audit-log must be false")
    String auditLogEnabledMustBeFalse();

    @Message(id = 36, value = "There is no handler called '%s'")
    IllegalStateException noHandlerCalled(String name);

    @Message(id = 37, value = "Unauthorized access")
    JMRuntimeException unauthorized();

    @Message(id = 38, value = "Not authorized to write attribute: '%s'")
    JMRuntimeException notAuthorizedToWriteAttribute(String attributeName);

    @Message(id = 39, value = "Not authorized to read attribute: '%s'")
    JMRuntimeException notAuthorizedToReadAttribute(String attributeName);

    @Message(id = 40, value = "Not authorized to invoke operation: '%s'")
    JMRuntimeException notAuthorizedToExecuteOperation(String operationName);
}
