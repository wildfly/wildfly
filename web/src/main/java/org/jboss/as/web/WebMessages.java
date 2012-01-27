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

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.vfs.VirtualFile;

/**
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

    @Message(id = 18011, value = "The welcome root can not be enabled on a host that has a default web module")
    String noWelcomeWebappWithDefaultWebModule();

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
    String failedToResolveModule(VirtualFile deploymentRoot);

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
    String invalidRelativeOrderingUnkownName(String jar);

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

    @Message(id = 18050, value = "@ManagedBean is only allowed at class level %s")
    String invalidManagedBeanAnnotation(AnnotationTarget target);

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

    @Message(id = 18057, value = "This session manager does not support session passivation")
    String noSessionPassivation();

    @Message(id = 18058, value = "Session is already expired")
    IllegalStateException expiredSession();

    @Message(id = 18059, value = "Specified manager does not implement ClusteredManager")
    IllegalArgumentException invalidManager();

    @Message(id = 18060, value = "Exception acquiring ownership of %s")
    RuntimeException failAcquiringOwnership(String id, @Cause Throwable t);

    @Message(id = 18061, value = "Interruped acquiring ownership of %s")
    RuntimeException interruptedAcquiringOwnership(String id, @Cause Throwable t);

    @Message(id = 18062, value = "Specified attribute cannot be replicated")
    IllegalArgumentException failToReplicateAttribute();

    @Message(id = 18063, value = "Error calling value bound session listener")
    String errorValueBoundEvent(@Cause Throwable t);

    @Message(id = 18064, value = "Error calling value unbound session listener")
    String errorValueUnboundEvent(@Cause Throwable t);

    @Message(id = 18065, value = "Error calling session attribute listener")
    String errorSessionAttributeEvent(@Cause Throwable t);

    @Message(id = 18066, value = "Session data is null")
    String nullSessionData();

    @Message(id = 18067, value = "Error calling session listener")
    String errorSessionEvent(@Cause Throwable t);

    @Message(id = 18068, value = "Error calling session activation listener")
    String errorSessionActivationEvent(@Cause Throwable t);

    @Message(id = 18069, value = "DistributedCacheManager is null")
    IllegalStateException nullDistributedCacheManager();

    @Message(id = 18070, value = "Session manager is null")
    String nullManager();

    @Message(id = 18071, value = "Fail to start batch transaction")
    String failToStartBatchTransaction(@Cause Throwable t);

    @Message(id = 18072, value = "Unable to start manager")
    String failToStartManager();

    @Message(id = 18073, value = "Invalid snapshot mode specified")
    IllegalArgumentException invalidSnapshotMode();

    @Message(id = 18074, value = "Failed to instantiate %s %s")
    RuntimeException failToCreateSessionNotificationPolicy(String className, String policyClass, @Cause Throwable t);

    @Message(id = 18075, value = "Number of active sessions exceeds limit %s trying to create session %s")
    IllegalStateException tooManyActiveSessions(int limit, String id);

    @Message(id = 18076, value = "Exception expiring or passivating sesion %s")
    String errorPassivatingSession(String id);

    @Message(id = 18077, value = "Failed to load session %s for passivation")
    String failToPassivateLoad(String id);

    @Message(id = 18078, value = "Failed to unload session %s for passivation")
    String failToPassivateUnloaded(String id);

    @Message(id = 18079, value = "Failed to passivate %s session %s")
    String failToPassivate(String unloaded, String id);

    @Message(id = 18080, value = "Standard expiration of session %s failed; switching to a brute force cleanup. Problem is %s")
    String bruteForceCleanup(String id, String t);

    @Message(id = 18081, value = "Recieved notification for inactive session %s")
    String notificationForInactiveSession(String id);

    @Message(id = 18082, value = "Caught exception during brute force cleanup of unloaded session %s  Session will be removed from Manager but may still exist in distributed cache")
    String failToBruteForceCleanup(String id);

    @Message(id = 18083, value = "Failed to replicate session")
    RuntimeException failedSessionReplication(@Cause Throwable t);

    @Message(id = 18084, value = "Caught exception rolling back transaction")
    String exceptionRollingBackTransaction();

    @Message(id = 18085, value = "Expected clustered session, but got a %s")
    IllegalArgumentException invalidSession(String className);

    @Message(id = 18086, value = "Null owned session update")
    String nullOsu();

    @Message(id = 18087, value = "Null session id")
    String nullSessionId();

    @Message(id = 18088, value = "Null session")
    String nullSession();

    @Message(id = 18089, value = "Failed to replicate session %s")
    String failedSessionReplication(String id);

    @Message(id = 18090, value = "Failed to queue replicate session %s")
    String failedQueueingSessionReplication(Object session);

    @Message(id = 18091, value = "Exception storing session %s")
    String failedToStoreSession(String id);

    @Message(id = 18092, value = "Exception sprocessing sessions")
    String exceptionProcessingSessions();

    @Message(id = 18093, value = "Null real id")
    IllegalArgumentException nullRealId();

    @Message(id = 18094, value = "Clustered SSO valve is already started")
    String valveAlreadyStarted();

    @Message(id = 18095, value = "Clustered SSO valve is not started")
    String valveNotStarted();

}
