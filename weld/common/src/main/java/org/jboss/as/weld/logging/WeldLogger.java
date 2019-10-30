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

package org.jboss.as.weld.logging;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.resources.spi.ClassFileInfoException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("deprecation")
@MessageLogger(projectCode = "WFLYWELD", length = 4)
public interface WeldLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    WeldLogger ROOT_LOGGER = Logger.getMessageLogger(WeldLogger.class, "org.jboss.as.weld");

    /**
     * A logger with the category {@code org.jboss.weld}.
     */
    WeldLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(WeldLogger.class, "org.jboss.weld.deployer");


    @LogMessage(level= Logger.Level.ERROR)
    @Message(id = 1, value = "Failed to setup Weld contexts")
    void failedToSetupWeldContexts(@Cause Throwable throwable);

    @LogMessage(level= Logger.Level.ERROR)
    @Message(id = 2, value = "Failed to tear down Weld contexts")
    void failedToTearDownWeldContexts(@Cause Throwable throwable);

    @LogMessage(level= Logger.Level.INFO)
    @Message(id = 3, value = "Processing weld deployment %s")
    void processingWeldDeployment(String deployment);

//    @LogMessage(level = Logger.Level.WARN)
//    @Message(id = 4, value = "Found beans.xml file in non-standard location: %s, war deployments should place beans.xml files into WEB-INF/beans.xml")
//    void beansXmlInNonStandardLocation(String location);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 5, value = "Could not find BeanManager for deployment %s")
    void couldNotFindBeanManagerForDeployment(String beanManager);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 6, value = "Starting Services for CDI deployment: %s")
    void startingServicesForCDIDeployment(String deploymentName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 7, value = "Could not load portable extension class %s")
    void couldNotLoadPortableExceptionClass(String className, @Cause Throwable throwable);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 8, value = "@Resource injection of type %s is not supported for non-ejb components. Injection point: %s")
    void injectionTypeNotValue(String type, Member injectionPoint);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 9, value = "Starting weld service for deployment %s")
    void startingWeldService(String deploymentName);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 10, value = "Stopping weld service for deployment %s")
    void stoppingWeldService(String deploymentName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 11, value = "Warning while parsing %s:%s %s")
    void beansXmlValidationWarning(URL file, int line , String message);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 12, value = "Warning while parsing %s:%s %s")
    void beansXmlValidationError(URL file, int line , String message);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 13, value = "Deployment %s contains CDI annotations but no bean archive was found (no beans.xml or class with bean defining annotations was present).")
    void cdiAnnotationsButNotBeanArchive(String deploymentUnit);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 14, value = "Exception tearing down thread state")
    void exceptionClearingThreadState(@Cause Exception e);

//    @LogMessage(level = Logger.Level.ERROR)
//    @Message(id = 15, value = "Error loading file %s")
//    void errorLoadingFile(String newPath);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 16, value = "Could not read entries")
    void couldNotReadEntries(@Cause IOException ioe);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 17, value = "URL scanner does not understand the URL protocol %s, CDI beans will not be scanned.")
    void doNotUnderstandProtocol(URL url);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 18, value = "Found both WEB-INF/beans.xml and WEB-INF/classes/META-INF/beans.xml. It is not portable to use both locations at the same time. Weld is going to use the former location for this deployment.")
    void duplicateBeansXml();

    @Message(id = 19, value = "Could get beans.xml file as URL when processing file: %s")
    DeploymentUnitProcessingException couldNotGetBeansXmlAsURL(String beansXml, @Cause Throwable cause);

    @Message(id = 20, value = "Could not load interceptor class : %s")
    DeploymentUnitProcessingException couldNotLoadInterceptorClass(String interceptorClass, @Cause Throwable cause);

    @Message(id = 21, value = "Service %s didn't implement the javax.enterprise.inject.spi.Extension interface")
    DeploymentUnitProcessingException extensionDoesNotImplementExtension(Class<?> clazz);

    @Message(id = 22, value = "View of type %s not found on EJB %s")
    IllegalArgumentException viewNotFoundOnEJB(String viewType, String ejb);

    // @Message(id = 23, value = "EJB has been removed")
    // NoSuchEJBException ejbHashBeenRemoved();

//    @Message(id = 24, value = "Failed to perform CDI injection of field: %s on %s")
//    RuntimeException couldNotInjectField(Field field, Class<?> beanClass, @Cause Throwable cause);
//
//    @Message(id = 25, value = "Failed to perform CDI injection of method: %s on %s")
//    RuntimeException couldNotInjectMethod(Method method, Class<?> beanClass, @Cause Throwable cause);
//
//    @Message(id = 26, value = "Class %s has more that one constructor annotated with @Inject")
//    RuntimeException moreThanOneBeanConstructor(Class<?> beanClass);
//
//    @Message(id = 27, value = "Component %s is attempting to inject the InjectionPoint into a field: %s")
//    RuntimeException attemptingToInjectInjectionPointIntoField(Class<?> clazz, Field field);
//
//    @Message(id = 28, value = "Could not resolve CDI bean for injection point %s with qualifiers %s")
//    RuntimeException couldNotResolveInjectionPoint(String injectionPoint, Set<Annotation> qualifier);
//
//    @Message(id = 29, value = "Component %s is attempting to inject the InjectionPoint into a method on a component that is not a CDI bean %s")
//    RuntimeException attemptingToInjectInjectionPointIntoNonBean(Class<?> componentClass, Method injectionPoint);

    @Message(id = 30, value = "Unknown interceptor class for CDI injection %s")
    IllegalArgumentException unknownInterceptorClassForCDIInjection(Class<?> interceptorClass);

    @Message(id = 31, value = "%s cannot be null")
    IllegalArgumentException parameterCannotBeNull(String param);

    @Message(id = 32, value = "Injection point represents a method which doesn't follow JavaBean conventions (must have exactly one parameter) %s")
    IllegalArgumentException injectionPointNotAJavabean(Method method);

    @Message(id = 33, value = "%s annotation not found on %s")
    IllegalArgumentException annotationNotFound(Class<? extends Annotation> type, Member member);

    @Message(id = 34, value = "Could not resolve @EJB injection for %s on %s")
    IllegalStateException ejbNotResolved(Object ejb, Member member);

    @Message(id = 35, value = "Resolved more than one EJB for @EJB injection of %s on %s. Found %s")
    IllegalStateException moreThanOneEjbResolved(Object ejb, Member member, final Set<ViewDescription> viewService);

    @Message(id = 36, value = "Could not determine bean class from injection point type of %s")
    IllegalArgumentException couldNotDetermineUnderlyingType(Type type);

    @Message(id = 37, value = "Error injecting persistence unit into CDI managed bean. Can't find a persistence unit named '%s' in deployment %s for injection point %s")
    IllegalArgumentException couldNotFindPersistenceUnit(String unitName, String deployment, Member injectionPoint);

    @Message(id = 38, value = "Could not inject SecurityManager, security is not enabled")
    IllegalStateException securityNotEnabled();

    @Message(id = 39, value = "Singleton not set for %s. This means that you are trying to access a weld deployment with a Thread Context ClassLoader that is not associated with the deployment.")
    IllegalStateException singletonNotSet(ClassLoader classLoader);

    @Message(id = 40, value = "%s is already running")
    IllegalStateException alreadyRunning(String object);

    @Message(id = 41, value = "%s is not started")
    IllegalStateException notStarted(String object);

    // @Message(id = 42, value = "services cannot be added after weld has started")
    // IllegalStateException cannotAddServicesAfterStart();

    @Message(id = 43, value = "BeanDeploymentArchive with id %s not found in deployment")
    IllegalArgumentException beanDeploymentNotFound(String beanDeploymentId);

    @Message(id = 44, value = "Error injecting resource into CDI managed bean. Can't find a resource named %s")
    IllegalArgumentException couldNotFindResource(String resourceName, @Cause Throwable cause);

    @Message(id = 45, value = "Cannot determine resource name. Both jndiName and mappedName are null")
    IllegalArgumentException cannotDetermineResourceName();

    @Message(id = 46, value = "Cannot inject injection point %s")
    IllegalArgumentException cannotInject(InjectionPoint ip);

    @Message(id = 47, value = "%s cannot be used at runtime")
    IllegalStateException cannotUseAtRuntime(String description);

    @Message(id = 48, value = "These attributes must be 'true' for use with CDI 1.0 '%s'")
    String rejectAttributesMustBeTrue(Set<String> keySet);

    @Message(id = 49, value = "Error injecting resource into CDI managed bean. Can't find a resource named %s defined on %s")
    IllegalArgumentException couldNotFindResource(String resourceName, String member, @Cause Throwable cause);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(value = "Discovered %s")
    void beanArchiveDiscovered(BeanDeploymentArchive bda);

    @Message(id = 50, value = "%s was not found in composite index")
    ClassFileInfoException nameNotFoundInIndex(String name);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = Message.NONE, value = "Unable to load annotation %s")
    void unableToLoadAnnotation(String annotationClassName);

    @Message(id = 51, value = "Cannot load %s")
    ClassFileInfoException cannotLoadClass(String name, @Cause Throwable throwable);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 52, value = "Using deployment classloader to load proxy classes for module %s. Package-private access will not work. To fix this the module should declare dependencies on %s")
    void loadingProxiesUsingDeploymentClassLoader(ModuleIdentifier moduleIdentifier, String dependencies);

    @Message(id = 53, value = "Component interceptor support not available for: %s")
    IllegalStateException componentInterceptorSupportNotAvailable(Object componentClass);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 54, value = "Could not read provided index of an external bean archive: %s")
    void cannotLoadAnnotationIndexOfExternalBeanArchive(Object indexUrl);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 55, value = "Could not index class [%s] from an external bean archive: %s")
    void cannotIndexClassName(Object name, Object bda);

    @Message(id = 56, value = "Weld is not initialized yet")
    IllegalStateException weldNotInitialized();

}
