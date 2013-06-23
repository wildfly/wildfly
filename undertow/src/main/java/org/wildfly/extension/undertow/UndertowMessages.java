/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.io.File;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

/**
 * This module is using message IDs in the range 17300 - 17699.
 * <p/>
 * This file is using the subset 17300-17299 for non-logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 */
@MessageBundle(projectCode = "JBAS")
public interface UndertowMessages {

    /**
     * The default messages.
     */
    UndertowMessages MESSAGES = Messages.getBundle(UndertowMessages.class);

    @Message(id = 17301, value = "Unknown handler '%s' encountered")
    XMLStreamException unknownHandler(String name, @Param Location location);

    @Message(id = 17302, value = "Failed to parse XML descriptor %s at [%s,%s]")
    String failToParseXMLDescriptor(VirtualFile xmlFile, int line, int column);

    @Message(id = 17303, value = "Failed to parse XML descriptor %s")
    String failToParseXMLDescriptor(VirtualFile xmlFile);

    @Message(id = 17304, value = "@WebServlet is only allowed at class level %s")
    String invalidWebServletAnnotation(AnnotationTarget target);

    @Message(id = 17305, value = "@WebInitParam requires name and value on %s")
    String invalidWebInitParamAnnotation(AnnotationTarget target);

    @Message(id = 17306, value = "@WebFilter is only allowed at class level %s")
    String invalidWebFilterAnnotation(AnnotationTarget target);

    @Message(id = 17307, value = "@WebListener is only allowed at class level %s")
    String invalidWebListenerAnnotation(AnnotationTarget target);

    @Message(id = 17308, value = "@RunAs needs to specify a role name on %s")
    String invalidRunAsAnnotation(AnnotationTarget target);

    @Message(id = 17309, value = "@DeclareRoles needs to specify role names on %s")
    String invalidDeclareRolesAnnotation(AnnotationTarget target);

    @Message(id = 17310, value = "@MultipartConfig is only allowed at class level %s")
    String invalidMultipartConfigAnnotation(AnnotationTarget target);

    @Message(id = 17311, value = "@ServletSecurity is only allowed at class level %s")
    String invalidServletSecurityAnnotation(AnnotationTarget target);

    @Message(id = 17312, value = "%s has the wrong component type, it cannot be used as a web component")
    RuntimeException wrongComponentType(String clazz);

    @Message(id = 17313, value = "TLD file %s not contained in root %s")
    String tldFileNotContainedInRoot(String tldPath, String rootPath);

    @Message(id = 17314, value = "Failed to resolve module for deployment %s")
    DeploymentUnitProcessingException failedToResolveModule(DeploymentUnit deploymentUnit);

    @Message(id = 17315, value = "Duplicate others in absolute ordering")
    String invalidMultipleOthers();

    @Message(id = 17317, value = "Invalid relative ordering")
    String invalidRelativeOrdering();

    @Message(id = 17318, value = "Conflict occurred processing web fragment in JAR: %s")
    String invalidWebFragment(String jar);

    @Message(id = 17319, value = "Relative ordering processing error with JAR: %s")
    String invalidRelativeOrdering(String jar);

    @Message(id = 17320, value = "Ordering includes both before and after others in JAR: %s")
    String invalidRelativeOrderingBeforeAndAfter(String jar);

    @Message(id = 17321, value = "Duplicate name declared in JAR: %s")
    String invalidRelativeOrderingDuplicateName(String jar);

    @Message(id = 17322, value = "Unknown name declared in JAR: %s")
    String invalidRelativeOrderingUnknownName(String jar);

    @Message(id = 17323, value = "Relative ordering conflict with JAR: %s")
    String invalidRelativeOrderingConflict(String jar);

    @Message(id = 17324, value = "Failed to process WEB-INF/lib: %s")
    String failToProcessWebInfLib(VirtualFile xmlFile);

    @Message(id = 17325, value = "Error loading SCI from module: %s")
    DeploymentUnitProcessingException errorLoadingSCIFromModule(ModuleIdentifier identifier, @Cause Exception e);

    @Message(id = 17326, value = "Unable to resolve annotation index for deployment unit: %s")
    DeploymentUnitProcessingException unableToResolveAnnotationIndex(DeploymentUnit deploymentUnit);

    @Message(id = 17327, value = "Deployment error processing SCI for jar: %s")
    DeploymentUnitProcessingException errorProcessingSCI(String jar, @Cause Exception e);

    @Message(id = 17328, value = "Security context creation failed")
    RuntimeException failToCreateSecurityContext(@Cause Throwable t);

    @Message(id = 17329, value = "No security context found")
    IllegalStateException noSecurityContext();

    @Message(id = 17330, value = "Unknown metric %s")
    String unknownMetric(Object metric);

    @Message(id = 17331, value = "Null default host")
    IllegalArgumentException nullDefaultHost();

    @Message(id = 17332, value = "Null host name")
    IllegalStateException nullHostName();

    @Message(id = 17333, value = "Null parameter %s")
    IllegalArgumentException nullParamter(String id);

    @Message(id = 17345, value = "Cannot activate context: %s")
    IllegalStateException cannotActivateContext(@Cause Throwable th, ServiceName service);

    @Message(id = 17346, value = "Could not construct handler for class: %s. with parameters %s")
    RuntimeException cannotCreateHttpHandler(Class<?> handlerClass, ModelNode parameters, @Cause Throwable cause);

    @Message(id = 17347, value = "Could not delete servlet temp file %s")
    RuntimeException couldNotDeleteTempFile(File file);
}
