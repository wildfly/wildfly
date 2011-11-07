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

package org.jboss.as.domain.management;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.msc.service.StartException;

import javax.naming.NamingException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface DomainManagementMessages {

    /**
     * The messages
     */
    DomainManagementMessages MESSAGES = Messages.getBundle(DomainManagementMessages.class);

    /**
     * Creates an exception indicating the verification could not be performed.
     *
     * @param cause the cause of the error.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 15220, value = "Unable to perform verification")
    IOException cannotPerformVerification(@Cause Throwable cause);

    /**
     * Creates an exception indicating the realm was invalid.
     *
     * @param realm         the invalid realm.
     * @param expectedRealm the expected realm.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 15221, value = "Invalid Realm '%s' expected '%s'")
    IllegalStateException invalidRealm(String realm, String expectedRealm);

    /**
     * Creates an exception indicating the referral for authentication could not be followed.
     *
     * @param name the invalid name.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 15222, value = "Can't follow referral for authentication: %s")
    NamingException nameNotFound(String name);

    /**
     * Creates an exception indicating no authentication mechanism was defined in the security realm.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 15223, value = "No authentication mechanism defined in security realm.")
    IllegalStateException noAuthenticationDefined();

    /**
     * Creates an exception indicating no username was provided.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 15224, value = "No username provided.")
    IOException noUsername();

    /**
     * Creates an exception indicating no password was provided.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 15225, value = "No password to verify.")
    IOException noPassword();

    /**
     * Creates an exception indicating that one of {@code attr1} or {@code attr2} is required.
     *
     * @param attr1 the first attribute.
     * @param attr2 the second attribute.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15226, value = "One of '%s' or '%s' required.")
    IllegalArgumentException oneOfRequired(String attr1, String attr2);

    /**
     * Creates an exception indicating the realm is not supported.
     *
     * @param callback the callback used to create the exception.
     *
     * @return an {@link UnsupportedCallbackException} for the error.
     */
    @Message(id = 15227, value = "Realm choice not currently supported.")
    UnsupportedCallbackException realmNotSupported(@Param Callback callback);

    /**
     * Creates an exception indicating the properties could not be loaded.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 15228, value = "Unable to load properties")
    StartException unableToLoadProperties(@Cause Throwable cause);

    /**
     * Creates an exception indicating the inability to start the service.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 15229, value = "Unable to start service")
    StartException unableToStart(@Cause Throwable cause);

    /**
     * A message indicating the user, represented by the {@code username} parameter, was not found.
     *
     * @param username the username not found.
     *
     * @return the message.
     */
    @Message(id = 15230, value = "User '%s' not found.")
    String userNotFound(String username);

    /**
     * Creates an exception indicating the user, represented by the {@code username} parameter, was not found in the
     * directory.
     *
     * @param username the username not found.
     *
     * @return an {@link IOException} for the error.
     */
    @Message(id = 15231, value = "User '%s' not found in directory.")
    IOException userNotFoundInDirectory(String username);
}
