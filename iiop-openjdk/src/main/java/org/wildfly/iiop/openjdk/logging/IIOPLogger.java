/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.msc.service.StartException;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_PERMISSION;
import org.wildfly.iiop.openjdk.rmi.RMIIIOPViolationException;
import org.wildfly.iiop.openjdk.rmi.ir.IRConstructionException;

import javax.naming.ConfigurationException;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import java.io.IOException;
import java.net.MalformedURLException;

import static org.jboss.logging.Logger.Level.*;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@MessageLogger(projectCode = "WFLYIIOP", length = 4)
public interface IIOPLogger extends BasicLogger {

    IIOPLogger ROOT_LOGGER = Logger.getMessageLogger(IIOPLogger.class, "org.wildfly.iiop.openjdk");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating IIOP Subsystem")
    void activatingSubsystem();

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Error fetching CSIv2Policy")
    void failedToFetchCSIv2Policy(@Cause Throwable cause);


    @LogMessage(level = WARN)
    @Message(id = 3, value = "Caught exception while encoding GSSUPMechOID")
    void caughtExceptionEncodingGSSUPMechOID(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Internal error")
    void logInternalError(@Cause Exception cause);

    @LogMessage(level = ERROR)
    @Message(id = 5, value = "Failed to create CORBA naming context")
    void failedToCreateNamingContext(@Cause Exception cause);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "Unbind failed for %s")
    void failedToUnbindObject(Object name);

    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Failed to obtain JSSE security domain with name %s")
    void failedToObtainJSSEDomain(String securityDomain);

    @LogMessage(level = INFO)
    @Message(id = 8, value = "CORBA Naming Service started")
    void corbaNamingServiceStarted();

    @LogMessage(level = INFO)
    @Message(id = 9, value = "CORBA ORB Service started")
    void corbaORBServiceStarted();

    @LogMessage(level = WARN)
    @Message(id = 10, value = "Compatibility problem: Class javax.rmi.CORBA.ClassDesc does not conform to the Java(TM) Language to IDL Mapping Specification (01-06-07), section 1.3.5.11")
    void warnClassDescDoesNotConformToSpec();

    @LogMessage(level = WARN)
    @Message(id = 11, value = "Could not deactivate IR object")
    void warnCouldNotDeactivateIRObject(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 12, value = "Could not deactivate anonymous IR object")
    void warnCouldNotDeactivateAnonIRObject(@Cause Throwable cause);

    @Message(id = 13, value = "SSL support has been enabled but no security domain or client/server SSL contexts have been specified")
    OperationFailedException noSecurityDomainOrSSLContextsSpecified();

    @Message(id = 14, value = "Unexpected exception")
    RuntimeException unexpectedException(@Cause Throwable cause);

    @Message(id = 15, value = "Unexpected ContextError in SAS reply")
    NO_PERMISSION unexpectedContextErrorInSASReply(@Param int minorCode, @Param CompletionStatus status);

    @Message(id = 16, value = "Could not parse SAS reply: %s")
    MARSHAL errorParsingSASReply(Exception e, @Param int minorCode, @Param CompletionStatus status);

    @Message(id = 17, value = "Could not register initial reference for SASCurrent")
    RuntimeException errorRegisteringSASCurrentInitRef(@Cause Throwable cause);

    @Message(id = 18, value = "SAS context does not exist")
    NO_PERMISSION missingSASContext();

    @Message(id = 19, value = "Could not decode initial context token")
    NO_PERMISSION errorDecodingInitContextToken();

    @Message(id = 20, value = "Could not decode target name in initial context token")
    NO_PERMISSION errorDecodingTargetInContextToken();

    @Message(id = 21, value = "Could not decode incoming principal name")
    NO_PERMISSION errorDecodingPrincipalName();

    @Message(id = 22, value = "Exception decoding context data in %s")
    RuntimeException errorDecodingContextData(String interceptorName, @Cause Throwable e);

    @Message(id = 23, value = "Batch size not numeric: %s")
    IllegalArgumentException illegalBatchSize(String batch);

    @Message(id = 24, value = "Error getting binding list")
    NamingException errorGettingBindingList();

    @Message(id = 25, value = "Error generating object via object factory")
    NamingException errorGeneratingObjectViaFactory();

    @Message(id = 26, value = "Error constructing context: either ORB or NamingContext must be supplied")
    ConfigurationException errorConstructingCNCtx();

    @Message(id = 27, value = "%s does not name a NamingContext")
    ConfigurationException notANamingContext(String name);

    @Message(id = 28, value = "Cannot convert IOR to NamingContext: %s")
    ConfigurationException errorConvertingIORToNamingCtx(String ior);

    @Message(id = 29, value = "ORB.resolve_initial_references(\"NameService\") does not return a NamingContext")
    ConfigurationException errorResolvingNSInitRef();

    @Message(id = 30, value = "COS Name Service not registered with ORB under the name 'NameService'")
    NamingException cosNamingNotRegisteredCorrectly();

    @Message(id = 31, value = "Cannot connect to ORB")
    NamingException errorConnectingToORB();

    @Message(id = 32, value = "Invalid IOR or URL: %s")
    NamingException invalidURLOrIOR(String ior);

    @Message(id = 33, value = "Invalid object reference:  %s")
    NamingException invalidObjectReference(String ior);

    @Message(id = 34, value = "%s does not contain an IOR")
    ConfigurationException urlDoesNotContainIOR(String url);

    @Message(id = 35, value = "Only instances of org.omg.CORBA.Object can be bound")
    IllegalArgumentException notACorbaObject();

    @Message(id = 36, value = "No object reference bound for specified name")
    NamingException noReferenceFound();

    @Message(id = 37, value = "Invalid empty name")
    InvalidNameException invalidEmptyName();

    @Message(id = 38, value = "%s: unescaped \\ at end of component")
    InvalidNameException unescapedCharacter(String cnString);

    @Message(id = 39, value = "%s: Invalid character being escaped")
    InvalidNameException invalidEscapedCharacter(String cnString);

    @Message(id = 40, value = "Invalid %s URL: %s")
    MalformedURLException invalidURL(String protocol, String url);

    @Message(id = 41, value = "Problem with PortableRemoteObject.toStub(); object not exported or stub not found")
    ConfigurationException problemInvokingPortableRemoteObjectToStub();

    @Message(id = 42, value = "Cannot invoke javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
    ConfigurationException cannotInvokePortableRemoteObjectToStub();

    @Message(id = 43, value = "No method definition for javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
    IllegalStateException noMethodDefForPortableRemoteObjectToStub();

    @Message(id = 44, value = "Problem invoking javax.rmi.CORBA.Stub.connect()")
    ConfigurationException problemInvokingStubConnect();

    @Message(id = 45, value = "Cannot invoke javax.rmi.CORBA.Stub.connect()")
    ConfigurationException cannotInvokeStubConnect();

    @Message(id = 46, value = "No method definition for javax.rmi.CORBA.Stub.connect(org.omg.CORBA.ORB)")
    IllegalStateException noMethodDefForStubConnect();

    @Message(id = 47, value = "Invalid IIOP URL version: %s")
    MalformedURLException invalidIIOPURLVersion(String version);

    @Message(id = 48, value = "javax.rmi packages not available")
    ConfigurationException unavailableRMIPackages();

    @Message(id = 49, value = "ISO-Latin-1 decoder unavailable")
    MalformedURLException unavailableISOLatin1Decoder();

    @Message(id = 50, value = "Invalid URI encoding: %s")
    MalformedURLException invalidURIEncoding(String encoding);

    @Message(id = 51, value = "Error configuring domain socket factory: failed to lookup JSSE security domain")
    ConfigurationException failedToLookupJSSEDomain();

    @Message(id = 52, value = "keyManager[] is null for security domain %s")
    IOException errorObtainingKeyManagers(String securityDomain);

    @Message(id = 53, value = "Failed to get SSL context")
    IOException failedToGetSSLContext(@Cause Throwable cause);

    @Message(id = 54, value = "Failed to start the JBoss Corba Naming Service")
    StartException failedToStartJBossCOSNaming(@Cause Throwable cause);

    @Message(id = 55, value = "Foreign Transaction")
    UnsupportedOperationException foreignTransaction();

    @Message(id = 56, value = "Exception raised during encoding")
    RuntimeException errorEncodingContext(@Cause Throwable cause);

    @Message(id = 57, value = "Exception getting slot in TxServerInterceptor")
    RuntimeException errorGettingSlotInTxInterceptor(@Cause Throwable cause);

    @Message(id = 58, value = "Exception setting slot in TxServerInterceptor")
    RuntimeException errorSettingSlotInTxInterceptor(@Cause Throwable cause);

    @Message(id = 59, value = "Cannot analyze a null class")
    IllegalArgumentException cannotAnalyzeNullClass();

    @Message(id = 60, value = "Bad type for a constant: %s")
    IllegalArgumentException badConstantType(String type);

    @Message(id = 61, value = "Cannot analyze special class: %s")
    IllegalArgumentException cannotAnalyzeSpecialClass(String type);

    @Message(id = 62, value = "Not an accessor: %s")
    IllegalArgumentException notAnAccessor(String name);

    @Message(id = 63, value = "Not a class or interface: %s")
    IllegalArgumentException notAnClassOrInterface(String name);

    @Message(id = 64, value = "Class %s is not an interface")
    IllegalArgumentException notAnInterface(String name);

    @Message(id = 65, value = "Not a primitive type: %s")
    IllegalArgumentException notAPrimitive(String type);

    @Message(id = 66, value = "Field %s of interface %s is a constant, but it is not primitive or String")
    RMIIIOPViolationException badRMIIIOPConstantType(String field, String intface, @Param String section);

    @Message(id = 67, value = "Exception type %s must be a checked exception class")
    RMIIIOPViolationException badRMIIIOPExceptionType(String type, @Param String section);

    @Message(id = 68, value = "All interface methods must throw javax.rmi.RemoteException but method %s of interface %s does not")
    RMIIIOPViolationException badRMIIIOPMethodSignature(String method, String intface, @Param String section);

    @Message(id = 69, value = "Name cannot be null, empty or qualified")
    IllegalArgumentException nameCannotBeNullEmptyOrQualified();

    @Message(id = 70, value = "Primitive types have no IR IDs")
    IllegalArgumentException primitivesHaveNoIRIds();

    @Message(id = 71, value = "No SHA message digest available")
    RuntimeException unavailableSHADigest(@Cause Throwable cause);

    @Message(id = 72, value = "Unknown primitive type: %s")
    RuntimeException unknownPrimitiveType(String type);

    @Message(id = 73, value = "Cannot analyze java.lang.String: it is a special case")
    IllegalArgumentException cannotAnalyzeStringType();

    @Message(id = 74, value = "Cannot analyze java.lang.Class: it is a special case")
    IllegalArgumentException cannotAnalyzeClassType();

    @Message(id = 75, value = "Value type %s cannot implement java.rmi.Remote")
    RMIIIOPViolationException valueTypeCantImplementRemote(String type, @Param String section);

    @Message(id = 76, value = "Value type %s cannot be a proxy or inner class")
    RMIIIOPViolationException valueTypeCantBeProxy(String type);

    @Message(id = 77, value = "Error loading class %s")
    RuntimeException errorLoadingClass(String type, @Cause Throwable cause);

    @Message(id = 78, value = "No read method in helper class %s")
    RuntimeException noReadMethodInHelper(String type, @Cause Throwable cause);

    @Message(id = 79, value = "No write method in helper class %s")
    RuntimeException noWriteMethodInHelper(String type, @Cause Throwable cause);

    @Message(id = 80, value = "Error unmarshaling %s")
    RuntimeException errorUnmarshaling(Class<?> type, @Cause Throwable cause);

    @Message(id = 81, value = "Error marshaling %s")
    RuntimeException errorMarshaling(Class<?> type, @Cause Throwable cause);

    @Message(id = 82, value = "Cannot obtain exception repository id for %s")
    RuntimeException cannotObtainExceptionRepositoryID(String type, @Cause Throwable cause);

    @Message(id = 83, value = "Cannot marshal parameter: unexpected number of parameters")
    RuntimeException errorMashalingParams();

    @Message(id = 84, value = "Cannot change RMI/IIOP mapping")
    BAD_INV_ORDER cannotChangeRMIIIOPMapping();

    @Message(id = 85, value = "Bad kind %d for TypeCode")
    RuntimeException badKindForTypeCode(int kind);

    @Message(id = 86, value = "Wrong interface repository")
    IRConstructionException wrongInterfaceRepository();

    @Message(id = 87, value = "Duplicate repository name")
    IRConstructionException duplicateRepositoryName();

    @Message(id = 88, value = "Invalid null class")
    IllegalArgumentException invalidNullClass();

    @Message(id = 89, value = "Bad class %s for a constant")
    IRConstructionException badClassForConstant(String className);

    @Message(id = 90, value = "TypeCode for class %s is unknown")
    IRConstructionException unknownTypeCodeForClass(String className);

    @Message(id = 91, value = "TypeCode for class %s already established")
    IRConstructionException duplicateTypeCodeForClass(String className);

    @Message(id = 92, value = "Name collision while creating package")
    IRConstructionException collisionWhileCreatingPackage();

    @Message(id = 93, value = "Class %s is not an array class")
    IRConstructionException classIsNotArray(String className);

    @Message(id = 94, value = "Cannot destroy RMI/IIOP mapping")
    BAD_INV_ORDER cannotDestroyRMIIIOPMapping();

    @Message(id = 95, value = "Bad kind for super valuetype of %s")
    IRConstructionException badKindForSuperValueType(String id);

    @Message(id = 96, value = "ValueDef %s unable to resolve reference to implemented interface %s")
    IRConstructionException errorResolvingRefToImplementedInterface(String id, String intface);

    @Message(id = 97, value = "ValueDef %s unable to resolve reference to abstract base valuetype %s")
    IRConstructionException errorResolvingRefToAbstractValuetype(String id, String valuetype);

    @Message(id = 98, value = "Failed to resolve initial reference %s")
    StartException errorResolvingInitRef(String refName, @Cause Throwable cause);

    @Message(id = 99, value = "Failed to create POA from parent")
    StartException errorCreatingPOAFromParent(@Cause Throwable cause);

    @Message(id = 100, value = "Unable to instantiate POA: either the running ORB or the parent POA must be specified")
    StartException invalidPOACreationArgs();

    @Message(id = 101, value = "Failed to activate POA")
    StartException errorActivatingPOA(@Cause Throwable cause);

    @Message(id = 102, value = "Caught exception destroying Iterator %s")
    INTERNAL exceptionDestroingIterator(String cause);

    @Message(id = 103, value = "IOR settings imply ssl connections usage, but secure connections have not been configured")
    OperationFailedException sslNotConfigured();

    @Message(id = 104, value = "Inconsistent transport-config configuration: %s is supported, please configure it to %s value")
    String inconsistentSupportedTransportConfig(final String transportAttributeName, final String suggested);

    @Message(id = 105, value = "Inconsistent transport-config configuration: %s is not supported, please remove it or configure it to none value")
    String inconsistentUnsupportedTransportConfig(final String transportAttributeName);

    @Message(id = 106, value = "Inconsistent transport-config configuration: %s is set to true, please configure %s as required")
    String inconsistentRequiredTransportConfig(final String requiredAttributeName, final String transportAttributeName);


//    @Message(id = 108, value = "Security attribute server-requires-ssl is not supported in previous iiop-openjdk versions and can't be converted")
//    String serverRequiresSslNotSupportedInPreviousVersions();

    @LogMessage(level = WARN)
    @Message(id = 109, value = "SSL socket is required by server but secure connections have not been configured")
    void cannotCreateSSLSocket();

    @Message(id = 110, value = "Client requires SSL but server does not support it")
    IllegalStateException serverDoesNotSupportSsl();

    @Message(id = 111, value = "SSL has not been configured but ssl-port property has been specified - the connection will use clear-text protocol")
    String sslPortWithoutSslConfiguration();

//    @Message(id = 112, value = "Security initializer was set to 'elytron' but no authentication-context has been specified")
//    OperationFailedException elytronInitializerMissingAuthContext();

    @Message(id = 113, value = "Authentication context has been defined but it is ineffective because the security initializer is not set to 'elytron'")
    OperationFailedException ineffectiveAuthenticationContextConfiguration();

    @Message(id = 114, value = "Elytron security initializer not supported in previous iiop-openjdk versions and can't be converted")
    String elytronInitializerNotSupportedInPreviousVersions();

    @Message(id = 115, value = "No IIOP socket bindings have been configured")
    IllegalStateException noSocketBindingsConfigured();

    @LogMessage(level = WARN)
    @Message(id = 117, value = "CLEARTEXT in IIOP subsystem won't be used because server-requires-ssl parameter have been set to true")
    void wontUseCleartextSocket();
}
