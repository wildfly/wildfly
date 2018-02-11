/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYUT", length = 4)
public interface UndertowLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    UndertowLogger ROOT_LOGGER = Logger.getMessageLogger(UndertowLogger.class, "org.wildfly.extension.undertow");


    /*
    UNDERTOW messages start
     */

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 1, value = "Could not initialize JSP")
    void couldNotInitJsp(@Cause ClassNotFoundException e);

    // @LogMessage(level = ERROR)
    // @Message(id = 2, value = "Failed to purge EL cache.")
    // void couldNotPurgeELCache(@Cause Exception exception);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Undertow %s starting")
    void serverStarting(String version);

    @LogMessage(level = INFO)
    @Message(id = 4, value = "Undertow %s stopping")
    void serverStopping(String version);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Secure listener for protocol: '%s' not found! Using non secure port!")
    void secureListenerNotAvailableForPort(String protocol);

    /**
     * Creates an exception indicating the class, represented by the {@code className} parameter, cannot be accessed.
     *
     * @param name    name of the listener
     * @param address socket address
     */
    @LogMessage(level = INFO)
    @Message(id = 6, value = "Undertow %s listener %s listening on %s:%d")
    void listenerStarted(String type, String name, String address, int port);

    @LogMessage(level = INFO)
    @Message(id = 7, value = "Undertow %s listener %s stopped, was bound to %s:%d")
    void listenerStopped(String type, String name, String address, int port);

    @LogMessage(level = INFO)
    @Message(id = 8, value = "Undertow %s listener %s suspending")
    void listenerSuspend(String type, String name);

    @LogMessage(level = INFO)
    @Message(id = 9, value = "Could not load class designated by HandlesTypes [%s].")
    void cannotLoadDesignatedHandleTypes(ClassInfo classInfo, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 10, value = "Could not load web socket endpoint %s.")
    void couldNotLoadWebSocketEndpoint(String s, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 11, value = "Could not load web socket application config %s.")
    void couldNotLoadWebSocketConfig(String s, @Cause Exception e);

    @LogMessage(level = INFO)
    @Message(id = 12, value = "Started server %s.")
    void startedServer(String name);

    @LogMessage(level = WARN)
    @Message(id = 13, value = "Could not create redirect URI.")
    void invalidRedirectURI(@Cause Throwable cause);

    @LogMessage(level = INFO)
    @Message(id = 14, value = "Creating file handler for path '%s' with options [directory-listing: '%s', follow-symlink: '%s', case-sensitive: '%s', safe-symlink-paths: '%s']")
    void creatingFileHandler(String path, boolean directoryListing, boolean followSymlink, boolean caseSensitive, List<String> safePaths);

    // @LogMessage(level = TRACE)
    // @Message(id = 15, value = "registering handler %s under path '%s'")
    // void registeringHandler(HttpHandler value, String locationPath);

    @LogMessage(level = WARN)
    @Message(id = 16, value = "Could not resolve name in absolute ordering: %s")
    void invalidAbsoluteOrdering(String name);

    @LogMessage(level = WARN)
    @Message(id = 17, value = "Could not delete servlet temp file %s")
    void couldNotDeleteTempFile(File file);

    @LogMessage(level = INFO)
    @Message(id = 18, value = "Host %s starting")
    void hostStarting(String version);

    @LogMessage(level = INFO)
    @Message(id = 19, value = "Host %s stopping")
    void hostStopping(String version);

    @LogMessage(level = WARN)
    @Message(id = 20, value = "Clustering not supported, falling back to non-clustered session manager")
    void clusteringNotSupported();


    @LogMessage(level = INFO)
    @Message(id = 21, value = "Registered web context: '%s' for server '%s'")
    void registerWebapp(String webappPath, String serverName);

    @LogMessage(level = INFO)
    @Message(id = 22, value = "Unregistered web context: '%s' from server '%s'")
    void unregisterWebapp(String webappPath, String serverName);

    @LogMessage(level = INFO)
    @Message(id = 23, value = "Skipped SCI for jar: %s.")
    void skippedSCI(String jar, @Cause Exception e);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 24, value = "Failed to persist session attribute %s with value %s for session %s")
    void failedToPersistSessionAttribute(String attributeName, Object value, String sessionID, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 25, value = "Failed to register policy context handler for key %s")
    void failedToRegisterPolicyContextHandler(String key, @Cause Exception e);

    // @Message(id = 26, value = "Unknown handler '%s' encountered")
    // XMLStreamException unknownHandler(String name, @Param Location location);

    @Message(id = 27, value = "Failed to parse XML descriptor %s at [%s,%s]")
    String failToParseXMLDescriptor(String xmlFile, Integer line, Integer column);

    @Message(id = 28, value = "Failed to parse XML descriptor %s")
    String failToParseXMLDescriptor(String xmlFile);

    @Message(id = 29, value = "@WebServlet is only allowed at class level %s")
    String invalidWebServletAnnotation(AnnotationTarget target);

    @Message(id = 30, value = "@WebInitParam requires name and value on %s")
    String invalidWebInitParamAnnotation(AnnotationTarget target);

    @Message(id = 31, value = "@WebFilter is only allowed at class level %s")
    String invalidWebFilterAnnotation(AnnotationTarget target);

    @Message(id = 32, value = "@WebListener is only allowed at class level %s")
    String invalidWebListenerAnnotation(AnnotationTarget target);

    @Message(id = 33, value = "@RunAs needs to specify a role name on %s")
    String invalidRunAsAnnotation(AnnotationTarget target);

    @Message(id = 34, value = "@DeclareRoles needs to specify role names on %s")
    String invalidDeclareRolesAnnotation(AnnotationTarget target);

    @Message(id = 35, value = "@MultipartConfig is only allowed at class level %s")
    String invalidMultipartConfigAnnotation(AnnotationTarget target);

    @Message(id = 36, value = "@ServletSecurity is only allowed at class level %s")
    String invalidServletSecurityAnnotation(AnnotationTarget target);

    @Message(id = 37, value = "%s has the wrong component type, it cannot be used as a web component")
    RuntimeException wrongComponentType(String clazz);

    @Message(id = 38, value = "TLD file %s not contained in root %s")
    String tldFileNotContainedInRoot(String tldPath, String rootPath);

    @Message(id = 39, value = "Failed to resolve module for deployment %s")
    DeploymentUnitProcessingException failedToResolveModule(DeploymentUnit deploymentUnit);

    @Message(id = 40, value = "Duplicate others in absolute ordering")
    String invalidMultipleOthers();

    @Message(id = 41, value = "Invalid relative ordering")
    String invalidRelativeOrdering();

    @Message(id = 42, value = "Conflict occurred processing web fragment in JAR: %s")
    String invalidWebFragment(String jar);

    @Message(id = 43, value = "Relative ordering processing error with JAR: %s")
    String invalidRelativeOrdering(String jar);

    @Message(id = 44, value = "Ordering includes both before and after others in JAR: %s")
    String invalidRelativeOrderingBeforeAndAfter(String jar);

    @Message(id = 45, value = "Duplicate name declared in JAR: %s")
    String invalidRelativeOrderingDuplicateName(String jar);

    @LogMessage(level = WARN)
    @Message(id = 46, value = "Unknown web fragment name declared in JAR: %s")
    void invalidRelativeOrderingUnknownName(String jar);

    @Message(id = 47, value = "Relative ordering conflict with JAR: %s")
    String invalidRelativeOrderingConflict(String jar);

    @Message(id = 48, value = "Failed to process WEB-INF/lib: %s")
    String failToProcessWebInfLib(VirtualFile xmlFile);

    @Message(id = 49, value = "Error loading SCI from module: %s")
    DeploymentUnitProcessingException errorLoadingSCIFromModule(String identifier, @Cause Exception e);

    @Message(id = 50, value = "Unable to resolve annotation index for deployment unit: %s")
    DeploymentUnitProcessingException unableToResolveAnnotationIndex(DeploymentUnit deploymentUnit);

    @Message(id = 51, value = "Deployment error processing SCI for jar: %s")
    DeploymentUnitProcessingException errorProcessingSCI(String jar, @Cause Exception e);

    @Message(id = 52, value = "Security context creation failed")
    RuntimeException failToCreateSecurityContext(@Cause Throwable t);

    @Message(id = 53, value = "No security context found")
    IllegalStateException noSecurityContext();

    @Message(id = 54, value = "Unknown metric %s")
    String unknownMetric(Object metric);

    @Message(id = 55, value = "Null default host")
    IllegalArgumentException nullDefaultHost();

    @Message(id = 56, value = "Null host name")
    IllegalStateException nullHostName();

    @Message(id = 57, value = "Null parameter %s")
    IllegalArgumentException nullParamter(String id);

    @Message(id = 58, value = "Cannot activate context: %s")
    IllegalStateException cannotActivateContext(@Cause Throwable th, ServiceName service);

    @Message(id = 59, value = "Could not construct handler for class: %s. with parameters %s")
    RuntimeException cannotCreateHttpHandler(Class<?> handlerClass, ModelNode parameters, @Cause Throwable cause);

    @Message(id = 60, value = "Invalid persistent sessions directory %s")
    StartException invalidPersistentSessionDir(File baseDir);

    @Message(id = 61, value = "Failed to create persistent sessions dir %s")
    StartException failedToCreatePersistentSessionDir(File baseDir);

    @Message(id = 62, value = "Could not create log directory: %s")
    StartException couldNotCreateLogDirectory(Path directory, @Cause IOException e);

    @Message(id = 63, value = "Could not find the port number listening for protocol %s")
    IllegalStateException noPortListeningForProtocol(final String protocol);

    @Message(id = 64, value = "Failed to configure handler %s")
    RuntimeException failedToConfigureHandler(Class<?> handlerClass, @Cause Exception e);

    @Message(id = 65, value = "Handler class %s was not a handler or a wrapper")
    IllegalArgumentException handlerWasNotAHandlerOrWrapper(Class<?> handlerClass);

    @Message(id = 66, value = "Failed to configure handler %s")
    RuntimeException failedToConfigureHandlerClass(String handlerClass, @Cause Exception e);

    @Message(id = 67, value = "Servlet class not defined for servlet %s")
    IllegalArgumentException servletClassNotDefined(final String servletName);

    @LogMessage(level = ERROR)
    @Message(id = 68, value = "Error obtaining authorization helper")
    void noAuthorizationHelper(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 69, value = "Ignoring shared-session-config in jboss-all.xml in deployment %s. This entry is only valid in top level deployments.")
    void sharedSessionConfigNotInRootDeployment(String deployment);

    @Message(id = 70, value = "Could not load handler %s from %s module")
    RuntimeException couldNotLoadHandlerFromModule(String className, String moduleName, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 71, value = "No ALPN provider found, HTTP/2 will not be enabled. To remove this message set enable-http2 to false on the listener %s in the Undertow subsystem.")
    void alpnNotFound(String listener);

    @Message(id = 72, value = "Could not find configured external path %s")
    DeploymentUnitProcessingException couldNotFindExternalPath(File path);

    @Message(id = 73, value = "mod_cluster advertise socket binding requires multicast address to be set")
    StartException advertiseSocketBindingRequiresMulticastAddress();

    @LogMessage(level = ERROR)
    @Message(id = 74, value = "Could not find TLD %s")
    void tldNotFound(String location);

    @Message(id = 75, value = "Cannot register resource of type %s")
    IllegalArgumentException cannotRegisterResourceOfType(String type);

    @Message(id = 76, value = "Cannot remove resource of type %s")
    IllegalArgumentException cannotRemoveResourceOfType(String type);

    @LogMessage(level = ERROR)
    @Message(id = 78, value = "Failed to register management view for websocket %s at %s")
    void failedToRegisterWebsocket(Class endpoint, String path, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 77, value = "Error invoking secure response")
    void errorInvokingSecureResponse(@Cause Exception e);

    @Message(id = 79, value = "No SSL Context available from security realm '%s'. Either the realm is not configured for SSL, or the server has not been reloaded since the SSL config was added.")
    IllegalStateException noSslContextInSecurityRealm(String securityRealm);

    @LogMessage(level = WARN)
    @Message(id = 80, value = "Valves are no longer supported, %s is not activated.")
    void unsupportedValveFeature(String valve);

    @LogMessage(level = WARN)
    @Message(id = 81, value = "The deployment %s will not be distributable because this feature is disabled in web-fragment.xml of the module %s.")
    void distributableDisabledInFragmentXml(String deployment, String module);

    @Message(id = 82, value = "Could not start '%s' listener.")
    StartException couldNotStartListener(String name, @Cause IOException e);

    @Message(id = 83, value = "%s is not allowed to be null")
    String nullNotAllowed(String name);

    //@Message(id = 84, value = "There are no mechanisms available from the HttpAuthenticationFactory.")
    //IllegalStateException noMechanismsAvailable();

    //@Message(id = 85, value = "The required mechanism '%s' is not available in mechanisms %s from the HttpAuthenticationFactory.")
    //IllegalStateException requiredMechanismNotAvailable(String mechanismName, Collection<String> availableMechanisms);

    //@Message(id = 86, value = "No authentication mechanisms have been selected.")
    //IllegalStateException noMechanismsSelected();

    @Message(id = 87, value = "Duplicate default web module '%s' configured on server '%s', host '%s'")
    IllegalArgumentException duplicateDefaultWebModuleMapping(String defaultDeploymentName, String serverName, String hostName);

//    @LogMessage(level = WARN)
//    @Message(id = 88, value = "HTTP/2 will not be enabled as TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 is not enabled. You may need to install JCE to enable strong ciphers to allow HTTP/2 to function.")
//    void noStrongCiphers();

    @Message(id = 89, value = "Predicate %s was not valid, message was: %s")
    String predicateNotValid(String predicate, String error);

    @Message(id = 90, value = "Key alias %s does not exist in the configured key store")
    IllegalArgumentException missingKeyStoreEntry(String alias);

    @Message(id = 91, value = "Key store entry %s is not a private key entry")
    IllegalArgumentException keyStoreEntryNotPrivate(String alias);

    @Message(id = 92, value = "Credential alias %s does not exist in the configured credential store")
    IllegalArgumentException missingCredential(String alias);

    @Message(id = 93, value = "Credential %s is not a clear text password")
    IllegalArgumentException credentialNotClearPassword(String alias);

    @Message(id = 94, value = "Configuration option [%s] ignored when using Elytron subsystem")
    @LogMessage(level = WARN)
    void configurationOptionIgnoredWhenUsingElytron(String option);

    @Message(id = 95, value = "the path ['%s'] doesn't exist on file system")
    String unableAddHandlerForPath(String path);

    //@Message(id = 96, value = "Unable to obtain identity for name %s")
    //IllegalStateException unableToObtainIdentity(String name, @Cause Throwable cause);

    @Message(id = 97, value = "If http-upgrade is enabled, remoting worker and http(s) worker must be the same. Please adjust values if need be.")
    String workerValueInHTTPListenerMustMatchRemoting();

    @LogMessage(level = ERROR)
    @Message(id = 98, value = "Unexpected Authentication Error: %s")
    void unexceptedAuthentificationError(String errorMessage, @Cause Throwable t);

    @Message(id = 99, value = "Session manager not available")
    OperationFailedException sessionManagerNotAvailable();

    @Message(id = 100, value = "Session %s not found")
    OperationFailedException sessionNotFound(String sessionId);

    @LogMessage(level = WARN)
    @Message(id = 101, value = "Duplicate servlet mapping %s found")
    void duplicateServletMapping(String mapping);
}
