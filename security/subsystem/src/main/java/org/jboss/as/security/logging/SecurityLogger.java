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

package org.jboss.as.security.logging;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYSEC", length = 4)
public interface SecurityLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    SecurityLogger ROOT_LOGGER = Logger.getMessageLogger(SecurityLogger.class, "org.jboss.as.security");

//   /** Logs a message indicating the current version of the PicketBox library
//    *
//    * @param version a {@link String} representing the current version
//    */
//   @LogMessage(level = Level.INFO)
//   @Message(id = 1, value = "Current PicketBox version=%s")
//   void currentVersion(String version);
//
//   /**
//    * Logs a message indicating that the security subsystem is being activated
//    */
//   @LogMessage(level = Level.INFO)
//   @Message(id = 2, value = "Activating Security Subsystem")
//   void activatingSecuritySubsystem();

//   /**
//    * Logs a message indicating that there was an exception while trying to delete the Jakarta Authorization Policy
//    * @param t the underlying exception
//    */
//   @LogMessage(level = Level.WARN)
//   @Message(id = 3, value = "Error deleting Jakarta Authorization Policy")
//   void errorDeletingJACCPolicy(@Cause Throwable t);

//    /**
//     * Creates an exception indicating the inability to get the {@link org.jboss.modules.ModuleClassLoader}
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 4, value = "Unable to get the Module Class Loader")
//    IllegalStateException unableToGetModuleClassLoader(@Cause Throwable e);
//
//    /**
//     * Creates an exception indicating that the operation is not supported
//     *
//     * @return an {@link javax.naming.OperationNotSupportedException} for the error.
//     */
//    @Message(id = 5, value = "Operation not supported : %s")
//    OperationNotSupportedException operationNotSupported(Method method);
//
//    /**
//     * Creates an exception indicating that the module name was missing
//     * @param name the missing module name
//     * @return {@link IllegalArgumentException}
//     */
//    @Message(id = 6, value = "Missing module name for the %s")
//    IllegalArgumentException missingModuleName(String name);

    /**
     * Creates a {@link RuntimeException}
     * @param e the underlying exception
     * @return the exception
     */
    @Message(id = 7, value = "Runtime Exception:")
    RuntimeException runtimeException(@Cause Throwable e);

//    /**
//     * Creates a {@link org.jboss.modules.ModuleLoadException}
//     * @param e the underlying exception
//     * @return
//     */
//    @Message(id = 8, value = "Module Load Exception:")
//    ModuleLoadException moduleLoadException(@Cause Throwable e);

//    /**
//     * Creates an exception indicating that the name passed to jndi is null or empty
//     * @return {@link javax.naming.InvalidNameException}
//     */
//    @Message(id = 9, value = "Name cannot be null or empty")
//    InvalidNameException nullName();

//    /**
//     * Create a {@link javax.security.auth.login.LoginException} to indicate that there was no User Principal even though
//     * a remoting connection existed
//     * @return {@link javax.security.auth.login.LoginException}
//     */
//    @Message(id = 10, value = "Remoting connection found but no UserPrincipal.")
//    LoginException remotingConnectionWithNoUserPrincipal();

//    /**
//     * Create a {@link IllegalArgumentException} when a null argument is passed
//     * @param arg an argument that is null
//     * @return {@link IllegalArgumentException}
//     */
//    @Message(id = 11, value = "Argument %s is null")
//    IllegalArgumentException nullArgument(String arg);

//    /**
//     * Create a {@link org.jboss.msc.service.StartException} to indicate that a service could not be started
//     * @param service the name of the service
//     * @param t underlying exception
//     * @return {@link org.jboss.msc.service.StartException}
//     */
//    @Message(id = 12, value = "Unable to start the %s service")
//    StartException unableToStartException(String service, @Cause Throwable t);

//    /**
//     * Create a {@link ClassNotFoundException} to indicate that a class could not be found
//     * @param name name of the class
//     * @return {@link ClassNotFoundException}
//     */
//    @Message(id = 13, value = "Class not found : %s")
//    ClassNotFoundException cnfe(String name);

//    /**
//     * Create a {@link ClassNotFoundException} to indicate that a class could not be found
//     * @param name  name of the class
//     * @param t underlying exception
//     * @return {@link ClassNotFoundException}
//     */
//    @Message(id = 14, value = "Class not found : %s")
//    ClassNotFoundException cnfeThrow(String name, @Cause Throwable t);

    /**
     * Create a {@link SecurityException}
     * @param t underlying exception
     * @return {@link SecurityException}
     */
    @Message(id = 15, value = "Security Exception")
    SecurityException securityException(@Cause Throwable t);

//    /**
//     * Create a {@link SecurityException}
//     * @param msg message that is passed in creating the exception
//     * @return {@link SecurityException}
//     */
//    @Message(id = 16, value = "Security Exception: %s")
//    SecurityException securityException(String msg);

//    /**
//     * Create a {@link org.jboss.as.server.services.security.VaultReaderException} to indicate there was an exception while reading from the vault
//     * @param t underlying exception
//     * @return {@link org.jboss.as.server.services.security.VaultReaderException}
//     */
//    @Message(id = 17, value = "Vault Reader Exception:")
//    VaultReaderException vaultReaderException(@Cause Throwable t);

    /**
     * Exception indicates that the method being used indicates a misuse of this class
     *
     * @return {@link UnsupportedOperationException}
     */
    @Message(id = 18, value = "Use the ResourceDescriptionResolver variant")
    UnsupportedOperationException unsupportedOperationExceptionUseResourceDesc();

    /**
     * Create a {@link UnsupportedOperationException} to indicate that the intended operation is not supported
     * @return {@link UnsupportedOperationException}
     */
    @Message(id = 19, value = "Unsupported Operation")
    UnsupportedOperationException unsupportedOperation();

//    /**
//     * Create a {@link IllegalArgumentException} to indicate an argument to a method was illegal
//     * @param str string message to the exception
//     * @return {@link IllegalArgumentException}
//     */
//    @Message(id = 20, value = "Illegal Argument:%s")
//    IllegalArgumentException illegalArgument(String str);
//
//    /**
//     * Create a {@link javax.xml.stream.XMLStreamException} indicating a failure during the stax parsing
//     * @param msg failure description
//     * @param loc current location of the stax parser
//     * @return {@link javax.xml.stream.XMLStreamException}
//     */
//    @Message(id = 21, value = "Illegal Argument:%s")
//    XMLStreamException xmlStreamException(String msg, @Param Location loc);

    /**
     * Create a {@link XMLStreamException} to indicate that the security domain configuration cannot have both JAAS and JASPI config
     * @param loc the current location of the stax parser
     * @return {@link XMLStreamException}
     */
    @Message(id = 22, value = "A security domain can have either an <authentication> or <authentication-jaspi> element, not both")
    XMLStreamException xmlStreamExceptionAuth(@Param Location loc);

    /**
     * Creates a {@link XMLStreamException} to indicate a missing required attribute
     * @param a the first attribute
     * @param b the second attribute
     * @param loc the current location of the stax parser
     * @return {@link XMLStreamException}
     */
    @Message(id = 23, value = "Missing required attribute: either %s or %s must be present")
    XMLStreamException xmlStreamExceptionMissingAttribute(String a, String b, @Param Location loc);

//    /**
//     * Create a {@link IllegalArgumentException} to indicate that the auth-module references a login module stack that does not exist
//     * @param str login module stack name
//     * @return {@link IllegalArgumentException}
//     */
//    @Message(id = 24, value = "auth-module references a login module stack that doesn't exist::%s")
//    IllegalArgumentException loginModuleStackIllegalArgument(String str);
//
//    /**
//     * Create a {@link IllegalArgumentException} when the path address does not contain a security domain name
//     * @return {@link IllegalArgumentException}
//     */
//    @Message(id = 25, value = "Address did not contain a security domain name")
//    IllegalArgumentException addressDidNotContainSecurityDomain();

//    /**
//     * Create a {@link SecurityException} to indicate that the vault is not initialized
//     * @return {@link SecurityException}
//     */
//    @Message(id = 26, value = "Vault is not initialized")
//    SecurityException vaultNotInitializedException();

//    /**
//     * Create a {@link SecurityException} to indicate that the user is invalid.
//     * @return {@link SecurityException}
//     */
//    @Message(id = 27, value = "Invalid User")
//    SecurityException invalidUserException();
//
//    /**
//     * Create a {@link SecurityException} to indicate that the security management has not been injected
//     * @return {@link SecurityException}
//     */
//    @Message(id = 28, value = "Security Management not injected")
//    SecurityException securityManagementNotInjected();

//    /**
//     * Create a {@link SecurityException} to indicate that the specified realm has not been found.
//     * @return {@link SecurityException}
//     */
//    @Message(id = 29, value = "Security realm '%s' not found.")
//    SecurityException realmNotFound(final String name);

//    /**
//     * Create a {@link SecurityException} to indicate that no password validation mechanism has been identified.
//     * @return {@link SecurityException}
//     */
    //@Message(id = 30, value = "No suitable password validation mechanism identified for realm '%s'")
    //SecurityException noPasswordValidationAvailable(final String realmName);

//    /**
//     * Create a {@link LoginException} to indicate a failure calling the security realm.
//     * @return {@link LoginException}
//     */
//    @Message(id = 31, value = "Failure calling CallbackHandler '%s'")
//    LoginException failureCallingSecurityRealm(String cause);
//
//    /**
//     * Create an OperationFailedException to indicate a failure to find an authentication cache
//     * @return the exception
//     */
//    @Message(id = 32, value = "No authentication cache for security domain '%s' available")
//    OperationFailedException noAuthenticationCacheAvailable(String securityDomain);
//
//    /**
//     * Create an IllegalStateFoundException to indicate no UserPrincipal was found on the underlying connection.
//     * @return the exception
//     */
//    @Message(id= 33, value = "No UserPrincipalFound constructing RemotingConnectionPrincipal.")
//    IllegalStateException noUserPrincipalFound();
//
//    @Message(id = 34, value = "Interrupted waiting for security domain '%s'")
//    OperationFailedException interruptedWaitingForSecurityDomain(String securityDomainName);
//
//    @Message(id = 35, value = "Required security domain is not available '%s'")
//    OperationFailedException requiredSecurityDomainServiceNotAvailable(String securityDomainName);

//    @Message(id = 36, value = "At least one attribute is to be defined")
//    OperationFailedException requiredJSSEConfigurationAttribute();

//    /**
//     * Create an Exception when KeyStore cannot be located with example how to create one.
//     *
//     * @param keystoreURL nonexistent keystore URL
//     * @param keystoreURLExample  example keystore url
//     * @return the exception
//     */
//    @Message(id = 37, value = "Keystore '%s' doesn't exist."
//            + "\nkeystore could be created: "
//            + "keytool -genseckey -alias Vault -storetype jceks -keyalg AES -keysize 128 -storepass secretsecret -keypass secretsecret -keystore %s")
//    Exception keyStoreDoesnotExistWithExample(final String keystoreURL, final String keystoreURLExample);

//    /**
//     * Create an Exception when one cannot write to the KeyStore or it is not a file.
//     *
//     * @param keystoreURL URL of the keystore
//     * @return the exception
//     */
//    @Message(id = 38, value = "Keystore [%s] is not writable or not a file.")
//    Exception keyStoreNotWritable(final String keystoreURL);

//    /**
//     * Create an exception when KeyStore password is not specified.
//     *
//     * @return the exception
//     */
//    @Message(id = 39, value = "Keystore password has to be specified.")
//    Exception keyStorePasswordNotSpecified();

//    /**
//     * Create an exception when encryption directory is not specified.
//     *
//     * @return
//     */
//    @Message(id = 40, value = "Encryption directory has to be specified.")
//    Exception encryptionDirectoryHasToBeSpecified();

//    /**
//     * Create an exception when encryption directory does not exist or is not a directory.
//     *
//     * @param directory directory name
//     * @return the exception
//     */
//    @Message(id = 41, value = "Encryption directory is not a directory or doesn't exist. (%s)")
//    Exception encryptionDirectoryDoesNotExist(final String directory);

//    /**
//     * Create an exception when encryption directory cannot be created.
//     *
//     * @param directory directory name
//     * @return the exception
//     */
//    @Message(id = 42, value = "Cannot create encryption directory %s")
//    Exception cannotCreateEncryptionDirectory(final String directory);

//    /**
//     * Create an exception when iteration count is out of range.
//     *
//     * @param iteration iteration count
//     * @return the exception
//     */
//    @Message(id = 43, value = "Iteration count has to be within 1 - " + Integer.MAX_VALUE + ", but it is %s.")
//    Exception iterationCountOutOfRange(final String iteration);

//    /**
//     * Create an exception when salt has different length than 8.
//     *
//     * @return the exception
//     */
//    @Message(id = 44, value = "Salt has to be exactly 8 characters long.")
//    Exception saltWrongLength();

//    /**
//     * Unspecified exception encountered.
//     *
//     * @return the exception
//     */
//    @Message(id = 45, value = "Exception encountered:")
//    Exception securityVaultException(@Cause SecurityVaultException cause);

//    /**
//     * Create an exception when Vault alias is not specified.
//     *
//     * @return the exception
//     */
//    @Message(id = 46, value = "Vault alias has to be specified.")
//    Exception vaultAliasNotSpecified();

//    /**
//     * Display string at the end of successful attribute creation.
//     *
//     * @param VaultBlock  name of vault block
//     * @param attributeName name of value attribute
//     * @param configurationString configuration details
//     * @return the localized text
//     */
//    @Message(id = 47, value =
//            "Secured attribute value has been stored in Vault.\n" +
//            "Please make note of the following:\n" +
//            "********************************************\n" +
//            "Vault Block:%s\n" + "Attribute Name:%s\n" +
//            "Configuration should be done as follows:\n" +
//            "%s\n" +
//            "********************************************")
//    String vaultAttributeCreateDisplay(String VaultBlock, String attributeName, String configurationString);

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 48, value = "Vault Configuration commands in WildFly for CLI:")
//    String vaultConfigurationTitle();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 49, value = "No console.")
//    String noConsole();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 56, value = "Initializing Vault")
//    String initializingVault();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 57, value = "Vault is initialized and ready for use")
//    String vaultInitialized();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 58, value = "Handshake with Vault complete")
//    String handshakeComplete();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 59, value = "Exception encountered:")
//    String exceptionEncountered();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @deprecated do not use this message to build confirmation message
     *
     * @return the localized text
     */
    @Deprecated
    @Message(id = 61, value = " again: ")
    String passwordAgain();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 68, value = "Problem while parsing command line parameters:")
//    String problemParsingCommandLineParameters();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 80, value = "Secured attribute (password) already exists.")
//    String cmdLineSecuredAttributeAlreadyExists();

//    /**
//     * i18n version of string from Vault Tool utility
//     *
//     * @return the localized text
//     */
//    @Message(id = 81, value = "Secured attribute (password) doesn't exist.")
//    String cmdLineSecuredAttributeDoesNotExist();

//    /**
//     * Keystore parameter type checking
//     *
//     * @return
//     */
//    @Message(id = 84, value = "'%s' parameter type or length is incorrect")
//    IllegalArgumentException incorrectKeystoreParameters(final String keystoreName);

//    /**
//     * Creates an exception indicating the inability to find a JSSE-enabled security domain with the specified name.
//     *
//     * @return a {@link StartException} instance.
//     */
//    @Message(id = 100, value = "Legacy security domain %s doesn't contain a valid JSSE configuration")
//    StartException unableToLocateJSSEConfig(final String legacyDomainName);
//
//    /**
//     * Creates an exception indicating the inability to find a component (keystore, truststore, keymanager, etc) in
//     * the specified JSSE security domain.
//     *
//     * @return a {@link StartException} instance.
//     */
//    @Message(id = 101, value = "Unable to find a %s configuration in JSSE security domain %s")
//    StartException unableToLocateComponentInJSSEDomain(final String componentName, final String legacyDomainName);

//    /**
//     * Creates an exception indicating that the expected manager type was not found in the JSSE security domain.
//     *
//     * @param managerName the name of the manager being retrieved (KeyManager or TrustManager).
//     * @param managerType the expected type.
//     * @return a {@link StartException} instance.
//     */
//    @Message(id = 102, value = "Could not find a %s of type %s in the JSSE security domain %s")
//    StartException expectedManagerTypeNotFound(final String managerName, final String managerType, final String legacyDomainName);
//
//    /**
//     * Creates an exception indicating that an {@link org.wildfly.security.authz.AuthorizationIdentity} could not be created
//     * because a valid authenticated Subject was not established yet.
//     *
//     * @return a {@link IllegalStateException} instance.
//     */
//    @Message(id = 103, value = "Unable to create AuthorizationIdentity: no authenticated Subject was found")
//    IllegalStateException unableToCreateAuthorizationIdentity();
//
//    @LogMessage(level = Level.WARN)
//    @Message(id = 104, value = "Default %s cache capability missing.  Assuming %s as default-cache.")
//    void defaultCacheRequirementMissing(String containerName, String legacyCacheName);

    @Message(id=105, value = "Unable to initialize legacy JACC support while elytron JACC support is enabled.")
    IllegalStateException unableToEnableJaccSupport();
}
