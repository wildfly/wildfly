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

package org.jboss.as.security;

import java.lang.reflect.Method;

import javax.naming.InvalidNameException;
import javax.naming.OperationNotSupportedException;
import javax.security.auth.login.LoginException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Anil Saldhana
 */
@MessageBundle(projectCode = "JBAS")
public interface SecurityMessages {

    /**
     * The messages
     */
    SecurityMessages MESSAGES = Messages.getBundle(SecurityMessages.class);

    /**
     * Creates an exception indicating the inability to get the {@link ModuleClassLoader}
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 13300, value = "Unable to get the Module Class Loader")
    IllegalStateException unableToGetModuleClassLoader(@Cause Throwable e);

    /**
     * Creates an exception indicating that the operation is not supported
     *
     * @return a {@link OperationNotSupportedException} for the error.
     */
    @Message(id = 13301, value = "Operation not supported : %s")
    OperationNotSupportedException operationNotSupported(Method method);

    /**
     * Creates an exception indicating that the module name was missing
     * @param name the missing module name
     * @return {@link IllegalArgumentException}
     */
    @Message(id = 13302, value = "Missing module name for the %s")
    IllegalArgumentException missingModuleName(String name);

    /**
     * Creates a {@link RuntimeException}
     * @param e the underlying exception
     * @return
     */
    @Message(id = 13303, value = "Runtime Exception:")
    RuntimeException runtimeException(@Cause Throwable e);

    /**
     * Creates a {@link ModuleLoadException}
     * @param e the underlying exception
     * @return
     */
    @Message(id = 13304, value = "Module Load Exception:")
    ModuleLoadException moduleLoadException(@Cause Throwable e);

    /**
     * Creates an exception indicating that the name passed to jndi is null or empty
     * @return {@link InvalidNameException}
     */
    @Message(id = 13305, value = "Name cannot be null or empty")
    InvalidNameException nullName();

    /**
     * Create a {@link LoginException} to indicate that there was no User Principal even though
     * a remoting connection existed
     * @return {@link LoginException}
     */
    @Message(id = 13306, value = "Remoting connection found but no UserPrincipal.")
    LoginException remotingConnectionWithNoUserPrincipal();

    /**
     * Create a {@link IllegalArgumentException} when a null argument is passed
     * @param arg an argument that is null
     * @return {@link IllegalArgumentException}
     */
    @Message(id = 13307, value = "Argument %s is null")
    IllegalArgumentException nullArgument(String arg);

    /**
     * Create a {@link StartException} to indicate that a service could not be started
     * @param service the name of the service
     * @param t underlying exception
     * @return {@link StartException}
     */
    @Message(id = 13308, value = "Unable to start the %s service")
    StartException unableToStartException(String service, @Cause Throwable t);

    /**
     * Create a {@link ClassNotFoundException} to indicate that a class could not be found
     * @param name name of the class
     * @return {@link ClassNotFoundException}
     */
    @Message(id = 13309, value = "Class not found : %s")
    ClassNotFoundException cnfe(String name);

    /**
     * Create a {@link ClassNotFoundException} to indicate that a class could not be found
     * @param name  name of the class
     * @param t underlying exception
     * @return {@link ClassNotFoundException}
     */
    @Message(id = 13310, value = "Class not found : %s")
    ClassNotFoundException cnfeThrow(String name, @Cause Throwable t);

    /**
     * Create a {@link SecurityException}
     * @param t underlying exception
     * @return {@link SecurityException}
     */
    @Message(id = 13311, value = "Security Exception")
    SecurityException securityException(@Cause Throwable t);

    /**
     * Create a {@link SecurityException}
     * @param msg message that is passed in creating the exception
     * @return {@link SecurityException}
     */
    @Message(id = 13312, value = "Security Exception: %s")
    SecurityException securityException(String msg);

    /**
     * Create a {@link VaultReaderException} to indicate there was an exception while reading from the vault
     * @param t underlying exception
     * @return {@link VaultReaderException}
     */
    @Message(id = 13313, value = "Vault Reader Exception:")
    VaultReaderException vaultReaderException(@Cause Throwable t);

    /**
     * Exception indicates that the method being used indicates a misuse of this class
     *
     * @return {@link UnsupportedOperationException}
     */
    @Message(id = 13314, value = "Use the ResourceDescriptionResolver variant")
    UnsupportedOperationException unsupportedOperationExceptionUseResourceDesc();

    /**
     * Create a {@link UnsupportedOperationException} to indicate that the intended operation is not supported
     * @return {@link UnsupportedOperationException}
     */
    @Message(id = 13315, value = "Unsupported Operation")
    UnsupportedOperationException unsupportedOperation();

    /**
     * Create a {@link IllegalArgumentException} to indicate an argument to a method was illegal
     * @param str string message to the exception
     * @return {@link IllegalArgumentException}
     */
    @Message(id = 13316, value = "Illegal Argument:%s")
    IllegalArgumentException illegalArgument(String str);

    /**
     * Create a {@link XMLStreamException} indicating a failure during the stax parsing
     * @param msg failure description
     * @param loc current location of the stax parser
     * @return {@link XMLStreamException}
     */
    @Message(id = 13317, value = "Illegal Argument:%s")
    XMLStreamException xmlStreamException(String msg, @Param Location loc);

    /**
     * Create a {@link XMLStreamException} to indicate that the security domain configuration cannot have both JAAS and JASPI config
     * @param loc the current location of the stax parser
     * @return {@link XMLStreamException}
     */
    @Message(id = 13318, value = "A security domain can have either an <authentication> or <authentication-jaspi> element, not both")
    XMLStreamException xmlStreamExceptionAuth(@Param Location loc);

    /**
     * Creates a {@link XMLStreamException} to indicate a missing required attribute
     * @param a the first attribute
     * @param b the second attribute
     * @param loc the current location of the stax parser
     * @return {@link XMLStreamException}
     */
    @Message(id = 13319, value = "Missing required attribute: either %s or %s must be present")
    XMLStreamException xmlStreamExceptionMissingAttribute(String a, String b, @Param Location loc);

    /**
     * Create a {@link IllegalArgumentException} to indicate that the auth-module references a login module stack that does not exist
     * @param str login module stack name
     * @return {@link IllegalArgumentException}
     */
    @Message(id = 13320, value = "auth-module references a login module stack that doesn't exist::%s")
    IllegalArgumentException loginModuleStackIllegalArgument(String str);

    /**
     * Create a {@link IllegalArgumentException} when the path address does not contain a security domain name
     * @param arg an argument that is null
     * @return {@link IllegalArgumentException}
     */
    @Message(id = 13321, value = "Address did not contain a security domain name")
    IllegalArgumentException addressDidNotContainSecurityDomain();
}