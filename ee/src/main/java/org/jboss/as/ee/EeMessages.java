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

package org.jboss.as.ee;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.vfs.VirtualFile;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface EeMessages {

    /**
     * The messages.
     */
    EeMessages MESSAGES = Messages.getBundle(EeMessages.class);

    /**
     * Creates an exception indicating the alternate deployment descriptor specified for the module file could not be
     * found.
     *
     * @param deploymentDescriptor the alternate deployment descriptor.
     * @param moduleFile           the module file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11025, value = "Could not find alternate deployment descriptor %s specified for %s")
    DeploymentUnitProcessingException alternateDeploymentDescriptor(VirtualFile deploymentDescriptor, VirtualFile moduleFile);

    /**
     * Creates an exception indicating the annotation must provide the attribute.
     *
     * @param annotation the annotation.
     * @param attribute  the attribute.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 11026, value = "%s annotations must provide a %s.")
    IllegalArgumentException annotationAttributeMissing(String annotation, String attribute);

    /**
     * Creates an exception indicating more items cannot be added once getSortedItems() has been called.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11027, value = "Cannot add any more items once getSortedItems() has been called")
    IllegalStateException cannotAddMoreItems();

    /**
     * Creates an exception indicating the {@code name} cannot be empty.
     *
     * @param name the name that cannot be empty.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11028, value = "%s may not be empty")
    RuntimeException cannotBeEmpty(String name);

    /**
     * Creates an exception indicating the {@code name} cannot be {@code null} or empty.
     *
     * @param name  the name that cannot be empty.
     * @param value the value of the object.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11029, value = "%s cannot be null or empty: %s")
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
    @Message(id = 11030, value = "Could not configure component %s")
    DeploymentUnitProcessingException cannotConfigureComponent(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the type for the resource-env-ref could not be determined.
     *
     * @param name the name of the of the resource environment reference.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11031, value = "Could not determine type for resource-env-ref %s")
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
    @Message(id = 11032, value = "Could not determine type for %s %s please specify the %s.")
    DeploymentUnitProcessingException cannotDetermineType(String tag, String value, String typeTag);

    /**
     * Creates an exception indicating the injection target referenced in the env-entry injection point could not be
     * loaded.
     *
     * @param injectionTarget the injection target.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11033, value = "Could not load %s referenced in env-entry")
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
    @Message(id = 11034, value = "Could not load interceptor class %s")
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
    @Message(id = 11035, value = "Could not load interceptor class %s on component %s")
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
    @Message(id = 11036, value = "Could not load view class %s for component %s")
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
    @Message(id = 11037, value = "Unable to process modules in application.xml for EAR [%s], module file %s not found")
    DeploymentUnitProcessingException cannotProcessEarModule(VirtualFile earFile, String moduleFile);

    /**
     * Creates an exception indicating the inability to parse the resource-ref URI.
     *
     * @param cause the cause of the error.
     * @param uri   the URI.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11038, value = "Unable to parse resource-ref URI: %s")
    DeploymentUnitProcessingException cannotParseResourceRefUri(@Cause Throwable cause, String uri);

    /**
     * Creates an exception indicating the injection point could not be resolved on the class specified in the web.xml.
     *
     * @param targetName the injection point name.
     * @param className  the class name.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11039, value = "Could not resolve injection point %s on class %s specified in web.xml")
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
    @Message(id = 11040, value = "Could not resolve method %s on class %s with annotations %s")
    RuntimeException cannotResolveMethod(MethodIdentifier method, Class<?> component, Collection<?> annotations);

    /**
     * Creates an exception indicating the property, represented by the {@code name} parameter, could not be set on the
     * datasource class, represented by the {@code className} parameter.
     *
     * @param cause     the cause of the error.
     * @param name      the name of the property.
     * @param className the datasource class name.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11041, value = "Could not set property %s on datasource class %s")
    RuntimeException cannotSetProperty(@Cause Throwable cause, String name, String className);

    /**
     * Creates an exception indicating both the {@code element1} and {@code element2} cannot be specified in an
     * environment entry.
     *
     * @param element1 the first element.
     * @param element2 the second element.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11042, value = "Cannot specify both a %s and a %s in an environment entry.")
    DeploymentUnitProcessingException cannotSpecifyBoth(String element1, String element2);

    /**
     * Creates an exception indicating a circular dependency is installing.
     *
     * @param bindingName the binding name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11043, value = "Circular dependency installing %s")
    IllegalArgumentException circularDependency(String bindingName);

    /**
     * Creates an exception indicating the annotation is only allowed on a class.
     *
     * @param annotation the annotation.
     * @param target     the annotation target.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11044, value = "%s annotation is only allowed on a class. %s is not a class.")
    DeploymentUnitProcessingException classOnlyAnnotation(String annotation, AnnotationTarget target);

    /**
     * Creates an exception indicating the annotation is only allowed on method or class targets.
     *
     * @param annotation the annotation.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11045, value = "@%s annotation is only allowed on methods and classes")
    DeploymentUnitProcessingException classOrMethodOnlyAnnotation(DotName annotation);

    /**
     * Creates an exception indicating a component, represented by the {@code name} parameter, is already defined in
     * this module.
     *
     * @param name the name of the module.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11046, value = "A component named '%s' is already defined in this module")
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
    @Message(id = 11047, value = "Component class %s for component %s has errors: %n%s")
    DeploymentUnitProcessingException componentClassHasErrors(String className, String componentName, String errorMsg);

    /**
     * Creates an exception indicating a failure to construct a component instance.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11048, value = "Failed to construct component instance")
    IllegalStateException componentConstructionFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating the component is stopped.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11049, value = "Component is stopped")
    IllegalStateException componentIsStopped();

    /**
     * Creates an exception indicating the component is not available.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11050, value = "Component not available (interrupted)")
    IllegalStateException componentNotAvailable();

    /**
     * Creates an exception indicating no component was found for the type.
     *
     * @param typeName the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11051, value = "No component found for type '%s'")
    DeploymentUnitProcessingException componentNotFound(String typeName);

    /**
     * Creates an exception indicating a failure to construct a component view.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11052, value = "Failed to instantiate component view")
    IllegalStateException componentViewConstructionFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating an incompatible conflicting binding.
     *
     * @param bindingName the binding name.
     * @param source      the source.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11053, value = "Incompatible conflicting binding at %s source: %s")
    IllegalArgumentException conflictingBinding(String bindingName, InjectionSource source);

    /**
     * Creates an exception indicating the default constructor for the class, represented by the {@code clazz}
     * parameter, could not be found.
     *
     * @param clazz the class.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11054, value = "Could not find default constructor for %s")
    DeploymentUnitProcessingException defaultConstructorNotFound(Class<?> clazz);

    /**
     * Creates an exception indicating the default constructor for the class, represented by the {@code className}
     * parameter, could not be found.
     *
     * @param annotation the annotation.
     * @param className  the name of the class.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11055, value = "Could not find default constructor for %s class %s")
    DeploymentUnitProcessingException defaultConstructorNotFound(String annotation, String className);

    /**
     * Creates an exception indicating the default constructor for the class, represented by the {@code className}
     * parameter, could not be found on the component.
     *
     * @param className the name of the class.
     * @param component the component name.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11056, value = "No default constructor for interceptor class %s on component %s")
    DeploymentUnitProcessingException defaultConstructorNotFoundOnComponent(String className, Class<?> component);

    /**
     * Creates an exception indicating the element must provide the attribute.
     *
     * @param element   the element.
     * @param attribute the attribute.
     *
     * @return an {@link IllegalArgumentException} for the exception.
     */
    @Message(id = 11057, value = "%s elements must provide a %s.")
    IllegalArgumentException elementAttributeMissing(String element, String attribute);

    /**
     * Creates an exception indicating a failure to install the component.
     *
     * @param cause the cause of the error.
     * @param name  the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11058, value = "Failed to install component %s")
    DeploymentUnitProcessingException failedToInstallComponent(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating a failure to parse the {@code xmlFile}.
     *
     * @param cause   the cause of the error.
     * @param xmlFile the XML file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11059, value = "Failed to parse %s")
    DeploymentUnitProcessingException failedToParse(@Cause Throwable cause, VirtualFile xmlFile);

    /**
     * Creates an exception indicating a failure to process the children for the EAR.
     *
     * @param cause   the cause of the error.
     * @param earFile the EAR file.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11060, value = "Failed to process children for EAR [%s]")
    DeploymentUnitProcessingException failedToProcessChild(@Cause Throwable cause, VirtualFile earFile);

    /**
     * A message indicating a failure to read the entries in the application.
     *
     * @param entryName the name of the entry.
     * @param appName   the application name.
     *
     * @return the message.
     */
    @Message(id = 11061, value = "Failed to read %s entries for application [%s]")
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
    @Message(id = 11062, value = "Failed to read %s entries for module [%s, %s]")
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
    @Message(id = 11063, value = "Failed to read %s entries for component [%s, %s, %s]")
    String failedToRead(String entryName, String appName, String moduleName, String componentName);

    /**
     * Creates an exception indicating the field, represented by the {@code name} parameter, was not found.
     *
     * @param name the name of the field.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11064, value = "No matching field found for '%s'")
    DeploymentUnitProcessingException fieldNotFound(String name);

    /**
     * Creates an exception indicating no injection target was found.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11065, value = "No injection target found")
    IllegalStateException injectionTargetNotFound();

    /**
     * Creates an exception indicating the {@code elementName} character type is not exactly one character long.
     *
     * @param elementName the element name.
     * @param value       the value.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11066, value = "%s of type java.lang.Character is not exactly one character long %s")
    DeploymentUnitProcessingException invalidCharacterLength(String elementName, String value);

    /**
     * Creates an exception indicating the descriptor is not valid.
     *
     * @param descriptor the invalid descriptor
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11067, value = "%s is not a valid descriptor")
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
    @Message(id = 11068, value = "Injection target %s on class %s is not compatible with the type of injection: %s")
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
    @Message(id = 11069, value = "Invalid number of arguments for method %s annotated with %s on class %s")
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
    @Message(id = 11070, value = "A return type of %s is required for method %s annotated with %s on class %s")
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
    @Message(id = 11071, value = "Invalid signature for method %s annotated with %s on class %s, signature must be '%s'")
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
    @Message(id = 11072, value = "Invalid value: %s for '%s' element")
    XMLStreamException invalidValue(String value, String element, @Param Location location);

    /**
     * Creates an exception indicating the method does not exist.
     *
     * @param method the method that does not exist.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11073, value = "Method does not exist %s")
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
    @Message(id = 11074, value = "No matching method found for method %s (%s) on %s")
    DeploymentUnitProcessingException methodNotFound(String name, String paramType, String className);

    /**
     * Creates an exception indicating the annotation is only allowed on method targets.
     *
     * @param annotation the annotation.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11075, value = "@%s is only valid on method targets.")
    DeploymentUnitProcessingException methodOnlyAnnotation(DotName annotation);

    /**
     * Creates an exception indicating multiple components were found for the type.
     *
     * @param typeName the name of the component.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11076, value = "Multiple components found for type '%s'")
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
    @Message(id = 11077, value = "More than one matching method found for method '%s (%s) on %s")
    DeploymentUnitProcessingException multipleMethodsFound(String name, String paramType, String className);

    /**
     * Creates an exception indicating multiple setter methods found.
     *
     * @param targetName the name of the method.
     * @param className  the class name.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11078, value = "Multiple setter methods for %s on class %s found when applying <injection-target> for env-entry")
    DeploymentUnitProcessingException multipleSetterMethodsFound(String targetName, String className);

    /**
     * Creates an exception indicating there is no component instance associated.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11079, value = "No component instance associated")
    IllegalStateException noComponentInstance();

    /**
     * Creates an exception indicating the binding name must not be {@code null}.
     *
     * @param config the binding configuration.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11080, value = "Binding name must not be null: %s")
    DeploymentUnitProcessingException nullBindingName(BindingConfiguration config);

    /**
     * Creates an exception indicating a {@code null} or empty managed bean class name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11081, value = "Managed bean class name cannot be null or empty")
    IllegalArgumentException nullOrEmptyManagedBeanClassName();

    /**
     * Creates an exception indicating a {@code null} or empty resource reference type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11082, value = "Resource reference type cannot be null or empty")
    IllegalArgumentException nullOrEmptyResourceReferenceType();

    /**
     * Creates an exception indicating a {@code null} resource reference processor cannot be registered.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11083, value = "Cannot register a null resource reference processor")
    IllegalArgumentException nullResourceReference();

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11084, value = "%s is null")
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
    @Message(id = 11085, value = "Can't add %s, priority 0x%s is already taken by %s")
    IllegalArgumentException priorityAlreadyExists(Object item, String hexPriority, Object current);

    /**
     * Creates an exception indicating the ResourceDescriptionResolver variant should be used.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 11086, value = "Use the ResourceDescriptionResolver variant")
    UnsupportedOperationException resourceDescriptionResolverError();

    /**
     * Creates an exception indicating the resource reference for the {@code type} is not registered.
     *
     * @param type the resource reference type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11087, value = "Resource reference for type: %s is not registered. Cannot unregister")
    IllegalArgumentException resourceReferenceNotRegistered(String type);

    /**
     * Creates an exception indicating the service is not started.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11088, value = "Service not started")
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
    @Message(id = 11089, value = "%s injection target is invalid.  Only setter methods are allowed: %s")
    IllegalArgumentException setterMethodOnly(String annotation, MethodInfo methodInfo);

    /**
     * Creates an exception indicating the {@link AnnotationTarget AnnotationTarget} type is unknown.
     *
     * @param target the annotation target.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11090, value = "Unknown AnnotationTarget type: %s")
    RuntimeException unknownAnnotationTargetType(AnnotationTarget target);

    /**
     * Creates an exception indicating the type for the {@code elementName} for the {@code type} is unknown.
     *
     * @param elementName the name of the element.
     * @param type        the type.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11091, value = "Unknown %s type %s")
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
    @Message(id = 11092, value = "Could not find method %s %s on view %s of %s")
    IllegalArgumentException viewMethodNotFound(String name, String descriptor, Class<?> viewClass, Class<?> component);


    @Message(id = 11093, value = "Could not load component class %s")
    DeploymentUnitProcessingException couldNotLoadComponentClass(@Cause Throwable cause, final String className);

    /**
     * Creates an exception indicating an unexpected element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 11094, value = "Unexpected element '%s' encountered")
    XMLStreamException unexpectedElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating that the jboss-ejb-client.xml couldn't be processed
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11095, value = "Failed to process jboss-ejb-client.xml")
    DeploymentUnitProcessingException failedToProcessEJBClientDescriptor(@Cause Throwable cause);

    /**
     * Creates an exception indicating that there was a exception while parsing a jboss-ejb-client.xml
     *
     *
     * @param fileLocation the location of jboss-ejb-client.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11096, value = "Exception while parsing jboss-ejb-client.xml file found at %s")
    DeploymentUnitProcessingException xmlErrorParsingEJBClientDescriptor(@Cause XMLStreamException cause, String fileLocation);

    /**
     * Creates an exception indicating that there was a exception while parsing a jboss-ejb-client.xml
     *
     * @param message The error message
     * @param location The location of the error
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 11097, value = "%s")
    XMLStreamException errorParsingEJBClientDescriptor(String message, @Param Location location);

    /**
     * If a message destination could not be resolved
     */
    @Message(id = 11098, value = "No message destination with name %s for binding %s")
    String noMessageDestination(String name, String binding);


    /**
     * If a message destination could not be resolved
     */
    @Message(id = 11099, value = "More than one message destination with name %s for binding %s destinations: %s")
    String moreThanOneMessageDestination(String name, String binding, Set<String> jndiNames);

    @Message(id = 11100, value = "Failed to load jboss.properties")
    DeploymentUnitProcessingException failedToLoadJbossProperties(@Cause IOException e);
}
