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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import javax.naming.NamingException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.domain.management.security.password.PasswordValidationException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;
import org.jboss.msc.service.StartException;

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
    //@Message(id = 15223, value = "No authentication mechanism defined in security realm.")
    //IllegalStateException noAuthenticationDefined();

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
    NamingException userNotFoundInDirectory(String username);

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
    //@Message(id = 15233, value = "JBOSS_HOME environment variable not set.")
    //String jbossHomeNotSet();

    /**
     * A message indicating no mgmt-users.properties have been found.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15234, value = "No %s files found.")
    String propertiesFileNotFound(String file);

    /**
     * A message prompting the user to enter the details of the user being added.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Enter the details of the new user to add.")
    String enterNewUserDetails();

    /**
     * The prompt to obtain the realm from the user.
     *
     * @param realm - the default realm.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Realm (%s)")
    String realmPrompt(String realm);

    /**
     * The prompt to obtain the new username from the user.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Username")
    String usernamePrompt();

    /**
     * The prompt to obtain the new username from the user.
     *
     * @param defaultUsername - The default username if no value is entered.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Username (%s)")
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
    @Message(id = Message.NONE, value = "Password")
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
    @Message(id = Message.NONE, value = "Re-enter Password")
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
    @Message(id = Message.NONE, value = "About to add user '%s' for realm '%s'")
    String aboutToAddUser(String username, String realm);

    /**
     * Prompt to ask user to confirm the previous statement is correct.
     *
     * Do not include the translation specific yes/no
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Is this correct")
    String isCorrectPrompt();

    /**
     * Warning that the username is easy to guess.
     *
     * @param username - The new username.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "The username '%s' is easy to guess")
    String usernameEasyToGuess(String username);

    /**
     * A prompt to double check the user is really sure they want to add this user.
     *
     * @param username - The new username.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Are you sure you want to add user '%s' yes/no?")
    String sureToAddUser(String username);

    /**
     * The error message if the confirmation response is invalid.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15240, value = "Invalid response. (Valid responses are %s and %s)")
    String invalidConfirmationResponse(String firstValues, String secondValues);

    /**
     * Message to inform user that the new user has been added to the file identified.
     *
     * @param username - The new username.
     * @param fileName - The file the user has been added to.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Added user '%s' to file '%s'")
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
     * The error message if loading the known users from file fails.
     *
     * @param file - The name of the file the load failed for.
     * @param error - The failure message.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15242, value = "Unable to add load users from %s due to error %s")
    String unableToLoadUsers(String file, String error);

    /**
     * The error message header.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Error")
    String errorHeader();

    /**
     * A message to check if this user is going to be used by a host controller to connect to the master domain controller.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Is this new user going to be used for one AS process to connect to another AS process? %ne.g. for a slave host controller connecting to the master or for a Remoting connection for server to server EJB calls.")
    String serverUser();

    /**
     * Simple yes/no prompt.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "yes/no?")
    String yesNo();

    /**
     * To represent this user use the following secret entry in the server-identities.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "To represent the user add the following to the server-identities definition <secret value=\"%s\" />")
    String secretElement(String base64);

    /**
     * Error message if more than one username/password authentication mechanism is defined.
     *
     * @param realmName the name of the security realm
     * @param mechanisms the set of mechanisms .
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15244, value = "Configuration for security realm '%s' includes multiple username/password based authentication mechanisms (%s). Only one is allowed")
    OperationFailedException multipleAuthenticationMechanismsDefined(String realmName, Set<String> mechanisms);

    /**
     * Creates an exception indicating that one of {@code attr1} or {@code attr2} is required.
     *
     * @param attr1 the first attribute.
     * @param attr2 the second attribute.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15246, value = "One of '%s' or '%s' required.")
    OperationFailedException operationFailedOneOfRequired(String attr1, String attr2);

    /**
     * Creates an exception indicating that only one of {@code attr1} or {@code attr2} is required.
     *
     * @param attr1 the first attribute.
     * @param attr2 the second attribute.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15247, value = "Only one of '%s' or '%s' is required.")
    OperationFailedException operationFailedOnlyOneOfRequired(String attr1, String attr2);

    /**
     * Creates an IllegalArgumentException indicating that a value can not be null.
     *
     * @param name - The name of the parameter that can not be null.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15248, value = "'%s' can not be null.")
    IllegalArgumentException canNotBeNull(String name);

    /**
     * Creates a String for use in an OperationFailedException to indicate that no security context has been established for a
     * call that requires one.
     */
    @Message(id = 15249, value = "No security context has been established.")
    String noSecurityContextEstablished();

    /**
     * Creates a String for use in an OperationFailedException to indicate that an unexpected number of RealmUser instances have
     * been found.
     *
     * @param count - The number of RealmUser instances found.
     */
    //@Message(id = 15250, value = "An unexpected number (%d) of RealmUsers are associated with the SecurityContext.")
    //String unexpectedNumberOfRealmUsers(int count);

    /**
     * Prompt for the file to update in add-users
     */
    @Message(id = Message.NONE, value = "What type of user do you wish to add? %n a) Management User (mgmt-users.properties) %n b) Application User (application-users.properties)")
    String filePrompt();

    /**
     * Prompt the user for the groups to add the user to
     * @return
     */
    @Message(id = Message.NONE, value = "What groups do you want this user to belong to? (Please enter a comma separated list, or leave blank for none)")
    String groupsPrompt();


    /**
     * Message to inform user that the new user has been added to the groups file identified.
     *
     * @param username - The new username.
     * @param groups - The new groups.
     * @param fileName - The file the user has been added to.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Added user '%s' with groups %s to file '%s'")
    String addedGroups(String username, String groups, String fileName);

    /**
     * The error message if the choice response is invalid.
     *
     * TODO - On translation we will need support for checking the possible responses.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15251, value = "Invalid response. (Valid responses are A, a, B, or b)")
    String invalidChoiceResponse();

    /**
     * Confirmation if the current user (enabled) is about to be updated.
     *
     * @param user - The name of the user.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "User '%s' already exists and is enabled, would you like to... %n a) Update the existing user password and roles %n b) Disable the existing user %n c) Type a new username")
    String aboutToUpdateEnabledUser(String user);

    /**
     * Confirmation if the current user (disabled) is about to be updated.
     *
     * @param user - The name of the user.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "User '%s' already exists and is disabled, would you like to... %n a) Update the existing user password and roles %n b) Enable the existing user %n c) Type a new username")
    String aboutToUpdateDisabledUser(String user);

    /**
     * Message to inform user that the user has been updated to the file identified.
     *
     * @param userName - The new username.
     * @param canonicalPath - The file the user has been added to.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Updated user '%s' to file '%s'")
    String updateUser(String userName, String canonicalPath);

    /**
     * The error message if updating user to the file fails.
     *
     * @param absolutePath - The name of the file the add failed for.
     * @param message - The failure message.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15254, value = "Unable to update user to %s due to error %s")
    String unableToUpdateUser(String absolutePath, String message);

    /**
     * Message to inform user that the user has been updated to the groups file identified.
     *
     * @param username - The new username.
     * @param groups - The new groups.
     * @param fileName - The file the user has been added to.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Updated user '%s' with groups %s to file '%s'")
    String updatedGroups(String username, String groups, String fileName);

    /**
     * IOException to indicate the user attempting to use local authentication has been rejected.
     *
     * @param userName - The user attempting local authentication.
     * @return an {@link IOException} for the failure.
     */
    @Message(id = 15255, value = "The user '%s' is not allowed in a local authentication.")
    IOException invalidLocalUser(final String userName);

    /**
     * StartException to indicate that multiple CallbackHandlerServices are associated for the same mechanism.
     *
     * @param mechanismName - the name of the mechanism being registered.
     * @return an {@link StartException} for the failure.
     */
    @Message(id = 15256, value = "Multiple CallbackHandlerServices for the same mechanism (%s)")
    StartException multipleCallbackHandlerForMechanism(final String mechanismName);

    /**
     * IllegalStateException to indicate a CallbackHandler has been requested for an unsupported mechanism.
     *
     * @param mechanism - The name of the mechanism requested.
     * @param realmName - The name of the realm the mechanism was requested from.
     * @return an {@link IllegalStateException} for the failure.
     */
    @Message(id = 15259, value = "No CallbackHandler available for mechanism %s in realm %s")
    IllegalStateException noCallbackHandlerForMechanism(final String mechanism, final String realmName);

    /**
     * IllegalStateException to indicate no plug in providers were loaded for the specified name.
     *
     * @param name The name of the module loaded.
     * @return an {@link IllegalStateException} for the failure.
     */
    @Message(id = 15260, value = "No plug in providers found for module name %s")
    IllegalArgumentException noPlugInProvidersLoaded(final String name);

    /**
     * IllegalStateException to indicate a failure loading the PlugIn.
     *
     * @param name - The name of the plug-in being loaded.
     * @param error - The error that occurred.
     * @return an {@link IllegalArgumentException} for the failure.
     */
    @Message(id = 15261, value = "Unable to load plug-in for module %s due to error (%s)")
    IllegalArgumentException unableToLoadPlugInProviders(final String name, final String error);

    /**
     * IllegalArgumentException to indicate that an AuthenticationPlugIn was not loaded.
     *
     * @param name - The name specified.
     * @return an {@link IllegalArgumentException} for the failure.
     */
    @Message(id = 15262, value = "No authentication plug-in found for name %s")
    IllegalArgumentException noAuthenticationPlugInFound(final String name);

    /**
     * IllegalStateException to indicate that a plug-in could not be initialised.
     *
     * @param name - The name specified.
     * @return an {@link IllegalArgumentException} for the failure.
     */
    @Message(id = 15263, value = "Unable to initialise plug-in %s due to error %s")
    IllegalStateException unableToInitialisePlugIn(final String name, final String message);

    /**
     * The error message for password which does not met strength requirement.
     *
     * @param currentStrength - strength value which has been computed from password.
     * @param desiredStrength - Minimum strength value which should be met.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15264, value = "Password is not strong enough, it is '%s'. It should be at least '%s'.")
    String passwordNotStrongEnough(String currentStrength, String desiredStrength);

    /**
     * The error message for password which has forbidden value.
     *
     * @param password - password value.
     *
     * @return a {@link PasswordValidationException} for the message.
     */
    @Message(id = 152565, value = "Password must not be equal to '%s', this value is restricted.")
    PasswordValidationException passwordMustNotBeEqual(String password);

    /**
     * The error message for password which has not enough digit.
     * @param minDigit - minimum digit values.
     * @return a {@link String} for the message.
     */
    @Message(id = 15266, value = "Password must have at least %d digit.")
    String passwordMustHaveDigit(int minDigit);

    /**
     * The error message for password which has not enough symbol.
     * @param minSymbol - minimum symbol values.
     * @return a {@link String} for the message.
     */
    @Message(id = 15267, value = "Password must have at least %s non-alphanumeric symbol.")
    String passwordMustHaveSymbol(int minSymbol);

    /**
     * The error message for password which has not enough alpha numerical values.
     * @param minAlpha - minimum alpha numerical values.
     * @return a {@link String} for the message.
     */
    @Message(id = 15268, value = "Password must have at least %d alphanumeric character.")
    String passwordMustHaveAlpha(int minAlpha);

    /**
     * The error message for password which is not long enough.
     * @param desiredLength - desired length of password.
     * @return a {@link PasswordValidationException} for the message.
     */
    @Message(id = 15269, value = "Password must have at least %s characters!")
    PasswordValidationException passwordNotLongEnough(int desiredLength);

    @Message(id = 15270, value = "Unable to load key trust file.")
    IllegalStateException unableToLoadKeyTrustFile(@Cause Throwable t);

    @Message(id = 15271, value = "Unable to operate on trust store.")
    IllegalStateException unableToOperateOnTrustStore(@Cause GeneralSecurityException gse);

    @Message(id = 15272, value = "Unable to create delegate trust manager.")
    IllegalStateException unableToCreateDelegateTrustManager();

    @Message(id = 15273, value = "The syslog-handler can only contain one protocol %s")
    XMLStreamException onlyOneSyslogHandlerProtocol(Location location);

    @Message(id = 15274, value = "There is no handler called '%s'")
    IllegalStateException noHandlerCalled(String name);

    @Message(id = 15275, value = "There is already a protocol configured for the syslog handler at %s")
    OperationFailedException sysLogProtocolAlreadyConfigured(PathAddress append);

    @Message(id = 15276, value = "No syslog protocol was given")
    OperationFailedException noSyslogProtocol();

    @Message(id = 15277, value = "There is no formatter called '%s'")
    OperationFailedException noFormatterCalled(String formatterName);

    @Message(id = 15278, value = "Can not remove formatter, it is still referenced by the hander '%s'")
    OperationFailedException cannotRemoveReferencedFormatter(PathElement pathElement);

    @Message(id = 15279, value = "Handler names must be unique. There is already a handler called '%s' at %s")
    OperationFailedException handlerAlreadyExists(String name, PathAddress append);

    /**
     * Parsing the user property file different realm names have been detected, the add-user utility requires the same realm
     * name to be used across all propery files a user is being added to.
     */
    @Message(id = 15280, value = "Different realm names detected '%s', '%s' reading user property files, all realms must be equal.")
    String multipleRealmsDetected(final String realmOne, final String realmTwo);

    /**
     * The user has supplied a realm name but the supplied name does not match the name discovered from the property files.
     */
    @Message(id = 15281, value = "The user supplied realm name '%s' does not match the realm name discovered from the property file(s) '%s'.")
    String userRealmNotMatchDiscovered(final String supplied, final String discovered);

    /**
     * The user has supplied a group properties file name but no user propertites file name.
     */
    @Message(id = 15282, value = "A group properties file '%s' has been specified, however no user properties has been specified.")
    String groupPropertiesButNoUserProperties(final String groupProperties);

    /**
     * There is no default realm name and the user has not specified one either.
     */
    @Message(id = 15283, value = "A realm name must be specified.")
    String realmMustBeSpecified();

    /**
     * Creates an exception indicating that RBAC has been enabled but it is not possible for users to be mapped to roles.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15284, value = "The current operation(s) would result in role based access control being enabled but leave it impossible for authenticated users to be assigned roles.")
    OperationFailedException inconsistentRbacConfiguration();

    /**
     * Creates an exception indicating that the runtime role mapping state is inconsistent.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15285, value = "The runtime role mapping configuration is inconsistent, the server must be restarted.")
    OperationFailedException inconsistentRbacRuntimeState();

    /**
     * The error message if the choice response is invalid to the update user state.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = 15286, value = "Invalid response. (Valid responses are A, a, B, b, C or c)")
    String invalidChoiceUpdateUserResponse();

    @Message(id = 15287, value = "Role '%s' already contains an %s for type=%s, name=%s, realm=%s.")
    OperationFailedException duplicateIncludeExclude(String roleName, String incExcl, String type, String name, String realm);

    /**
     * Error message if more than one authorization configuration is defined.
     *
     * @param realmName the name of the security realm
     * @param configurations the set of configurations .
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15288, value = "Configuration for security realm '%s' includes multiple authorization configurations (%s). Only one is allowed")
    OperationFailedException multipleAuthorizationConfigurationsDefined(String realmName, Set<String> configurations);

    /**
     * Error message if more than one username-to-dn resource is defined.
     *
     * @param realmName the name of the security realm
     * @param configurations the set of configurations .
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15289, value = "Configuration for security realm '%s' includes multiple username-to-dn resources within the authorization=ldap resource (%s). Only one is allowed")
    OperationFailedException multipleUsernameToDnConfigurationsDefined(String realmName, Set<String> configurations);

    /**
     * Error message if no group-search resource is defined.
     *
     * @param realmName the name of the security realm
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15290, value = "Configuration for security realm '%s' does not contain any group-search resource within the authorization=ldap resource.")
    OperationFailedException noGroupSearchDefined(String realmName);

    /**
     * Error message if more than one group-search resource is defined.
     *
     * @param realmName the name of the security realm
     * @param configurations the set of configurations .
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15291, value = "Configuration for security realm '%s' includes multiple group-search resources within the authorization=ldap resource (%s). Only one is allowed")
    OperationFailedException multipleGroupSearchConfigurationsDefined(String realmName, Set<String> configurations);

    /**
     * Error message if the name of a role mapping being added is invalid.
     *
     * @param roleName - The name of the role.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15292, value = "The role name '%s' is not a valid standard role.")
    OperationFailedException invalidRoleName(String roleName);

    /**
     * Error message if the name of a role mapping being added is invalid.
     *
     * @param roleName - The name of the role.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15293, value = "The role name '%s' is not a valid standard role and is not a host scoped role or a server group scoped role.")
    OperationFailedException invalidRoleNameDomain(String roleName);

    /**
     * Error message if the name of a scoped role can not be removed as the role mapping remains.
     *
     * @param roleName - The name of the role.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15294, value = "The scoped role '%s' can not be removed as a role mapping still exists.")
    OperationFailedException roleMappingRemaining(String roleName);

    /**
     * Error message if a scoped role already exists with the same name.
     *
     * @param scopeType - The type of scoped role.
     * @param roleName - The name of the role.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15295, value = "A %s already exists with name '%s'")
    OperationFailedException duplicateScopedRole(String scopeType, String roleName);

    /**
     * Error message if a scoped role name matches a standard role.
     *
     * @param scopedRole - The name of the scoped role.
     * @param standardRole - The name of the standard role.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15296, value = "The name '%s' conflicts with the standard role name of '%s' - comparison is case insensitive.")
    OperationFailedException scopedRoleStandardName(String scopedRole, String standardRole);

    /**
     * Error message if the base-role is not one of the standard roles.
     *
     * @param baseRole - The base-role supplied.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 15297, value = "The base-role '%s' is not one of the standard roles for the current authorization provider.")
    OperationFailedException badBaseRole(String baseRole);

    /**
     * Error message if the password and username match.
     *
     * @return an {@link PasswordValidationException} for the error.
     */
    @Message(id = 15298, value = "The password must be different from the username")
    PasswordValidationException passwordUsernameMatchError();

    /**
     * Create an exception indicating that there are no keys in the keystore.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 15299, value = "The KeyStore %s does not contain any keys.")
    StartException noKey(String path);

    /**
     * Create an exception indicating that the alias specified is not a key.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 21000, value = "The alias specified '%s' is not a Key, valid aliases are %s")
    StartException aliasNotKey(String alias, String validList);

    /**
     * Create an exception indicating that the alias specified was not found.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 21001, value = "The alias specified '%s' does not exist in the KeyStore, valid aliases are %s")
    StartException aliasNotFound(String alias, String validList);

    /**
     * Create an exception indicating that the keystore was not found.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 21002, value = "The KeyStore can not be found at %s")
    StartException keyStoreNotFound(String path);

    /**
     * Error message if more than one cache is defined.
     *
     * @param realmName the name of the security realm
     * @param configurations the set of configurations .
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 21003, value = "Configuration for security realm '%s' includes multiple cache definitions at the same position in the hierarchy. Only one is allowed")
    OperationFailedException multipleCacheConfigurationsDefined(String realmName);

    /**
     * Creates an exception indicating that is was not possible to load a username for the supplied username.
     *
     * @param name the supplied username.
     *
     * @return a {@link NamingException} for the error.
     */
    @Message(id = 21004, value = "Unable to load username for supplied username '%s'")
    NamingException usernameNotLoaded(String name);

    /*
     * Logging IDs 15200-15299 and 21000-21099 are reserved for domain management
     *
     * The file DomainManagementLogger also contains messages in this range 15200-15220.
     */

    /**
     * Information message saying the username and password must be different.
     *
     * @return an {@link String} for the error.
     */
    @Message(id = Message.NONE, value = "The password must be different from the username")
    String passwordUsernameMustMatchInfo();

    /**
     * Information message saying the username and password should be different.
     *
     * @return an {@link String} for the error.
     */
    @Message(id = Message.NONE, value = "The password should be different from the username")
    String passwordUsernameShouldMatchInfo();

    /**
     * Information message saying the password must not equal any of the restricted values.
     *
     * @param restricted - A list of restricted values.
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "The password must not be one of the following restricted values {%s}")
    String passwordMustNotEqualInfo(String restricted);

    /**
     * Information message saying the password should not equal any of the restricted values.
     *
     * @param restricted - A list of restricted values.
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "The password should not be one of the following restricted values {%s}")
    String passwordShouldNotEqualInfo(String restricted);

    /**
     * Information message to describe how many characters need to be in the password.
     *
     * @param desiredLength - desired length of password.
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "%s characters")
    String passwordLengthInfo(int desiredLength);

    /**
     * Information message for the number of alphanumerical characters required in a password.
     *
     * @param minAlpha - minimum alpha numerical values.
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "%d alphanumeric character(s)")
    String passwordMustHaveAlphaInfo(int minAlpha);

    /**
     * Information message for the number of digits required in a password.
     *
     * @param minDigit - minimum digit values.
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "%d digit(s)")
    String passwordMustHaveDigitInfo(int minDigit);

    /**
     * Information message for the number of non alphanumerical symbols required in a password.
     *
     * @param minSymbol - minimum symbol values.
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "%s non-alphanumeric symbol(s)")
    String passwordMustHaveSymbolInfo(int minSymbol);

    /**
     * Information message to describe what a password must contain.
     *
     * @param requirements - The requirements list to contain in the message.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "The password must contain at least %s")
    String passwordMustContainInfo(String requirements);

    /**
     * Information message to describe what a password should contain.
     *
     * @param requirements - The requirements list to contain in the message.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "The password should contain at least %s")
    String passwordShouldContainInfo(String requirements);

    /**
     * A prompt to double check the user is really sure they want to set password.
     *
     * @return a {@link String} for the message.
     */
    @Message(id = Message.NONE, value = "Are you sure you want to use the password entered yes/no?")
    String sureToSetPassword();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#USAGE} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Usage: ./add-user.sh [args...]%nwhere args include:")
    String argUsage();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#APPLICATION_USERS} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "If set add an application user instead of a management user")
    String argApplicationUsers();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#DOMAIN_CONFIG_DIR_USERS} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Define the location of the domain config directory.")
    String argDomainConfigDirUsers();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#SERVER_CONFIG_DIR_USERS} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Define the location the server config directory.")
    String argServerConfigDirUsers();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#USER_PROPERTIES} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "The file name of the user properties file which can be an absolute path.")
    String argUserProperties();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#GROUP_PROPERTIES} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "The file name of the group properties file which can be an absolute path. (If group properties is specified then user properties MUST also be specified).")
    String argGroupProperties();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#PASSWORD} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Password of the user, this will be checked against the password requirements defined within the add-user.properties configuration")
    String argPassword();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#USER} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the user")
    String argUser();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#REALM} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the realm used to secure the management interfaces (default is \"ManagementRealm\")")
    String argRealm();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#SILENT} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Activate the silent mode (no output to the console)")
    String argSilent();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#ROLE} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Comma-separated list of roles for the user.")
    String argRole();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#GROUPS} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Comma-separated list of groups for the user.")
    String argGroup();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#ENABLE} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Enable the user")
    String argEnable();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#DISABLE} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Disable the user")
    String argDisable();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#CONFIRM_WARNING} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Automatically confirm warning in interactive mode")
    String argConfirmWarning();

    /**
     * Instructions for the {@link org.jboss.as.domain.management.security.adduser.AddUser.CommandLineArgument#HELP} command line argument.
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Display this message and exit")
    String argHelp();

    /**
     * The long value a user would enter to indicate 'yes'
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The value a user would enter to indicate 'yes'.
     */
    @Message(id = Message.NONE, value = "yes")
    String yes();

    /**
     * The short value a user would enter to indicate 'yes'
     *
     * If no short value is available for a specific translation then only the long value will be accepted.
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The short value a user would enter to indicate 'yes'.
     */
    @Message(id = Message.NONE, value = "y")
    String shortYes();

    /**
     * The long value a user would enter to indicate 'no'
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The value a user would enter to indicate 'no'.
     */
    @Message(id = Message.NONE, value = "no")
    String no();

    /**
     * The short value a user would enter to indicate 'no'
     *
     * If no short value is available for a specific translation then only the long value will be accepted.
     *
     * This String should be the lower case representation in the respective locale.
     *
     * @return The short value a user would enter to indicate 'no'.
     */
    @Message(id = Message.NONE, value = "n")
    String shortNo();

    /**
     * Message to check if an alternative realm is really desired.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "The realm name supplied must match the name used by the server configuration which by default would be '%s'")
    String alternativeRealm(final String defaultRealm);

    /**
     * Confirmation of realm choice.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Are you sure you want to set the realm to '%s'")
    String realmConfirmation(final String chosenRealm);

    /**
     * Display password requirements and the command line argument option to modify these restrictions
     */
    @Message(id = Message.NONE, value = "Password requirements are listed below. To modify these restrictions edit the add-user.properties configuration file.")
    String passwordRequirements();

    /**
     * Display password recommendations and the command line argument option to modify these restrictions
     */
    @Message(id = Message.NONE, value = "Password recommendations are listed below. To modify these restrictions edit the add-user.properties configuration file.")
    String passwordRecommendations();

    /**
     * Message stating command line supplied realm name in use.
     */
    @Message(id = Message.NONE, value = "Using realm '%s' as specified on the command line.")
    String userSuppliedRealm(final String realmName);

    /**
     * Message stating discovered realm name in use.
     */
    @Message(id = Message.NONE, value = "Using realm '%s' as discovered from the existing property files.")
    String discoveredRealm(final String realmName);



    //PUT YOUR NUMBERED MESSAGES ABOVE THE id=NONE ones!
}
