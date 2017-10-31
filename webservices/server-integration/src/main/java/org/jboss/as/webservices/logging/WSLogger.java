/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.logging;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.xml.ws.WebServiceException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.config.DisabledOperationException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.WSFException;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
@MessageLogger(projectCode = "WFLYWS", length = 4)
public interface WSLogger extends BasicLogger {

    WSLogger ROOT_LOGGER = Logger.getMessageLogger(WSLogger.class, "org.jboss.as.webservices");

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Cannot load WS deployment aspects from %s")
    void cannotLoadDeploymentAspectsDefinitionFile(String resourcePath);

    RuntimeException cannotLoadDeploymentAspectsDefinitionFile(@Cause Throwable cause, String resourcePath);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Activating WebServices Extension")
    void activatingWebservicesExtension();

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Starting %s")
    void starting(Object object);

    @LogMessage(level = INFO)
    @Message(id = 4, value = "Stopping %s")
    void stopping(Object object);

    @LogMessage(level = FATAL)
    @Message(id = 5, value = "Error while creating configuration service")
    void configServiceCreationFailed();

    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Error while destroying configuration service")
    void configServiceDestroyFailed();

    @LogMessage(level = WARN)
    @Message(id = 7, value = "Could not read WSDL from: %s")
    void cannotReadWsdl(String wsdlLocation);

    @LogMessage(level = WARN)
    @Message(id = 8, value = "[JAXWS 2.2 spec, section 7.7] The @WebService and @WebServiceProvider annotations are mutually exclusive - %s won't be considered as a webservice endpoint, since it doesn't meet that requirement")
    void mutuallyExclusiveAnnotations(String className);

    @LogMessage(level = WARN)
    @Message(id = 9, value = "WebService endpoint class cannot be final - %s won't be considered as a webservice endpoint")
    void finalEndpointClassDetected(String className);

    @LogMessage(level = WARN)
    @Message(id = 10, value = "Ignoring <port-component-ref> without <service-endpoint-interface> and <port-qname>: %s")
    void ignoringPortComponentRef(Object o);

    @LogMessage(level = ERROR)
    @Message(id = 11, value = "Cannot register record processor in JMX server")
    void cannotRegisterRecordProcessor();

    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Cannot unregister record processor from JMX server")
    void cannotUnregisterRecordProcessor();

    @LogMessage(level = INFO)
    @Message(id = 13, value = "MBeanServer not available, skipping registration/unregistration of %s")
    void mBeanServerNotAvailable(Object bean);

    @LogMessage(level = WARN)
    @Message(id = 14, value = "Multiple EJB3 endpoints in the same deployment with different declared security roles; be aware this might be a security risk if you're not controlling allowed roles (@RolesAllowed) on each ws endpoint method.")
    void multipleEndpointsWithDifferentDeclaredSecurityRoles();

    @LogMessage(level = ERROR)
    @Message(id = 15, value = "Cannot register endpoint: %s in JMX server")
    void cannotRegisterEndpoint(Object endpoint);

    @LogMessage(level = ERROR)
    @Message(id = 16, value = "Cannot unregister endpoint: %s from JMX server")
    void cannotUnregisterEndpoint(Object endpoint);

    @LogMessage(level = WARN)
    @Message(id = 17, value = "Invalid handler chain file: %s")
    void invalidHandlerChainFile(String fileName);

    String WS_SPEC_REF_5_3_2_4_2 = ". See section 5.3.2.4.2 of \"Web Services for Java EE, Version 1.4\".";

    @LogMessage(level = ERROR)
    @Message(id = 18, value = "Web service method %s must not be static or final" + WS_SPEC_REF_5_3_2_4_2)
    void webMethodMustNotBeStaticOrFinal(Method staticWebMethod);

    @LogMessage(level = ERROR)
    @Message(id = 19, value = "Web service method %s must be public" + WS_SPEC_REF_5_3_2_4_2)
    void webMethodMustBePublic(Method staticWebMethod);

    @LogMessage(level = ERROR)
    @Message(id = 20, value = "Web service implementation class %s does not contain method %s")
    void webServiceMethodNotFound(Class<?> endpointClass, Method potentialWebMethod);

    @LogMessage(level = ERROR)
    @Message(id = 21, value = "Web service implementation class %s does not contain an accessible method %s")
    void accessibleWebServiceMethodNotFound(Class<?> endpointClass, Method potentialWebMethod, @Cause SecurityException e);

    @LogMessage(level = ERROR)
    @Message(id = 22, value = "Web service implementation class %s may not declare a finalize() method"
            + WS_SPEC_REF_5_3_2_4_2)
    void finalizeMethodNotAllowed(Class<?> seiClass);

    @Message(id = 23, value = "Null endpoint name")
    NullPointerException nullEndpointName();

    @Message(id = 24, value = "Null endpoint class")
    NullPointerException nullEndpointClass();

    @Message(id = 25, value = "Cannot resolve module or classloader for deployment %s")
    IllegalStateException classLoaderResolutionFailed(Object o);

    @Message(id = 26, value = "Handler chain config file %s not found in %s")
    WebServiceException missingHandlerChainConfigFile(String filePath, ResourceRoot resourceRoot);

    @Message(id = 27, value = "Unexpected element: %s")
    IllegalStateException unexpectedElement(String elementName);

    @Message(id = 28, value = "Unexpected end tag: %s")
    IllegalStateException unexpectedEndTag(String tagName);

    @Message(id = 29, value = "Reached end of xml document unexpectedly")
    IllegalStateException unexpectedEndOfDocument();

    @Message(id = 30, value = "Could not find class attribute for deployment aspect")
    IllegalStateException missingDeploymentAspectClassAttribute();

    @Message(id = 31, value = "Could not create a deployment aspect of class: %s")
    IllegalStateException cannotInstantiateDeploymentAspect(@Cause Throwable cause, String className);

    @Message(id = 32, value = "Could not find property name attribute for deployment aspect: %s")
    IllegalStateException missingPropertyNameAttribute(DeploymentAspect deploymentAspect);

    @Message(id = 33, value = "Could not find property class attribute for deployment aspect: %s")
    IllegalStateException missingPropertyClassAttribute(DeploymentAspect deploymentAspect);

    @Message(id = 34, value = "Unsupported property class: %s")
    IllegalArgumentException unsupportedPropertyClass(String className);

    @Message(id = 35, value = "Could not create list of type: %s")
    IllegalStateException cannotInstantiateList(@Cause Throwable cause, String className);

    @Message(id = 36, value = "Could not create map of type: %s")
    IllegalStateException cannotInstantiateMap(@Cause Throwable cause, String className);

    @Message(id = 37, value = "No metrics available")
    String noMetricsAvailable();

    @Message(id = 38, value = "Cannot find component view: %s")
    IllegalStateException cannotFindComponentView(ServiceName viewName);

    @Message(id = 39, value = "Child '%s' not found for VirtualFile: %s")
    IOException missingChild(String child, VirtualFile file);

    @Message(id = 40, value = "Failed to create context")
    Exception createContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 41, value = "Failed to start context")
    Exception startContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 42, value = "Failed to stop context")
    Exception stopContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 43, value = "Failed to destroy context")
    Exception destroyContextPhaseFailed(@Cause Throwable cause);

    @Message(id = 44, value = "Cannot create servlet delegate: %s")
    IllegalStateException cannotInstantiateServletDelegate(@Cause Throwable cause, String className);

    @Message(id = 45, value = "Cannot obtain deployment property: %s")
    IllegalStateException missingDeploymentProperty(String propertyName);

    @Message(id = 46, value = "Multiple security domains not supported. First domain: '%s' second domain: '%s'")
    IllegalStateException multipleSecurityDomainsDetected(String firstDomain, String secondDomain);

    @Message(id = 47, value = "Web Service endpoint %s with URL pattern %s is already registered. Web service endpoint %s is requesting the same URL pattern.")
    IllegalArgumentException sameUrlPatternRequested(String firstClass, String urlPattern, String secondClass);

    @Message(id = 48, value = "@WebServiceRef injection target is invalid.  Only setter methods are allowed: %s")
    DeploymentUnitProcessingException invalidServiceRefSetterMethodName(Object o);

    @Message(id = 49, value = "@WebServiceRef attribute 'name' is required for class level annotations.")
    DeploymentUnitProcessingException requiredServiceRefName();

    @Message(id = 50, value = "@WebServiceRef attribute 'type' is required for class level annotations.")
    DeploymentUnitProcessingException requiredServiceRefType();

    @Message(id = 51, value = "Config %s doesn't exist")
    OperationFailedException missingConfig(String configName);

    @Message(id = 52, value = "Unsupported handler chain type: %s. Supported types are either %s or %s")
    StartException wrongHandlerChainType(String unknownChainType, String knownChainType1, String knownChainType2);

//    @Message(id = 53, value = "Cannot add new handler chain of type %s with id %s. This id is already used in config %s for another chain.")
//    StartException multipleHandlerChainsWithSameId(String chainType, String handlerChainId, String configId);

    @Message(id = 54, value = "Config %s: %s handler chain with id %s doesn't exist")
    OperationFailedException missingHandlerChain(String configName, String handlerChainType, String handlerChainId);

    // @Message(id = 55, value = "Config %s, %s handler chain %s: doesn't contain handler with name %s")
    // OperationFailedException missingHandler(String configName, String handlerChainType, String handlerChainId, String handlerName);

    //@LogMessage(level = ERROR)
    //@Message(id = 56, value = "Method invocation failed with exception: %s")
    //void methodInvocationFailed(@Cause Throwable cause, String message);

    @Message(id = 57, value = "Unable to get URL for: %s")
    DeploymentUnitProcessingException cannotGetURLForDescriptor(@Cause Throwable cause, String resourcePath);

    @Message(id = 58, value = "JAX-RPC not supported")
    DeploymentUnitProcessingException jaxRpcNotSupported();

    @Message(id = 59, value = "%s library (%s) detected in ws endpoint deployment; either provide a proper deployment replacing embedded libraries with container module "
            + "dependencies or disable the webservices subsystem for the current deployment adding a proper jboss-deployment-structure.xml descriptor to it. "
            + "The former approach is recommended, as the latter approach causes most of the webservices Java EE and any JBossWS specific functionality to be disabled.")
    DeploymentUnitProcessingException invalidLibraryInDeployment(String libraryName, String jar);

    @Message(id = 60, value = "Web service endpoint class %s not found")
    DeploymentUnitProcessingException endpointClassNotFound(String endpointClassName);

    @Message(id = 61, value = "The endpointInterface %s declared in the @WebService annotation on web service implementation bean %s was not found.")
    DeploymentUnitProcessingException declaredEndpointInterfaceClassNotFound(String endpointInterface, Class<?> endpointClass);

    @Message(id = 62, value = "Class verification of Java Web Service implementation class %s failed.")
    DeploymentUnitProcessingException jwsWebServiceClassVerificationFailed(Class<?> seiClass);

    @Message(id = 63, value = "Could not update WS server configuration because of pending former model update(s) requiring reload.")
    DisabledOperationException couldNotUpdateServerConfigBecauseOfReloadRequired();

    @Message(id = 64, value = "Could not update WS server configuration because of existing WS deployment on the server.")
    DisabledOperationException couldNotUpdateServerConfigBecauseOfExistingWSDeployment();

    @LogMessage(level = WARN)
    @Message(id = 65, value = "Annotation '@%s' found on class '%s'. Perhaps you forgot to add a '%s' module dependency to your deployment?")
    void missingModuleDependency(String annotation, String clazz, String module);

    @Message(id = 66, value = "Servlet class %s declared in web.xml; either provide a proper deployment relying on JBossWS or disable the webservices subsystem for the "
            + "current deployment adding a proper jboss-deployment-structure.xml descriptor to it. "
            + "The former approach is recommended, as the latter approach causes most of the webservices Java EE and any JBossWS specific functionality to be disabled.")
    WSFException invalidWSServlet(String servletClass);

    @LogMessage(level = ERROR)
    @Message(id = 67, value = "Could not activate the webservices subsystem.")
    void couldNotActivateSubsystem(@Cause Throwable cause);

    @Message(id = 68, value = "Service %s not available")
    OperationFailedException serviceNotAvailable(String serviceName);

//    @Message(id = 69, value = "String format password is required")
//    IllegalArgumentException invalidPasswordType();

    @LogMessage(level = DEBUG)
    @Message(id = 70, value = "Authorization failed for user: %s")
    void failedAuthorization(String username);

    @LogMessage(level = DEBUG)
    @Message(id = 71, value = "Failed to authenticate username %s:, incorrect username/password")
    void failedAuthentication(final String username);

    @LogMessage(level = DEBUG)
    @Message(id = 72, value = "Error occured when authenticate username %s. Exception message: %s")
    void failedAuthenticationWithException(@Cause final Throwable cause, final String username, final String message);

    @Message(id = 73, value = "The target endpoint %s is undeploying or stopped" )
    IllegalStateException endpointAlreadyStopped(String endpointName);
}
