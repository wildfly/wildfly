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

package org.jboss.as.service;

import javax.xml.namespace.QName;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

/**
 * This module is using message IDs in the range 17200-17299. This file is using the subset 17220-17299 for
 * non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface SarMessages {

    /**
     * The messages.
     */
    SarMessages MESSAGES = Messages.getBundle(SarMessages.class);

    /**
     * Creates an exception indicating the class could not be found.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17220, value = "Class not found")
    IllegalArgumentException classNotFound(@Cause Throwable cause);

    /**
     * Creates an exception indicating the class was not instantiated.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17221, value = "Class not instantiated")
    IllegalArgumentException classNotInstantiated(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to execute a legacy service method, represented by the {@code
     * methodName} parameter.
     *
     * @param cause      the cause of the error.
     * @param methodName the name of the method that failed to execute.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 17222, value = "Failed to execute legacy service %s method")
    StartException failedExecutingLegacyMethod(@Cause Throwable cause, String methodName);

    /**
     * Creates an exception indicating a failure to get an attachment.
     *
     * @param attachmentType the type/name of the attachment.
     * @param deploymentUnit the deployment unit.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 17223, value = "Failed to get %s attachment for %s")
    DeploymentUnitProcessingException failedToGetAttachment(String attachmentType, DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating a failure to parse the service XML file.
     *
     * @param xmlFile the XML file that failed to parse.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 17224, value = "Failed to parse service xml [%s]")
    DeploymentUnitProcessingException failedXmlParsing(VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to parse the service XML file.
     *
     * @param cause   the cause of the error.
     * @param xmlFile the XML file that failed to parse.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    DeploymentUnitProcessingException failedXmlParsing(@Cause Throwable cause, VirtualFile xmlFile);

    /**
     * Creates an exception indicating the method could not be found.
     *
     * @param methodName   the name of the method.
     * @param methodParams the method parameters.
     * @param className    the name of the class.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 17225, value = "Method '%s(%s)' not found for: %s")
    IllegalStateException methodNotFound(String methodName, String methodParams, String className);

    /**
     * A message indicating one or more required attributes are missing.
     *
     * @return the message.
     */
    @Message(id = 17226, value = "Missing one or more required attributes:")
    String missingRequiredAttributes();

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17227, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the method for the property is not found.
     *
     * @param methodPrefix the prefix for the method, e.g. {@code get} or {@code set}.
     * @param propertyName the name of the property.
     * @param className    the class the property was being queried on.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 17228, value = "%s method for property '%s' not found for: %s")
    IllegalStateException propertyMethodNotFound(String methodPrefix, String propertyName, String className);

    /**
     * A message indicating unexpected content was found.
     *
     * @param kind the kind of content.
     * @param name the name of the attribute.
     * @param text the value text of the attribute.
     *
     * @return the message.
     */
    @Message(id = 17229, value = "Unexpected content of type '%s' named '%s', text is: %s")
    String unexpectedContent(String kind, QName name, String text);
}
