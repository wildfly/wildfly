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

package org.jboss.as.naming;

import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.ServiceName;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.security.Permission;

/**
 * Date: 17.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface NamingMessages {
    /**
     * The default messages.
     */
    NamingMessages MESSAGES = Messages.getBundle(NamingMessages.class);

    /**
     * Creates an exception indicating you cannot add a {@link Permission} to a read-only
     * {@link java.security.PermissionCollection}.
     *
     * @return A {@link SecurityException} for the error.
     */
    @Message(id = 11830, value = "Attempt to add a Permission to a readonly PermissionCollection")
    SecurityException cannotAddToReadOnlyPermissionCollection();

    /**
     * A message indicating the {@code name} cannot be {@code null}.
     *
     * @param name the name.
     *
     * @return the message.
     */
    @Message(id = 11831, value = "%s cannot be null.")
    String cannotBeNull(String name);

    /**
     * Creates an exception indicating the object dereference to the object failed.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 11832, value = "Could not dereference object")
    NamingException cannotDeferenceObject(@Cause Throwable cause);

    /**
     * Creates an exception indicating the inability to list a non-context binding.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 11833, value = "Unable to list a non Context binding.")
    NamingException cannotListNonContextBinding();

    /**
     * A message indicating the link could not be looked up.
     *
     * @return the message.
     */
    @Message(id = 11834, value = "Could not lookup link")
    String cannotLookupLink();

    /**
     * Creates an exception indicating the {@code name} could not be obtained.
     *
     * @param cause the cause of the error.
     * @param name  the name of the object.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11835, value = "Cannot obtain %s")
    IllegalStateException cannotObtain(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the service name could not be resolved.
     *
     * @param serviceName the service name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 11836, value = "Could not resolve service %s")
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
    @Message(id = 11837, value = "Could not resolve service reference to %s in factory %s. Service was in state %s.")
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
    @Message(id = 11838, value = "Could not resolve service reference to %s in factory %s. This is a bug in ServiceReferenceObjectFactory. State was %s.")
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
    @Message(id = 11839, value = "Duplicate JNDI bindings for '%s' are not compatible.  [%s] != [%s]")
    String duplicateBinding(JndiName jndiName, Object existing, Object value);

    /**
     * Creates an exception indicating an empty name is not allowed.
     *
     * @return an {@link InvalidNameException} for the error.
     */
    @Message(id = 11840, value = "An empty name is not allowed")
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
    @Message(id = 11841, value = "Jndi entry '%s' is not yet registered in context '%s'")
    IllegalStateException entryNotRegistered(@Cause Throwable cause, String contextName, Context context);

    /**
     * Creates an exception indicating a failure to destroy the root context.
     *
     * @param cause the cause of the failure.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11842, value = "Failed to destroy root context")
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
    @Message(id = 11843, value = "Failed instantiate %s %s from classloader %s")
    NamingException failedToInstantiate(String description, String className, ClassLoader classLoader);

    /**
     * A message indicating the context entries could not be read from the binding name represented by the
     * {@code bindingName} parameter.
     *
     * @param bindingName the binding name parameter.
     *
     * @return the message.
     */
    @Message(id = 11844, value = "Failed to read %s context entries.")
    String failedToReadContextEntries(String bindingName);

    /**
     * A message indicating a failure to start the {@code name} service.
     *
     * @param name the name of the service.
     *
     * @return the message.
     */
    @Message(id = 11845, value = "Failed to start %s")
    String failedToStart(String name);

    /**
     * Creates an exception indicating there was an illegal context in the name.
     *
     * @param jndiName the JNDI name.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11846, value = "Illegal context in name: %s")
    RuntimeException illegalContextInName(String jndiName);

    /**
     * Creates an exception indicating the actions mask is invalid.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11847, value = "invalid actions mask")
    IllegalArgumentException invalidActionMask();

    /**
     * Creates an exception indicating the context reference is invalid.
     *
     * @param referenceName the reference name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 11848, value = "Invalid context reference.  Not a '%s' reference.")
    NamingException invalidContextReference(String referenceName);

    /**
     * Creates an exception indicating the JNDI name is invalid.
     *
     * @param jndiName the invalid JNDI name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11849, value = "A valid JNDI name must be provided: %s")
    IllegalArgumentException invalidJndiName(String jndiName);

    /**
     * Creates an exception indicating the load factor must be greater than 0 and less then or equal 1.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11850, value = "Load factor must be greater than 0 and less than or equal to 1")
    IllegalArgumentException invalidLoadFactor();

    /**
     * Creates an exception indicating the permission is invalid.
     *
     * @param permission the permission.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11851, value = "invalid permission, unknown action: %s")
    IllegalArgumentException invalidPermission(Permission permission);

    /**
     * Creates an exception indicating the permission actions is unknown.
     *
     * @param permissionAction the permission action.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11852, value = "invalid permission, unknown action: %s")
    IllegalArgumentException invalidPermissionAction(String permissionAction);

    /**
     * Creates an exception indicating the table size cannot be negative.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11853, value = "Can not have a negative size table!")
    IllegalArgumentException invalidTableSize();

    /**
     * A message indicating that JNDI view is only available in runtime mode.
     *
     * @return the message.
     */
    @Message(id = 11854, value = "Jndi view is only available in runtime mode.")
    String jndiViewNotAvailable();

    /**
     * Creates an exception indicating the name could not be found in the context.
     *
     * @param name        the name that could not be found.
     * @param contextName the context name.
     *
     * @return a {@link NameNotFoundException} for the error.
     */
    @Message(id = 11855, value = "Name '%s' not found in context '%s'")
    NameNotFoundException nameNotFoundInContext(String name, Name contextName);

    /**
     * Creates an exception indicating there is nothing available to bind to.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11856, value = "Nothing available to bind to.")
    IllegalStateException noBindingsAvailable();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11857, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * Creates an exception indicating the object factory failed to create from the classloader.
     *
     * @param cause the cause of the failure.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 11858, value = "Failed to create object factory from classloader.")
    NamingException objectFactoryCreationFailure(@Cause Throwable cause);

    /**
     * Creates an exception indicating the naming context is read-only.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 11859, value = "Naming context is read-only")
    UnsupportedOperationException readOnlyNamingContext();

    /**
     * Creates an exception indicating the service name has already been bound.
     *
     * @param serviceName the service name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11860, value = "Service with name [%s] already bound.")
    IllegalArgumentException serviceAlreadyBound(ServiceName serviceName);

    /**
     * Creates an exception indicating the table is full.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11861, value = "Table is full!")
    IllegalStateException tableIsFull();

    /**
     * Creates an exception indicating the thread was interrupted while retrieving the service.
     *
     * @param serviceName the service name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 11862, value = "Thread interrupted while retrieving service reference for service %s")
    NamingException threadInterrupt(ServiceName serviceName);

    @Message(id = 11863, value = "Invalid name for context binding %s")
    DeploymentUnitProcessingException invalidNameForContextBinding(String name);
}
