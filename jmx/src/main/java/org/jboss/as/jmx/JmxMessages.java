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

package org.jboss.as.jmx;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface JmxMessages {

    /**
     * The message.
     */
    JmxMessages MESSAGES = Messages.getBundle(JmxMessages.class);

    /**
     * Creates an exception indicating no attribute could be found matching the name.
     *
     * @param name the attribute name.
     *
     * @return an {@link AttributeNotFoundException} for the error.
     */
    @Message(id = 11330, value = "Could not find any attribute matching: %s")
    AttributeNotFoundException attributeNotFound(String name);

    /**
     * Creates an exception indicating the attribute is not writable.
     *
     * @param attribute the attribute that is not writable.
     *
     * @return an {@link AttributeNotFoundException} for the error.
     */
    @Message(id = 11331, value = "Attribute %s is not writable")
    AttributeNotFoundException attributeNotWritable(javax.management.Attribute attribute);

    /**
     * Creates an exception indicating the {@link ObjectName} could not be created for the address.
     *
     * @param cause   the cause of the error.
     * @param address the address.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11332, value = "Could not create ObjectName for address %s")
    RuntimeException cannotCreateObjectName(@Cause Throwable cause, PathAddress address);

    /**
     * Creates an exception indicating the attribute could not be set.
     *
     * @param cause the cause of the error.
     * @param name  the name of the attribute.
     *
     * @return a {@link ReflectionException} for the error.
     */
    @Message(id = 11333, value = "Could not set %s")
    ReflectionException cannotSetAttribute(@Cause Exception cause, String name);

    /**
     * Creates an exception indicating no description provider found for the address.
     *
     * @param address the address.
     *
     * @return an {@link InstanceNotFoundException} for the exception.
     */
    @Message(id = 11334, value = "No description provider found for %s")
    InstanceNotFoundException descriptionProviderNotFound(PathAddress address);

    /**
     * Creates an exception indicating the {@code name1} has a different length than {@code name2}.
     *
     * @param name1 the first name.
     * @param name2 the second name.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 11335, value = "%s and %s have different lengths")
    IllegalArgumentException differentLengths(String name1, String name2);

    /**
     * Creates an exception indicating the attribute type is invalid.
     *
     * @param cause the cause of the error.
     * @param name  the attribute name.
     *
     * @return an {@link InvalidAttributeValueException} for the error.
     */
    @Message(id = 11336, value = "Bad type for '%s'")
    InvalidAttributeValueException invalidAttributeType(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the key is invalid.
     *
     * @param keys  the list of keys.
     * @param entry the entry.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11337, value = "Invalid key %s for %s")
    IllegalArgumentException invalidKey(List<?> keys, Map.Entry<?, Object> entry);

    /**
     * Creates an exception indicating the {@link ObjectName} is invalid.
     *
     * @param name    the name of the object.
     * @param message a message to append.
     *
     * @return an {@link Error} for the error.
     */
    @Message(id = 11338, value = "Invalid ObjectName: %s; %s")
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
    @Message(id = 11339, value = "Received request for server socket %s on port [%d] but the service socket configured for port [%d]")
    IllegalStateException invalidServerSocketPort(String name, int port, int configuredPort);

    /**
     * Creates an exception indicating no MBean found with the name.
     *
     * @param name the object name.
     *
     * @return an {@link InstanceNotFoundException} for the error.
     */
    @Message(id = 11340, value = "No MBean found with name %s")
    InstanceNotFoundException mbeanNotFound(ObjectName name);

    /**
     * Creates an exception indicating a failure to register the MBean.
     *
     * @param cause the cause of the error.
     * @param name  the name of the MBean.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 11341, value = "Failed to register mbean [%s]")
    StartException mbeanRegistrationFailed(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating there is no operation called {@code operation}.
     *
     * @param operation the operation.
     *
     * @return a {@link InstanceNotFoundException} for the error.
     */
    @Message(id = 11342, value = "No operation called '%s'")
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
    @Message(id = 11343, value = "No operation called '%s' at %s")
    MBeanException noOperationCalled(@Cause Exception cause, String operation, PathAddress address);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11344, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating there is was no registration found for the path address.
     *
     * @param address the address.
     *
     * @return an {@link InstanceNotFoundException} for the error.
     */
    @Message(id = 11345, value = "No registration found for path address %s")
    InstanceNotFoundException registrationNotFound(PathAddress address);

    /**
     * A message indicating you cannot create mbeans under the reserved domain.
     *
     * @param name the reserved name.
     *
     * @return the message.
     */
    @Message(id = 11346, value = "You can't create mbeans under the reserved domain '%s'")
    String reservedMBeanDomain(String name);

    /**
     * Creates an exception indicating the type is unknown.
     *
     * @param type the unknown type.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11347, value = "Unknown type %s")
    RuntimeException unknownType(ModelType type);

    /**
     * Creates an exception indicating the value is unknown.
     *
     * @param value the unknown value.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11348, value = "Unknown value %s")
    IllegalArgumentException unknownValue(Object value);

    /**
     * Creates an exception indicating the need for a name parameter for wildcard add.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11349, value = "Need the name parameter for wildcard add")
    IllegalStateException wildcardNameParameterRequired();
}

