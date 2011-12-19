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
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
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

    /**
     * Creates an exception indicating that no java.io.Console is available.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 15232, value = "No java.io.Console available to interact with user.")
    IllegalStateException noConsoleAvailable();

    /**
     * A message indicating JBOSS_HOME not set.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15233, value = "JBOSS_HOME environment variable not set.")
    String jbossHomeNotSet();

    /**
     * A message indicating no mgmt-users.properties have been foun.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15234, value = "No mgmt-users.properties files found.")
    String mgmtUsersPropertiesNotFound();

    /**
     * A message prompting the user to enter the details of the user being added.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Enter the details of the new user to add.")
    String enterNewUserDetails();

    /**
     * The prompt to obtain the realm from the user.
     *
     * @param realm - the default realm.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Realm (%s)")
    String realmPrompt(String realm);

    /**
     * The prompt to obtain the new username from the user.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Username")
    String usernamePrompt();

    /**
     * The prompt to obtain the new username from the user.
     *
     * @param defaultUsername - The default username if no value is entered.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Username (%s)")
    String usernamePrompt(String defaultUsername);

    /**
     * The error message if no username is entered.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15235, value = "No Username entered, exiting.")
    String noUsernameExiting();

    /**
     * The prompt to obtain the password from the user.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Password")
    String passwordPrompt();

    /**
     * The error message if no password is entered.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15236, value = "No Password entered, exiting.")
    String noPasswordExiting();

    /**
     * The prompt to obtain the password confirmation from the user.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Re-enter Password")
    String passwordConfirmationPrompt();

    /**
     * The error message if the passwords do not match.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15237, value = "The passwords do not match.")
    String passwordMisMatch();

    /**
     * The error message if the username and password are equal.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15238, value = "Username must not match the password.")
    String usernamePasswordMatch();

    /**
     * The error message if the username is not alpha numeric
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15239, value = "Only alpha/numeric usernames accepted.")
    String usernameNotAlphaNumeric();

    /**
     * Confirmation of the user being added.
     *
     * @param username - The new username.
     * @param realm - The realm the user is being added for.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "About to add user '%s' for realm '%s'")
    String aboutToAddUser(String username, String realm);

    /**
     * Prompt to ask user to confirm yes or no.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Is this correct yes/no?")
    String isCorrectPrompt();

    /**
     * Warning that the username is easy to guess.
     *
     * @param username - The new username.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "The username '%s' is easy to guess")
    String usernameEasyToGuess(String username);

    /**
     * A prompt to double check the user is really sure they want to add this user.
     *
     * @param username - The new username.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Are you sure you want to add user '%s' yes/no?")
    String sureToAddUser(String username);

    /**
     * The error message if the confirmation response is invalid.
     *
     * TODO - On translation we will need support for checking the possible responses.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15240, value = "Invalid response. (Valid responses are yes, y, no, and n)")
    String invalidConfirmationResponse();

    /**
     * Message to inform user that the new user has been added to the file identified.
     *
     * @param username - The new username.
     * @param fileName - The file the user has been added to.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Added user '%s' to file '%s'")
    String addedUser(String username, String fileName);

    /**
     * The error message if adding the user to the file fails.
     *
     * @param file - The name of the file the add failed for.
     * @param error - The failure message.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15241, value = "Unable to add user to %s due to error %s")
    String unableToAddUser(String file, String error);

    /**
     * Message to inform user that the new user is already in the file identified
     *
     * @param username - The new username
     * @param fileName - The file the user was supposed to be added to
     * @return a {@link String} for the message.
     */
    @Message(value = "User %s was already added to file %s, skipping...")
    String userAlreadyExists(String username, String fileName);

    /**
     * The error message header.
     *
     * @return a {@link String} for the message.
     */
    @Message(value = "Error")
    String errorHeader();
}
