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

package org.jboss.as.webservices;

import java.io.IOException;
import java.net.URL;

import javax.xml.ws.WebServiceException;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface WSMessages {

    WSMessages MESSAGES = Messages.getBundle(WSMessages.class);

    @Message(id = 15500, value = "Null endpoint name")
    NullPointerException nullEndpointName();

    @Message(id = 15501, value = "Null endpoint class")
    NullPointerException nullEndpointClass();

    @Message(id = 15502, value = "Cannot resolve module or classloader for deployment %s")
    IllegalStateException classLoaderResolutionFailed(Object o);

    @Message(id = 15505, value = "Cannot load WS deployment aspects from %s")
    RuntimeException cannotLoadDeploymentAspectsDefinitionFile(@Cause Throwable cause, String resourcePath);

    @Message(id = 15507, value = "Handler chain config file %s not found in %s")
    WebServiceException missingHandlerChainConfigFile(String filePath, ResourceRoot resourceRoot);

    @Message(id = 15508, value = "Unexpected element: %s")
    IllegalStateException unexpectedElement(String elementName);

    @Message(id = 15509, value = "Unexpected end tag: %s")
    IllegalStateException unexpectedEndTag(String tagName);

    @Message(id = 15510, value = "Reached end of xml document unexpectedly")
    IllegalStateException unexpectedEndOfDocument();

    @Message(id = 15511, value = "Could not find class attribute for deployment aspect")
    IllegalStateException missingDeploymentAspectClassAttribute();

    @Message(id = 15512, value = "Could not create a deployment aspect of class: %s")
    IllegalStateException cannotInstantiateDeploymentAspect(@Cause Throwable cause, String className);

    @Message(id = 15513, value = "Could not find property name attribute for deployment aspect: %s")
    IllegalStateException missingPropertyNameAttribute(DeploymentAspect deploymentAspect);

    @Message(id = 15514, value = "Could not find property class attribute for deployment aspect: %s")
    IllegalStateException missingPropertyClassAttribute(DeploymentAspect deploymentAspect);

    @Message(id = 15515, value = "Unsupported property class: %s")
    IllegalArgumentException unsupportedPropertyClass(String className);

    @Message(id = 15516, value = "Could not create list of type: %s")
    IllegalStateException cannotInstantiateList(@Cause Throwable cause, String className);

    @Message(id = 15517, value = "Could not create map of type: %s")
    IllegalStateException cannotInstantiateMap(@Cause Throwable cause, String className);

    @Message(id = 15518, value = "No metrics available")
    String noMetricsAvailable();

    @Message(id = 15519, value = "EJB component view name cannot be null")
    IllegalStateException missingEjbComponentViewName();

    @Message(id = 15520, value = "Cannot find ejb view: %s")
    IllegalStateException cannotFindEjbView(ServiceName viewName);

    @Message(id = 15521, value = "Null root url")
    IllegalArgumentException nullRootUrl();

    @Message(id = 15522, value = "Null path")
    IllegalArgumentException nullPath();

    @Message(id = 15523, value = "Unable to get VirtualFile from URL: %s")
    IOException cannotGetVirtualFile(@Cause Throwable cause, URL url);

    @Message(id = 15524, value = "VirtualFile %s does not exist")
    IOException missingVirtualFile(VirtualFile file);

    @Message(id = 15525, value = "VirtualFile %s is not mounted")
    IOException unmountedVirtualFile(VirtualFile file);

    @Message(id = 15526, value = "Child '%s' not found for VirtualFile: %s")
    IOException missingChild(String child, VirtualFile file);

    @Message(id = 15527, value = "Failed to create context")
    Exception createContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 15528, value = "Failed to start context")
    Exception startContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 15529, value = "Failed to stop context")
    Exception stopContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 15530, value = "Failed to destroy context")
    Exception destroyContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 15531, value = "Cannot create servlet delegate: %s")
    IllegalStateException cannotInstantiateServletDelegate(@Cause Throwable cause, String className);

    @Message(id = 15532, value = "Cannot obtain deployment property: %s")
    IllegalStateException missingDeploymentProperty(String propertyName);

    @Message(id = 15584, value = "Multiple security domains not supported. First domain: '%s' second domain: '%s'")
    IllegalStateException multipleSecurityDomainsDetected(String firstDomain, String secondDomain);

    @Message(id = 15533, value = "Web Service endpoint %s with URL pattern %s is already registered. Web service endpoint %s is requesting the same URL pattern.")
    IllegalArgumentException sameUrlPatternRequested(String firstClass, String urlPattern, String secondClass);

    @Message(id = 15534, value = "@WebServiceRef injection target is invalid.  Only setter methods are allowed: %s")
    DeploymentUnitProcessingException invalidServiceRefSetterMethodName(Object o);

    @Message(id = 15535, value = "@WebServiceRef attribute 'name' is required fo class level annotations.")
    DeploymentUnitProcessingException requiredServiceRefName();

    @Message(id = 15536, value = "@WebServiceRef attribute 'type' is required fo class level annotations.")
    DeploymentUnitProcessingException requiredServiceRefType();
}
