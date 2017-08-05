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

package org.jboss.as.web;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * This module is using message IDs in the range 18000-18099 and 18200-18399.
 * <p/>
 * This file is using the subset 18000-18199 for non-logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface WebMessages {

    /**
     * The messages
     */
    WebMessages MESSAGES = Messages.getBundle(WebMessages.class);

    @Message(id = 18000, value = "Name and value are required to add mime mapping")
    String nameAndValueRequiredForAddMimeMapping();
    /*
    @Message(id = 18001, value = "Name is required to remove mime mapping")
    String nameRequiredForRemoveMimeMapping();

    @Message(id = 18002, value = "Failed to get metrics %s")
    String failedToGetMetrics(String reason);

    @Message(id = 18003, value = "No metrics available")
    String noMetricsAvailable();

    @Message(id = 18005, value = "Non HTTP connectors do not support SSL")
    String noSSLWithNonHTTPConnectors();

    @Message(id = 18006, value = "SSL configuration failed")
    String failedSSLConfiguration();

    @Message(id = 18007, value = "Error starting web connector")
    String connectorStartError();

    @Message(id = 18008, value = "Null service value")
    IllegalStateException nullValue();

    @Message(id = 18009, value = "Error starting web container")
    String errorStartingWeb();

    @Message(id = 18010, value = "A default web module can not be specified when the welcome root has been enabled")
    String noRootWebappWithWelcomeWebapp();
    */

    @Message(id = 18011, value = "The welcome root can not be enabled on a host that has the default web module")
    String noWelcomeWebappWithDefaultWebModule();

    /*
    @Message(id = 18012, value = "Failed to create welcome context")
    String createWelcomeContextFailed();

    @Message(id = 18013, value = "Failed to start welcome context")
    String startWelcomeContextFailed();

    @Message(id = 18014, value = "Failed to parse XML descriptor %s at [%s,%s]")
    String failToParseXMLDescriptor(VirtualFile xmlFile, int line, int column);

    @Message(id = 18015, value = "Failed to parse XML descriptor %s")
    String failToParseXMLDescriptor(VirtualFile xmlFile);

    @Message(id = 18016, value = "@WebServlet is only allowed at class level %s")
    String invalidWebServletAnnotation(AnnotationTarget target);

    @Message(id = 18017, value = "@WebInitParam requires name and value on %s")
    String invalidWebInitParamAnnotation(AnnotationTarget target);

    @Message(id = 18018, value = "@WebFilter is only allowed at class level %s")
    String invalidWebFilterAnnotation(AnnotationTarget target);

    @Message(id = 18019, value = "@WebListener is only allowed at class level %s")
    String invalidWebListenerAnnotation(AnnotationTarget target);

    @Message(id = 18020, value = "@RunAs needs to specify a role name on %s")
    String invalidRunAsAnnotation(AnnotationTarget target);

    @Message(id = 18021, value = "@DeclareRoles needs to specify role names on %s")
    String invalidDeclareRolesAnnotation(AnnotationTarget target);

    @Message(id = 18022, value = "@MultipartConfig is only allowed at class level %s")
    String invalidMultipartConfigAnnotation(AnnotationTarget target);

    @Message(id = 18023, value = "@ServletSecurity is only allowed at class level %s")
    String invalidServletSecurityAnnotation(AnnotationTarget target);

    @Message(id = 18024, value = "Null default host")
    IllegalArgumentException nullDefaultHost();

    @Message(id = 18025, value = "Null host name")
    IllegalStateException nullHostName();

    @Message(id = 18026, value = "Failed to resolve module for deployment %s")
    String failedToResolveModule(Object deploymentHandle);

    @Message(id = 18027, value = "Failed to add JBoss Web deployment service")
    String failedToAddWebDeployment();

    @Message(id = 18028, value = "Duplicate others in absolute ordering")
    String invalidMultipleOthers();

    @Message(id = 18029, value = "Could not resolve name in absolute ordering: %s")
    String invalidAbsoluteOrdering(String name);

    @Message(id = 18030, value = "Invalid relative ordering")
    String invalidRelativeOrdering();

    @Message(id = 18031, value = "Conflict occurred processing web fragment in JAR: %s")
    String invalidWebFragment(String jar);

    @Message(id = 18032, value = "Relative ordering processing error with JAR: %s")
    String invalidRelativeOrdering(String jar);

    @Message(id = 18033, value = "Ordering includes both before and after others in JAR: %s")
    String invalidRelativeOrderingBeforeAndAfter(String jar);

    @Message(id = 18034, value = "Duplicate name declared in JAR: %s")
    String invalidRelativeOrderingDuplicateName(String jar);

    @Message(id = 18035, value = "Unknown name declared in JAR: %s")
    String invalidRelativeOrderingUnknownName(String jar);

    @Message(id = 18036, value = "Relative ordering conflict with JAR: %s")
    String invalidRelativeOrderingConflict(String jar);

    @Message(id = 18037, value = "Failed to process WEB-INF/lib: %s")
    String failToProcessWebInfLib(VirtualFile xmlFile);

    @Message(id = 18038, value = "Root contexts can not be deployed when the virtual host configuration has the welcome root enabled, disable it and redeploy")
    IllegalStateException conflictOnDefaultWebapp();

    @Message(id = 18039, value = "Failed to create context")
    String createContextFailed();

    @Message(id = 18040, value = "Failed to start context")
    String startContextFailed();

    @Message(id = 18041, value = "Servlet components must have exactly one view: %s")
    RuntimeException servletsMustHaveOneView(String componentName);

    @Message(id = 18042, value = "Not implemented")
    String notImplemented();

    @Message(id = 18043, value = "%s has the wrong component type, it cannot be used as a web component")
    RuntimeException wrongComponentType(String clazz);

    @Message(id = 18044, value = "Resource not found: %s")
    String resourceNotFound(String resourceName);

    @Message(id = 18045, value = "Failed to load annotated class: %s")
    String classLoadingFailed(DotName clazz);

    @Message(id = 18046, value = "Annotation %s in class %s is only allowed on classes")
    String invalidAnnotationLocation(Object annotation, AnnotationTarget classInfo);

    @Message(id = 18047, value = "Thread local injection container not set")
    IllegalStateException noThreadLocalInjectionContainer();

    @Message(id = 18048, value = "Instance creation failed")
    RuntimeException instanceCreationFailed(@Cause Throwable t);

    @Message(id = 18049, value = "Instance destruction failed")
    RuntimeException instanceDestructionFailed(@Cause Throwable t);

    @Message(id = 18051, value = "Authentication Manager has not been set")
    IllegalStateException noAuthenticationManager();

    @Message(id = 18052, value = "Authorization Manager has not been set")
    IllegalStateException noAuthorizationManager();

    @Message(id = 18053, value = "No security context found")
    IllegalStateException noSecurityContext();

    @Message(id = 18054, value = "Principal class %s is not a subclass of GenericPrincipal")
    IllegalStateException illegalPrincipalType(Class<?> clazz);

    @Message(id = 18055, value = "Security context creation failed")
    RuntimeException failToCreateSecurityContext(@Cause Throwable t);

    @Message(id = 18056, value = "Catalina Context is null while creating JACC permissions")
    IllegalStateException noCatalinaContextForJacc();

    @Message(id = 18096, value = "Error instantiating container component: %s")
    String failToCreateContainerComponentInstance(String className);

    @Message(id = 18097, value = "TLD file %s not contained in root %s")
    String tldFileNotContainedInRoot(String tldPath, String rootPath);

    @Message(id = 18098, value = "Unknown metric %s")
    String unknownMetric(Object metric);

    @Message(id = 18100, value = "Timeout context service activation: %s")
    TimeoutException timeoutContextActivation(ServiceName service);

    @Message(id = 18101, value = "Version 1.1.0 of the web subsystem had a bug meaning referencing virtual-server from connector is not supported. See https://issues.jboss.org/browse/JBPAPP-9314")
    String transformationVersion_1_1_0_JBPAPP_9314();

    @Message(id = 18102, value = "Error loading SCI from module: %s")
    DeploymentUnitProcessingException errorLoadingSCIFromModule(ModuleIdentifier identifier, @Cause Exception e);

    @Message(id = 18103, value = "Unable to resolve annotation index for deployment unit: %s")
    DeploymentUnitProcessingException unableToResolveAnnotationIndex(DeploymentUnit deploymentUnit);

    @Message(id = 18104, value = "Deployment error processing SCI for jar: %s")
    DeploymentUnitProcessingException errorProcessingSCI(String jar, @Cause Exception e);

    @Message(id = 18105, value = "Not applicable")
    RuntimeException notApplicable();*/

    @Message(id = 18106, value = "Param-name and param-value are required to add parameter")
    String paramNameAndParamValueRequiredForAddParam();

//    @Message(id = 18107, value = "Param-name is required to remove parameter")
//    String paramNameRequiredForRemoveParam();
}
