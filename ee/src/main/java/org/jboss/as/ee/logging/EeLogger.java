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

package org.jboss.as.ee.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentIsStoppedException;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYEE", length = 4)
public interface EeLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    EeLogger ROOT_LOGGER = Logger.getMessageLogger(EeLogger.class, "org.jboss.as.ee");

//    /**
//     * Logs a warning message indicating the transaction datasource, represented by the {@code className} parameter,
//     * could not be proxied and will not be enlisted in the transactions automatically.
//     *
//     * @param cause     the cause of the error.
//     * @param className the datasource class name.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 1, value = "Transactional datasource %s could not be proxied and will not be enlisted in transactions automatically")
//    void cannotProxyTransactionalDatasource(@Cause Throwable cause, String className);

    /**
     * Logs a warning message indicating the resource-env-ref could not be resolved.
     *
     * @param elementName the name of the element.
     * @param name        the name resource environment reference.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "Could not resolve %s %s")
    void cannotResolve(String elementName, String name);

//    /**
//     * Logs a warning message indicating the class path entry, represented by the {@code entry} parameter, was not found
//     * in the file.
//     *
//     * @param entry the class path entry.
//     * @param file  the file.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 3, value = "Class Path entry %s in %s does not point to a valid jar for a Class-Path reference.")
//    void classPathEntryNotAJar(String entry, VirtualFile file);

//    /**
//     * Logs a warning message indicating the class path entry in file may not point to a sub deployment.
//     *
//     * @param file the file.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 4, value = "Class Path entry in %s may not point to a sub deployment.")
//    void classPathEntryASubDeployment(VirtualFile file);

//    /**
//     * Logs a warning message indicating the class path entry, represented by the {@code entry} parameter, was not found
//     * in the file.
//     *
//     * @param entry the class path entry.
//     * @param file  the file.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 5, value = "Class Path entry %s in %s not found.")
//    void classPathEntryNotFound(String entry, VirtualFile file);

    /**
     * Logs a warning message indicating a failure to destroy the component instance.
     *
     * @param cause     the cause of the error.
     * @param component the component instance.
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "Failed to destroy component instance %s")
    void componentDestroyFailure(@Cause Throwable cause, ComponentInstance component);

    /**
     * Logs a warning message indicating the component is not being installed due to an exception.
     *
     * @param name  the name of the component.
     */
    @LogMessage(level = WARN)
    @Message(id = 7, value = "Not installing optional component %s due to an exception (enable DEBUG log level to see the cause)")
    void componentInstallationFailure(String name);

//    /**
//     * Logs a warning message indicating the property, represented by the {@code name} parameter, is be ignored due to
//     * missing on the setter method on the datasource class.
//     *
//     * @param name          the name of the property.
//     * @param methodName    the name of the method.
//     * @param parameterType the name of the parameter type.
//     * @param className     the name of the datasource class.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 8, value = "Ignoring property %s due to missing setter method: %s(%s) on datasource class: %s")
//    void ignoringProperty(String name, String methodName, String parameterType, String className);

    /**
     * Logs a warning message indicating the managed bean implementation class MUST NOT be an interface.
     *
     * @param sectionId the section id of the managed bean spec.
     * @param className the class name
     */
    @LogMessage(level = WARN)
    @Message(id = 9, value = "[Managed Bean spec, section %s] Managed bean implementation class MUST NOT be an interface - " +
            "%s is an interface, hence won't be considered as a managed bean.")
    void invalidManagedBeanAbstractOrFinal(String sectionId, String className);

    /**
     * Logs a warning message indicating the managed bean implementation class MUST NOT be abstract or final.
     *
     * @param sectionId the section id of the managed bean spec.
     * @param className the class name
     */
    @LogMessage(level = WARN)
    @Message(id = 10, value = "[Managed Bean spec, section %s] Managed bean implementation class MUST NOT be abstract or final - " +
            "%s won't be considered as a managed bean, since it doesn't meet that requirement.")
    void invalidManagedBeanInterface(String sectionId, String className);

    /**
     * Logs a warning message indicating an exception occurred while invoking the pre-destroy on the interceptor
     * component class, represented by the {@code component} parameter.
     *
     * @param cause     the cause of the error.
     * @param component the component.
     */
    @LogMessage(level = WARN)
    @Message(id = 11, value = "Exception while invoking pre-destroy interceptor for component class: %s")
    void preDestroyInterceptorFailure(@Cause Throwable cause, Class<?> component);

//    /**
//     * Logs a warning message indicating the transaction datasource, represented by the {@code className} parameter,
//     * will not be enlisted in the transaction as the transaction subsystem is not available.
//     *
//     * @param className the name of the datasource class.
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 12, value = "Transactional datasource %s will not be enlisted in the transaction as the transaction subsystem is not available")
//    void transactionSubsystemNotAvailable(String className);

    //@Message(id = 13, value = "Injection for a member with static modifier is only acceptable on application clients, ignoring injection for target %s")
    //void ignoringStaticInjectionTarget(InjectionTarget injectionTarget);

    @LogMessage(level = WARN)
    @Message(id = 14, value = "%s in subdeployment ignored. jboss-ejb-client.xml is only parsed for top level deployments.")
    void subdeploymentIgnored(String pathName);

    //@Message(id = 15, value = "Transaction started in EE Concurrent invocation left open, starting rollback to prevent leak.")
    //void rollbackOfTransactionStartedInEEConcurrentInvocation();

    //@Message(id = 16, value = "Failed to rollback transaction.")
    //void failedToRollbackTransaction(@Cause Throwable cause);

    //@Message(id = 17, value = "Failed to suspend transaction.")
    //void failedToSuspendTransaction(@Cause Throwable cause);

    //@Message(id = 18, value = "System error while checking for transaction leak in EE Concurrent invocation.")
    //void systemErrorWhileCheckingForTransactionLeak(@Cause Throwable cause);

    /**
     * Creates an exception indicating the alternate deployment descriptor specified for the module file could not be
     * found.
     *
     * @param deploymentDescriptor the alternate deployment descriptor.
     * @param moduleFile           the module file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 19, value = "Could not find alternate deployment descriptor %s specified for %s")
    DeploymentUnitProcessingException alternateDeploymentDescriptor(VirtualFile deploymentDescriptor, VirtualFile moduleFile);

    /**
     * Creates an exception indicating the annotation must provide the attribute.
     *
     * @param annotation the annotation.
     * @param attribute  the attribute.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 20, value = "%s annotations must provide a %s.")
    IllegalArgumentException annotationAttributeMissing(String annotation, String attribute);

    /**
     * Creates an exception indicating more items cannot be added once getSortedItems() has been called.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 21, value = "Cannot add any more items once getSortedItems() has been called")
    IllegalStateException cannotAddMoreItems();

    /**
     * Creates an exception indicating the {@code name} cannot be empty.
     *
     * @param name the name that cannot be empty.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 22, value = "%s may not be empty")
    RuntimeException cannotBeEmpty(String name);

    /**
     * Creates an exception indicating the {@code name} cannot be {@code null} or empty.
     *
     * @param name  the name that cannot be empty.
     * @param value the value of the object.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 23, value = "%s cannot be null or empty: %s")
    IllegalArgumentException cannotBeNullOrEmpty(String name, Object value);

    /**
     * Creates an exception indicating the component, represented by the {@code name} parameter, could not be
     * configured.
     *
     * @param cause the cause of the error.
     * @param name  the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 24, value = "Could not configure component %s")
    DeploymentUnitProcessingException cannotConfigureComponent(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the type for the resource-env-ref could not be determined.
     *
     * @param name the name of the of the resource environment reference.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 25, value = "Could not determine type for resource-env-ref %s")
    DeploymentUnitProcessingException cannotDetermineType(String name);

    /**
     * Creates an exception indicating the type for the {@code tag} could not be determined.
     *
     * @param tag     the tag name.
     * @param value   the value of the tag.
     * @param typeTag the type tag.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 26, value = "Could not determine type for %s %s please specify the %s.")
    DeploymentUnitProcessingException cannotDetermineType(String tag, String value, String typeTag);

    /**
     * Creates an exception indicating the injection target referenced in the env-entry injection point could not be
     * loaded.
     *
     * @param injectionTarget the injection target.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 27, value = "Could not load %s referenced in env-entry")
    DeploymentUnitProcessingException cannotLoad(String injectionTarget);

    /**
     * Creates an exception indicating the injection target referenced in the env-entry injection point could not be
     * loaded.
     *
     * @param cause           the cause of the error.
     * @param injectionTarget the injection target.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    DeploymentUnitProcessingException cannotLoad(@Cause Throwable cause, String injectionTarget);

    /**
     * Creates an exception indicating an interceptor class could not be loaded.
     *
     * @param cause     the cause of the error.
     * @param className the name of the interceptor class.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 28, value = "Could not load interceptor class %s")
    RuntimeException cannotLoadInterceptor(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating an interceptor class could not be loaded on the component.
     *
     * @param cause     the cause of the error.
     * @param className the name of the interceptor class.
     * @param component the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 29, value = "Could not load interceptor class %s on component %s")
    DeploymentUnitProcessingException cannotLoadInterceptor(@Cause Throwable cause, String className, Class<?> component);

    /**
     * Creates an exception indicating the view class, represented by the {@code className} parameter, for the
     * component.
     *
     * @param cause     the cause of the error.
     * @param className the name of the class.
     * @param component the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 30, value = "Could not load view class %s for component %s")
    DeploymentUnitProcessingException cannotLoadViewClass(@Cause Throwable cause, String className, ComponentConfiguration component);

    /**
     * Creates an exception indicating the inability to process modules in the application.xml for the EAR, represented
     * by the {@code earFile} parameter, module file was not found.
     *
     * @param earFile    the EAR file.
     * @param moduleFile the module file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 31, value = "Unable to process modules in application.xml for EAR [%s], module file %s not found")
    DeploymentUnitProcessingException cannotProcessEarModule(VirtualFile earFile, String moduleFile);

    /**
     * Creates an exception indicating the inability to parse the resource-ref URI.
     *
     * @param cause the cause of the error.
     * @param uri   the URI.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 32, value = "Unable to parse resource-ref URI: %s")
    DeploymentUnitProcessingException cannotParseResourceRefUri(@Cause Throwable cause, String uri);

    /**
     * Creates an exception indicating the injection point could not be resolved on the class specified in the web.xml.
     *
     * @param targetName the injection point name.
     * @param className  the class name.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 33, value = "Could not resolve injection point %s on class %s specified in web.xml")
    DeploymentUnitProcessingException cannotResolveInjectionPoint(String targetName, String className);

    /**
     * Creates an exception indicating the method could not be found on the class with the annotations.
     *
     * @param method      the method.
     * @param component   the class.
     * @param annotations the annotations.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 34, value = "Could not resolve method %s on class %s with annotations %s")
    RuntimeException cannotResolveMethod(MethodIdentifier method, Class<?> component, Collection<?> annotations);

//    /**
//     * Creates an exception indicating the property, represented by the {@code name} parameter, could not be set on the
//     * datasource class, represented by the {@code className} parameter.
//     *
//     * @param cause     the cause of the error.
//     * @param name      the name of the property.
//     * @param className the datasource class name.
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
//    @Message(id = 35, value = "Could not set property %s on datasource class %s")
//    RuntimeException cannotSetProperty(@Cause Throwable cause, String name, String className);

    /**
     * Creates an exception indicating both the {@code element1} and {@code element2} cannot be specified in an
     * environment entry.
     *
     * @param element1 the first element.
     * @param element2 the second element.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 36, value = "Cannot specify both a %s and a %s in an environment entry.")
    DeploymentUnitProcessingException cannotSpecifyBoth(String element1, String element2);

    /**
     * Creates an exception indicating a circular dependency is installing.
     *
     * @param bindingName the binding name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 37, value = "Circular dependency installing %s")
    IllegalArgumentException circularDependency(String bindingName);

    /**
     * Creates an exception indicating the annotation is only allowed on a class.
     *
     * @param annotation the annotation.
     * @param target     the annotation target.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 38, value = "%s annotation is only allowed on a class. %s is not a class.")
    DeploymentUnitProcessingException classOnlyAnnotation(String annotation, AnnotationTarget target);

//    /**
//     * Creates an exception indicating the annotation is only allowed on method or class targets.
//     *
//     * @param annotation the annotation.
//     *
//     * @return an {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 39, value = "@%s annotation is only allowed on methods and classes")
//    DeploymentUnitProcessingException classOrMethodOnlyAnnotation(DotName annotation);

    /**
     * Creates an exception indicating a component, represented by the {@code name} parameter, is already defined in
     * this module.
     *
     * @param name the name of the module.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 40, value = "A component named '%s' is already defined in this module")
    IllegalArgumentException componentAlreadyDefined(String name);

    /**
     * Creates an exception indicating the component class, represented by the {@code className} parameter, for the
     * component, represented by the {@code componentName} parameter, has errors.
     *
     * @param className     the class name.
     * @param componentName the name of the component.
     * @param errorMsg      the error message.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 41, value = "Component class %s for component %s has errors: %n%s")
    DeploymentUnitProcessingException componentClassHasErrors(String className, String componentName, String errorMsg);

    /**
     * Creates an exception indicating a failure to construct a component instance.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 42, value = "Failed to construct component instance")
    IllegalStateException componentConstructionFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating the component is stopped.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 43, value = "Component is stopped")
    ComponentIsStoppedException componentIsStopped();

    /**
     * Creates an exception indicating the component is not available.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 44, value = "Component not available (interrupted)")
    IllegalStateException componentNotAvailable();

    /**
     * Creates an exception indicating no component was found for the type.
     *
     * @param typeName the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 45, value = "No component found for type '%s'")
    DeploymentUnitProcessingException componentNotFound(String typeName);

    /**
     * Creates an exception indicating a failure to construct a component view.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 46, value = "Failed to instantiate component view")
    IllegalStateException componentViewConstructionFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating an incompatible conflicting binding.
     *
     * @param bindingName the binding name.
     * @param source      the source.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 47, value = "Incompatible conflicting binding at %s source: %s")
    IllegalArgumentException conflictingBinding(String bindingName, InjectionSource source);

    /**
     * Creates an exception indicating the default constructor for the class, represented by the {@code clazz}
     * parameter, could not be found.
     *
     * @param clazz the class.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 48, value = "Could not find default constructor for %s")
    DeploymentUnitProcessingException defaultConstructorNotFound(Class<?> clazz);

//    /**
//     * Creates an exception indicating the default constructor for the class, represented by the {@code className}
//     * parameter, could not be found.
//     *
//     * @param annotation the annotation.
//     * @param className  the name of the class.
//     *
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 49, value = "Could not find default constructor for %s class %s")
//    DeploymentUnitProcessingException defaultConstructorNotFound(String annotation, String className);

    /**
     * Creates an exception indicating the default constructor for the class, represented by the {@code className}
     * parameter, could not be found on the component.
     *
     * @param className the name of the class.
     * @param component the component name.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 50, value = "No default constructor for interceptor class %s on component %s")
    DeploymentUnitProcessingException defaultConstructorNotFoundOnComponent(String className, Class<?> component);

    /**
     * Creates an exception indicating the element must provide the attribute.
     *
     * @param element   the element.
     * @param attribute the attribute.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 51, value = "%s elements must provide a %s.")
    IllegalArgumentException elementAttributeMissing(String element, String attribute);

    /**
     * Creates an exception indicating a failure to install the component.
     *
     * @param cause the cause of the error.
     * @param name  the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 52, value = "Failed to install component %s")
    DeploymentUnitProcessingException failedToInstallComponent(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating a failure to parse the {@code xmlFile}.
     *
     * @param cause   the cause of the error.
     * @param xmlFile the XML file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 53, value = "Failed to parse %s")
    DeploymentUnitProcessingException failedToParse(@Cause Throwable cause, VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to process the children for the EAR.
     *
     * @param cause   the cause of the error.
     * @param earFile the EAR file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 54, value = "Failed to process children for EAR [%s]")
    DeploymentUnitProcessingException failedToProcessChild(@Cause Throwable cause, VirtualFile earFile);

    /**
     * A message indicating a failure to read the entries in the application.
     *
     * @param entryName the name of the entry.
     * @param appName   the application name.
     *
     * @return the message.
     */
    @Message(id = 55, value = "Failed to read %s entries for application [%s]")
    String failedToRead(String entryName, String appName);

    /**
     * A message indicating a failure to read the entries in the module.
     *
     * @param entryName  the name of the entry.
     * @param appName    the application name.
     * @param moduleName the module name.
     *
     * @return the message.
     */
    @Message(id = 56, value = "Failed to read %s entries for module [%s, %s]")
    String failedToRead(String entryName, String appName, String moduleName);

    /**
     * A message indicating a failure to read the entries in the module.
     *
     * @param entryName     the name of the entry.
     * @param appName       the application name.
     * @param moduleName    the module name.
     * @param componentName the component name
     *
     * @return the message.
     */
    @Message(id = 57, value = "Failed to read %s entries for component [%s, %s, %s]")
    String failedToRead(String entryName, String appName, String moduleName, String componentName);

    /**
     * Creates an exception indicating the field, represented by the {@code name} parameter, was not found.
     *
     * @param name the name of the field.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 58, value = "No matching field found for '%s'")
    DeploymentUnitProcessingException fieldNotFound(String name);

    /**
     * Creates an exception indicating no injection target was found.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 59, value = "No injection target found")
    IllegalStateException injectionTargetNotFound();

    /**
     * Creates an exception indicating the {@code elementName} character type is not exactly one character long.
     *
     * @param elementName the element name.
     * @param value       the value.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 60, value = "%s of type java.lang.Character is not exactly one character long %s")
    DeploymentUnitProcessingException invalidCharacterLength(String elementName, String value);

    /**
     * Creates an exception indicating the descriptor is not valid.
     *
     * @param descriptor the invalid descriptor
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 61, value = "%s is not a valid descriptor")
    RuntimeException invalidDescriptor(String descriptor);

    /**
     * Creates an exception indicating the injection target, represented by the {@code targetName} parameter, on the
     * class, represented by the {@code targetType} parameter, is not compatible with the type of injection.
     *
     * @param targetName the name of the target.
     * @param targetType the type of the target.
     * @param type       the type provided.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 62, value = "Injection target %s on class %s is not compatible with the type of injection: %s")
    DeploymentUnitProcessingException invalidInjectionTarget(String targetName, String targetType, Class<?> type);

    /**
     * A message indicating there are an invalid number of arguments for the method, represented by the parameter,
     * annotated with the {@code annotation} on the class, represented by the {@code className} parameter.
     *
     * @param methodName the name of the method.
     * @param annotation the annotation.
     * @param className  the name of the class.
     *
     * @return the message.
     */
    @Message(id = 63, value = "Invalid number of arguments for method %s annotated with %s on class %s")
    String invalidNumberOfArguments(String methodName, DotName annotation, DotName className);

    /**
     * Creates an exception indicating a return type for the method, represented by the
     * {@code methodName} parameter, annotated with the {@code annotation} on the class, represented by the
     * {@code className} parameter.
     *
     * @param returnType the return type required.
     * @param methodName the name of the method.
     * @param annotation the annotation.
     * @param className  the name of the class.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 64, value = "A return type of %s is required for method %s annotated with %s on class %s")
    IllegalArgumentException invalidReturnType(String returnType, String methodName, DotName annotation, DotName className);

    /**
     * A message indicating methods annotated with the {@code annotation} must have a single argument.
     *
     * @param name         the name of the method.
     * @param annotation   the annotation.
     * @param className    the class name.
     * @param signatureArg the signature argument.
     *
     * @return the message.
     */
    @Message(id = 65, value = "Invalid signature for method %s annotated with %s on class %s, signature must be '%s'")
    String invalidSignature(String name, DotName annotation, DotName className, String signatureArg);

    /**
     * Creates an exception indicating the value for the element is invalid.
     *
     * @param value    the invalid value.
     * @param element  the element.
     * @param location the location of the error.
     *
     * @return {@link XMLStreamException} for the error.
     */
    @Message(id = 66, value = "Invalid value: %s for '%s' element")
    XMLStreamException invalidValue(String value, String element, @Param Location location);

    /**
     * Creates an exception indicating the method does not exist.
     *
     * @param method the method that does not exist.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 67, value = "Method does not exist %s")
    IllegalStateException methodNotFound(Method method);

    /**
     * Creates an exception indicating the method does not exist.
     *
     * @param name      the name of the method.
     * @param paramType the parameter type.
     * @param className the class name.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 68, value = "No matching method found for method %s (%s) on %s")
    DeploymentUnitProcessingException methodNotFound(String name, String paramType, String className);

    /**
     * Creates an exception indicating the annotation is only allowed on method targets.
     *
     * @param annotation the annotation.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 69, value = "@%s is only valid on method targets.")
    DeploymentUnitProcessingException methodOnlyAnnotation(DotName annotation);

    /**
     * Creates an exception indicating multiple components were found for the type.
     *
     * @param typeName the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 70, value = "Multiple components found for type '%s'")
    DeploymentUnitProcessingException multipleComponentsFound(String typeName);

    /**
     * Creates an exception indicating multiple methods found.
     *
     * @param name      the name of the method.
     * @param paramType the parameter type.
     * @param className the class name.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 71, value = "More than one matching method found for method '%s (%s) on %s")
    DeploymentUnitProcessingException multipleMethodsFound(String name, String paramType, String className);

    /**
     * Creates an exception indicating multiple setter methods found.
     *
     * @param targetName the name of the method.
     * @param className  the class name.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 72, value = "Multiple setter methods for %s on class %s found when applying <injection-target> for env-entry")
    DeploymentUnitProcessingException multipleSetterMethodsFound(String targetName, String className);

    /**
     * Creates an exception indicating there is no component instance associated.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 73, value = "No component instance associated")
    IllegalStateException noComponentInstance();

    /**
     * Creates an exception indicating the binding name must not be {@code null}.
     *
     * @param config the binding configuration.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 74, value = "Binding name must not be null: %s")
    DeploymentUnitProcessingException nullBindingName(BindingConfiguration config);

    /**
     * Creates an exception indicating a {@code null} or empty managed bean class name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 75, value = "Managed bean class name cannot be null or empty")
    IllegalArgumentException nullOrEmptyManagedBeanClassName();

    /**
     * Creates an exception indicating a {@code null} or empty resource reference type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 76, value = "Resource reference type cannot be null or empty")
    IllegalArgumentException nullOrEmptyResourceReferenceType();

    /**
     * Creates an exception indicating a {@code null} resource reference processor cannot be registered.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 77, value = "Cannot register a null resource reference processor")
    IllegalArgumentException nullResourceReference();

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 78, value = "%s is null")
    IllegalArgumentException nullVar(String name);


    /**
     * Creates an exception indicating the item cannot be added because the priority is already taken.
     *
     * @param item        the item that was not added.
     * @param hexPriority the priority.
     * @param current     the current item at that priority.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 79, value = "Can't add %s, priority 0x%s is already taken by %s")
    IllegalArgumentException priorityAlreadyExists(Object item, String hexPriority, Object current);

//    /**
//     * Creates an exception indicating the ResourceDescriptionResolver variant should be used.
//     *
//     * @return an {@link UnsupportedOperationException} for the error.
//     */
//    @Message(id = 80, value = "Use the ResourceDescriptionResolver variant")
//    UnsupportedOperationException resourceDescriptionResolverError();

//    /**
//     * Creates an exception indicating the resource reference for the {@code type} is not registered.
//     *
//     * @param type the resource reference type.
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 81, value = "Resource reference for type: %s is not registered. Cannot unregister")
//    IllegalArgumentException resourceReferenceNotRegistered(String type);

    /**
     * Creates an exception indicating the service is not started.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 82, value = "Service not started")
    IllegalStateException serviceNotStarted();

    /**
     * Creates an exception indicating the {@code annotation} injection target is invalid and only setter methods are
     * allowed.
     *
     * @param annotation the annotation.
     * @param methodInfo the method information.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 83, value = "%s injection target is invalid.  Only setter methods are allowed: %s")
    IllegalArgumentException setterMethodOnly(String annotation, MethodInfo methodInfo);

    /**
     * Creates an exception indicating the {@link AnnotationTarget AnnotationTarget} type is unknown.
     *
     * @param target the annotation target.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 84, value = "Unknown AnnotationTarget type: %s")
    RuntimeException unknownAnnotationTargetType(AnnotationTarget target);

    /**
     * Creates an exception indicating the type for the {@code elementName} for the {@code type} is unknown.
     *
     * @param elementName the name of the element.
     * @param type        the type.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 85, value = "Unknown %s type %s")
    DeploymentUnitProcessingException unknownElementType(String elementName, String type);

    /**
     * Creates an exception indicating the method could not found on the view.
     *
     * @param name       the name of the method.
     * @param descriptor the method descriptor.
     * @param viewClass  the view class.
     * @param component  the component class.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 86, value = "Could not find method %s %s on view %s of %s")
    IllegalArgumentException viewMethodNotFound(String name, String descriptor, Class<?> viewClass, Class<?> component);


//    @Message(id = 87, value = "Could not load component class %s")
//    DeploymentUnitProcessingException couldNotLoadComponentClass(@Cause Throwable cause, final String className);

    /**
     * Creates an exception indicating an unexpected element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 88, value = "Unexpected element '%s' encountered")
    XMLStreamException unexpectedElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating that the jboss-ejb-client.xml couldn't be processed
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 89, value = "Failed to process jboss-ejb-client.xml")
    DeploymentUnitProcessingException failedToProcessEJBClientDescriptor(@Cause Throwable cause);

    /**
     * Creates an exception indicating that there was an exception while parsing a jboss-ejb-client.xml
     *
     *
     * @param fileLocation the location of jboss-ejb-client.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 90, value = "Exception while parsing jboss-ejb-client.xml file found at %s")
    DeploymentUnitProcessingException xmlErrorParsingEJBClientDescriptor(@Cause XMLStreamException cause, String fileLocation);

    /**
     * Creates an exception indicating that there was an exception while parsing a jboss-ejb-client.xml
     *
     * @param message The error message
     * @param location The location of the error
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 91, value = "%s")
    XMLStreamException errorParsingEJBClientDescriptor(String message, @Param Location location);

    /**
     * If a message destination could not be resolved
     */
    @Message(id = 92, value = "No message destination with name %s for binding %s")
    String noMessageDestination(String name, String binding);


    /**
     * If a message destination could not be resolved
     */
    @Message(id = 93, value = "More than one message destination with name %s for binding %s destinations: %s")
    String moreThanOneMessageDestination(String name, String binding, Set<String> jndiNames);

    @Message(id = 94, value = "Failed to load jboss.properties")
    DeploymentUnitProcessingException failedToLoadJbossProperties(@Cause IOException e);

    @Message(id = 95, value = "Unsupported ear module type: %s")
    DeploymentUnitProcessingException unsupportedModuleType(String moduleFileName);

    @Message(id = 96, value = "library-directory of value / is not supported")
    DeploymentUnitProcessingException rootAsLibraryDirectory();

    @Message(id = 97, value = "Module may not be a child of the EAR's library directory. Library directory: %s, module file name: %s")
    DeploymentUnitProcessingException earModuleChildOfLibraryDirectory(String libraryDirectory, String moduleFileName);

    @Message(id = 98, value = "ManagedReference was null and injection is not optional for injection into field %s")
    RuntimeException managedReferenceWasNull(Field field);

//    @Message(id = 99, value = "Only 'true' is allowed for 'jboss-descriptor-property-replacement' due to AS7-4892")
//    String onlyTrueAllowedForJBossDescriptorPropertyReplacement_AS7_4892();

    @Message(id = 100, value = "Global modules may not specify 'annotations', 'meta-inf' or 'services'.")
    String propertiesNotAllowedOnGlobalModules();

    //@Message(id = 101, value = "No concurrent context currently set, unable to locate the context service to delegate.")
    //IllegalStateException noConcurrentContextCurrentlySet();

    @Message(id = 102, value = "EE Concurrent Service's value uninitialized.")
    IllegalStateException concurrentServiceValueUninitialized();

    @Message(id = 103, value = "EE Concurrent ContextHandle serialization must be handled by the factory.")
    IOException serializationMustBeHandledByTheFactory();

    @Message(id = 104, value = "The EE Concurrent Context %s already has a factory named %s")
    IllegalArgumentException factoryAlreadyExists(ConcurrentContext concurrentContext, String factoryName);

    @Message(id = 105, value = "EE Concurrent Context %s does not has a factory named %s")
    IOException factoryNotFound(ConcurrentContext concurrentContext, String factoryName);

    @Message(id = 106, value = "EE Concurrent Context %s service not installed.")
    IOException concurrentContextServiceNotInstalled(ServiceName serviceName);

    @Message(id = 107, value = "EE Concurrent Transaction Setup Provider service not installed.")
    IllegalStateException transactionSetupProviderServiceNotInstalled();

    @Message(id = 108, value = "Instance data can only be set during construction")
    IllegalStateException instanceDataCanOnlyBeSetDuringConstruction();

    /**
     * Creates an exception indicating that there was a exception while deploying AroundInvokeInterceptor
     *
     * @param className the name of the class.
     * @param numberOfAnnotatedMethods the number of @aroundInvoke annotations in the specified class.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 109, value = "A class must not declare more than one AroundInvoke method. %s has %s methods annotated.")
    DeploymentUnitProcessingException aroundInvokeAnnotationUsedTooManyTimes(DotName className, int numberOfAnnotatedMethods);

    @LogMessage(level = ERROR)
    @Message(id = 110, value = "Failed to run scheduled task")
    void failedToRunTask(@Cause Exception e);

    @Message(id = 111, value = "Cannot run scheduled task %s as container is suspended")
    IllegalStateException cannotRunScheduledTask(Object delegate);

    /**
     * Creates an exception indicating the core-threads must be greater than 0 for the task queue.
     *
     * @param queueLengthValue the queue length value
     *
     * @return an {@link OperationFailedException} for the exception
     */
    @Message(id = 112, value = "The core-threads value must be greater than 0 when the queue-length is %s")
    OperationFailedException invalidCoreThreadsSize(String queueLengthValue);

    /**
     * Creates an exception indicating the max-threads value cannot be less than the core-threads value.
     *
     * @param maxThreads  the size for the max threads
     * @param coreThreads the size for the core threads
     *
     * @return an {@link OperationFailedException} for the exception
     */
    @Message(id = 113, value = "The max-threads value %d cannot be less than the core-threads value %d.")
    OperationFailedException invalidMaxThreads(int maxThreads, int coreThreads);

    @Message(id = 114, value = "Class does not implement all of the provided interfaces")
    IllegalArgumentException classDoesNotImplementAllInterfaces();

    /**
     * Creates an exception indicating the name of the @{code objectType}, is {@code null}.
     *
     * @param objectType the type of the object.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 115, value = "The name of the %s is null")
    IllegalArgumentException nullName(String objectType);

    /**
     * Creates an exception indicating the variable, represented by the {@code variable} parameter in the @{code objectType} {@code objectName}, is {@code null}.
     *
     * @param variable the name of the variable.
     * @param objectType the type of the object.
     * @param objectName the name of the object.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 116, value = "%s is null in the %s %s")
    IllegalArgumentException nullVar(String variable, String objectType, String objectName);

    @Message(id = 117, value = "Field %s cannot be set - object of %s loaded by %s is not assignable to %s loaded by %s")
    IllegalArgumentException cannotSetField(String fieldName, Class<?> injectedClass, ClassLoader injectedClassloader, Class<?> fieldClass, ClassLoader fieldClassloader);

    //@LogMessage(level = INFO)
    //@Message(id = 118, value = "The system property 'ee8.preview.mode' is set to 'true'. For provided EE 8 APIs where the EE 8 " +
    //        "version of the API differs from what is supported in EE 7, the EE 8 variant of the API will be used. " +
    //        "Support for this setting will be removed once all EE 8 APIs are provided and certified.")
    //void usingEE8PreviewMode();

    //@LogMessage(level = INFO)
    //@Message(id = 119, value = "The system property 'ee8.preview.mode' is NOT set to 'true'. For provided EE 8 APIs where the EE 8 " +
    //        "version of the API differs from what is supported in EE 7, the EE 7 variant of the API will be used. " +
    //        "Support for this setting will be removed once all EE 8 APIs are provided and certified.")
    //void notUsingEE8PreviewMode();

}
