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

package org.jboss.as.naming.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.security.Permission;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.subsystem.BindingType;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;

/**
 * Date: 17.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("deprecation")
@MessageLogger(projectCode = "WFLYNAM", length = 4)
public interface NamingLogger extends BasicLogger {
    /**
     * The root logger with a category of the package name.
     */
    NamingLogger ROOT_LOGGER = Logger.getMessageLogger(NamingLogger.class, "org.jboss.as.naming");

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Naming Subsystem")
    void activatingSubsystem();

    /**
     * Logs a warning message indicating the {@code name} failed to get set.
     *
     * @param cause the cause of the error.
     * @param name  the name of the object that didn't get set.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "Failed to set %s")
    void failedToSet(@Cause Throwable cause, String name);

    /**
     * Logs an informational message indicating the naming service is starting.
     */
    @LogMessage(level = INFO)
    @Message(id = 3, value = "Starting Naming Service")
    void startingService();

//    @LogMessage(level = ERROR)
//    @Message(id = 4, value = "Unable to send header, closing channel")
//    void failedToSendHeader(@Cause IOException exception);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 5, value = "Error determining version selected by client.")
//    void failedToDetermineClientVersion(@Cause IOException exception);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 6, value = "Closing channel %s due to an error")
//    void closingChannel(Channel channel, @Cause Throwable t);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 7, value = "Channel end notification received, closing channel %s")
//    void closingChannelOnChannelEnd(Channel channel);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 8, value = "Unexpected internal error")
//    void unexpectedError(@Cause Throwable t);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 9, value = "Null correlationId so error not sent to client")
//    void nullCorrelationId(@Cause Throwable t);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 10, value = "Failed to send exception response to client")
//    void failedToSendExceptionResponse(@Cause IOException ioe);
//
//
//    @LogMessage(level = ERROR)
//    @Message(id = 11, value = "Unexpected parameter type - expected: %d  received: %d")
//    void unexpectedParameterType(byte expected, byte actual);

    /**
     * Creates an exception indicating that a class is not an {@link ObjectFactory} instance, from the specified module.
     * @param cause the cause
     */
    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Failed to release binder service, used for a runtime made JNDI binding")
    void failedToReleaseBinderService(@Cause Throwable cause);

    /**
     *  A message indicating that the lookup of an entry's JNDI view value failed.
     *
     * @param cause the cause of the error.
     * @param entry the jndi name of the entry
     */
    @LogMessage(level = ERROR)
    @Message(id = 13, value = "Failed to obtain jndi view value for entry %s.")
    void failedToLookupJndiViewValue(String entry, @Cause Throwable cause);


    /**
     * Creates an exception indicating you cannot add a {@link Permission} to a read-only
     * {@link java.security.PermissionCollection}.
     *
     * @return A {@link SecurityException} for the error.
     */
    @Message(id = 14, value = "Attempt to add a Permission to a readonly PermissionCollection")
    SecurityException cannotAddToReadOnlyPermissionCollection();

    /**
     * A message indicating the {@code name} cannot be {@code null}.
     *
     * @param name the name.
     *
     * @return the message.
     */
    @Message(id = 15, value = "%s cannot be null.")
    String cannotBeNull(String name);

    /**
     * Creates an exception indicating the object dereference to the object failed.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 16, value = "Could not dereference object")
    NamingException cannotDeferenceObject(@Cause Throwable cause);

    /**
     * Creates an exception indicating the inability to list a non-context binding.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 17, value = "Unable to list a non Context binding.")
    NamingException cannotListNonContextBinding();

    /**
     * A message indicating the link could not be looked up.
     *
     * @return the message.
     */
    @Message(id = 18, value = "Could not lookup link")
    String cannotLookupLink();

//    /**
//     * Creates an exception indicating the {@code name} could not be obtained.
//     *
//     * @param cause the cause of the error.
//     * @param name  the name of the object.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 19, value = "Cannot obtain %s")
//    IllegalStateException cannotObtain(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the service name could not be resolved.
     *
     * @param serviceName the service name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 20, value = "Could not resolve service %s")
    NamingException cannotResolveService(ServiceName serviceName);

    /**
     * Creates an exception indicating the service name could not be resolved.
     *
     * @param serviceName the service name.
     * @param className   the factory class name.
     * @param state       the state of the service.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 21, value = "Could not resolve service reference to %s in factory %s. Service was in state %s.")
    NamingException cannotResolveService(ServiceName serviceName, String className, String state);

    /**
     * Creates an exception indicating the service name could not be resolved and here is a bug.
     *
     * @param serviceName the service name.
     * @param className   the factory class name.
     * @param state       the state of the service.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 22, value = "Could not resolve service reference to %s in factory %s. This is a bug in ServiceReferenceObjectFactory. State was %s.")
    NamingException cannotResolveServiceBug(ServiceName serviceName, String className, String state);

    /**
     * A message indicating duplicate JNDI bindings were found.
     *
     * @param jndiName the JNDI name.
     * @param existing the existing object.
     * @param value    the new object.
     *
     * @return the message.
     */
    @Message(id = 23, value = "Duplicate JNDI bindings for '%s' are not compatible.  [%s] != [%s]")
    String duplicateBinding(JndiName jndiName, Object existing, Object value);

    /**
     * Creates an exception indicating an empty name is not allowed.
     *
     * @return an {@link InvalidNameException} for the error.
     */
    @Message(id = 24, value = "An empty name is not allowed")
    InvalidNameException emptyNameNotAllowed();

    /**
     * Creates an exception indicating the JNDI entry is not yet registered in the context.
     *
     * @param cause       the cause of the error.
     * @param contextName the context name.
     * @param context     the context.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 25, value = "Jndi entry '%s' is not yet registered in context '%s'")
    IllegalStateException entryNotRegistered(@Cause Throwable cause, String contextName, Context context);

    /**
     * Creates an exception indicating a failure to destroy the root context.
     *
     * @param cause the cause of the failure.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 26, value = "Failed to destroy root context")
    IllegalStateException failedToDestroyRootContext(@Cause Throwable cause);

    /**
     * Creates an exception indicating the {@code className} could not be instantiated from the {@code classLoader}.
     *
     * @param description a description.
     * @param className   the class name.
     * @param classLoader the class loader
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 27, value = "Failed instantiate %s %s from classloader %s")
    NamingException failedToInstantiate(@Cause Throwable cause, String description, String className, ClassLoader classLoader);

    /**
     * A message indicating the context entries could not be read from the binding name represented by the
     * {@code bindingName} parameter.
     *
     * @param bindingName the binding name parameter.
     *
     * @return the message.
     */
    @Message(id = 28, value = "Failed to read %s context entries.")
    String failedToReadContextEntries(String bindingName);

    /**
     * A message indicating a failure to start the {@code name} service.
     *
     * @param name the name of the service.
     *
     * @return the message.
     */
    @Message(id = 29, value = "Failed to start %s")
    String failedToStart(String name);

    /**
     * Creates an exception indicating there was an illegal context in the name.
     *
     * @param jndiName the JNDI name.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 30, value = "Illegal context in name: %s")
    RuntimeException illegalContextInName(String jndiName);

//    /**
//     * Creates an exception indicating the actions mask is invalid.
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 31, value = "invalid actions mask")
//    IllegalArgumentException invalidActionMask();

    /**
     * Creates an exception indicating the context reference is invalid.
     *
     * @param referenceName the reference name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 32, value = "Invalid context reference.  Not a '%s' reference.")
    NamingException invalidContextReference(String referenceName);

    /**
     * Creates an exception indicating the JNDI name is invalid.
     *
     * @param jndiName the invalid JNDI name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 33, value = "A valid JNDI name must be provided: %s")
    IllegalArgumentException invalidJndiName(String jndiName);

    /**
     * Creates an exception indicating the load factor must be greater than 0 and less then or equal 1.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 34, value = "Load factor must be greater than 0 and less than or equal to 1")
    IllegalArgumentException invalidLoadFactor();

    /**
     * Creates an exception indicating the permission is invalid.
     *
     * @param permission the permission.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 35, value = "invalid permission, unknown action: %s")
    IllegalArgumentException invalidPermission(Permission permission);

    /**
     * Creates an exception indicating the permission actions is unknown.
     *
     * @param permissionAction the permission action.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 36, value = "invalid permission, unknown action: %s")
    IllegalArgumentException invalidPermissionAction(String permissionAction);

    /**
     * Creates an exception indicating the table size cannot be negative.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 37, value = "Can not have a negative size table!")
    IllegalArgumentException invalidTableSize();

    /**
     * A message indicating that JNDI view is only available in runtime mode.
     *
     * @return the message.
     */
    @Message(id = 38, value = "Jndi view is only available in runtime mode.")
    String jndiViewNotAvailable();

    /**
     * Creates an exception indicating the name could not be found in the context.
     *
     * @param name        the name that could not be found.
     * @param contextName the context name.
     *
     * @return a {@link NameNotFoundException} for the error.
     */
    @Message(id = 39, value = "Name '%s' not found in context '%s'")
    NameNotFoundException nameNotFoundInContext(String name, Name contextName);

//    /**
//     * Creates an exception indicating there is nothing available to bind to.
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 40, value = "Nothing available to bind to.")
//    IllegalStateException noBindingsAvailable();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 41, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * Creates an exception indicating the object factory failed to create from the classloader.
     *
     * @param cause the cause of the failure.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 42, value = "Failed to create object factory from classloader.")
    NamingException objectFactoryCreationFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating the naming context is read-only.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 43, value = "Naming context is read-only")
    UnsupportedOperationException readOnlyNamingContext();

    /**
     * Creates an exception indicating the service name has already been bound.
     *
     * @param serviceName the service name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 44, value = "Service with name [%s] already bound.")
    IllegalArgumentException serviceAlreadyBound(ServiceName serviceName);

    /**
     * Creates an exception indicating the table is full.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 45, value = "Table is full!")
    IllegalStateException tableIsFull();

    /**
     * Creates an exception indicating the thread was interrupted while retrieving the service.
     *
     * @param serviceName the service name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 46, value = "Thread interrupted while retrieving service reference for service %s")
    NamingException threadInterrupt(ServiceName serviceName);

    @Message(id = 47, value = "Invalid name for context binding %s")
    DeploymentUnitProcessingException invalidNameForContextBinding(String name);

    @Message(id = 48, value = "Invalid binding name %s, name must start with one of %s")
    OperationFailedException invalidNamespaceForBinding(String name, String namespaces);

    /**
     * Creates an exception indicating that the type for the binding to add is not known.
     * @param type the unknown type
     * @return the exception
     */
    @Message(id = 49, value = "Unknown binding type %s")
    OperationFailedException unknownBindingType(String type);

    /**
     * Creates an exception indicating that the type for the simple binding to add is not supported.
     * @param type the unsupported type
     * @return the exception
     */
    @Message(id = 50, value = "Unsupported simple binding type %s")
    OperationFailedException unsupportedSimpleBindingType(String type);

    /**
     * Creates an exception indicating that the string value for the simple URL binding failed to transform.
     * @param value the URL value as string
     * @param cause the original cause of failure
     * @return  the exception
     */
    @Message(id = 51, value = "Unable to transform URL binding value %s")
    OperationFailedException unableToTransformURLBindingValue(String value, @Cause Throwable cause);

    /**
     * Creates an exception indicating that a module could not be loaded.
     * @param moduleID the module not loaded
     * @return the exception
     */
    @Message(id = 52, value = "Could not load module %s")
    OperationFailedException couldNotLoadModule(ModuleIdentifier moduleID);

    /**
     * Creates an exception indicating that a class could not be loaded from a module.
     * @param className the name of the class not loaded
     * @param moduleID the module
     * @return the exception
     */
    @Message(id = 53, value = "Could not load class %s from module %s")
    OperationFailedException couldNotLoadClassFromModule(String className, ModuleIdentifier moduleID);

    /**
     * Creates an exception indicating that a class instance could not be instantiate, from the specified module.
     * @param className the name of the class not loaded
     * @param moduleID the module
     * @return the exception
     */
    @Message(id = 54, value = "Could not instantiate instance of class %s from module %s")
    OperationFailedException couldNotInstantiateClassInstanceFromModule(String className, ModuleIdentifier moduleID);

    /**
     * Creates an exception indicating that a class is not an {@link javax.naming.spi.ObjectFactory} instance, from the specified module.
     * @param className the name of the class
     * @param moduleID the module id
     * @return the exception
     */
    @Message(id = 55, value = "Class %s from module %s is not an instance of ObjectFactory")
    OperationFailedException notAnInstanceOfObjectFactory(String className, ModuleIdentifier moduleID);

//    /**
//     * A "simple URL" binding add operation was failed by the operation transformer.
//     * @param modelVersion the model version related with the transformer.
//     * @return
//     */
//    @Message(id = 56, value = "Binding add operation for Simple URL not supported in Naming Subsystem model version %s")
//    String failedToTransformSimpleURLNameBindingAddOperation(String modelVersion);
//
//    /**
//     * A "Object Factory With Environment" binding add operation was failed by the operation transformer.
//     * @param modelVersion the model version related with the transformer.
//     * @return
//     */
//    @Message(id = 57, value = "Binding add operation for Object Factory With Environment not supported in Naming Subsystem model version %s")
//    String failedToTransformObjectFactoryWithEnvironmentNameBindingAddOperation(String modelVersion);
//
//    /**
//     * An external context binding add operation was failed by the operation transformer.
//     * @param modelVersion the model version related with the transformer.
//     * @return
//     */
//    @Message(id = 58, value = "Binding add operation for external context not supported in Naming Subsystem model version %s")
//    String failedToTransformExternalContext(String modelVersion);

    /**
     * Creates an exception indicating a lookup failed, wrt {@link javax.annotation.Resource} injection.
     *
     * @param jndiName the JNDI name.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 59, value = "Resource lookup for injection failed: %s")
    RuntimeException resourceLookupForInjectionFailed(String jndiName, @Cause Throwable cause);

    /**
     * Creates an exception indicating that a required attribute is not defined.
     * @param bindingType binding type
     * @param attributeName missing attribute
     * @return the exception
     */
    @Message(id = 60, value = "Binding type %s requires attribute named %s defined")
    OperationFailedException bindingTypeRequiresAttributeDefined(BindingType bindingType, String attributeName);

    @Message(id = 61, value = "Binding type %s can not take a 'cache' attribute")
    OperationFailedException cacheNotValidForBindingType(BindingType type);

    /**
     * Creates an exception indicating a lookup failure.
     *
     * @param cause the cause of the error
     * @param name  the bind name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 62, value = "Failed to lookup %s")
    NamingException lookupError(@Cause Throwable cause, String name);

    /**
     * Indicates that a service is not started as expected.
     * @return  the exception
     */
    @Message(id = 63, value = "%s service not started")
    IllegalStateException serviceNotStarted(ServiceName serviceName);

    @Message(id = 64, value = "Cannot rebind external context lookup")
    OperationFailedException cannotRebindExternalContext();

    @Message(id = 65, value = "Could not load module %s - the module or one of its dependencies is missing [%s]")
    OperationFailedException moduleNotFound(ModuleIdentifier moduleID, String missingModule);
}
