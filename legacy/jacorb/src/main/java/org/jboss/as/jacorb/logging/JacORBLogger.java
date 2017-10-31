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

package org.jboss.as.jacorb.logging;

import static org.jboss.logging.Logger.Level.WARN;

import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYORB", length = 4)
public interface JacORBLogger extends BasicLogger {

    JacORBLogger ROOT_LOGGER = Logger.getMessageLogger(JacORBLogger.class, "org.jboss.as.jacorb");

//    @LogMessage(level = INFO)
//    @Message(id = 1, value = "Activating JacORB Subsystem")
//    void activatingSubsystem();
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 2, value = "IOR security config metadata: %s")
//    void debugIORSecurityConfigMetaData(IORSecurityConfigMetaData metaData);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 3, value = "CSIv2Policy not found in IORInfo")
//    void csiv2PolicyNotFoundInIORInfo();
//
//    @LogMessage(level = ERROR)
//    @Message(id = 4, value = "Error fetching CSIv2Policy")
//    void failedToFetchCSIv2Policy(@Cause Throwable cause);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 5, value = "Method createSSLTaggedComponent() called with null metadata")
//    void createSSLTaggedComponentWithNullMetaData();
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 6, value = "Method createSecurityTaggedComponent() called with null metadata")
//    void createSecurityTaggedComponentWithNullMetaData();
//
//    @LogMessage(level = WARN)
//    @Message(id = 7, value = "Caught exception while encoding GSSUPMechOID")
//    void caughtExceptionEncodingGSSUPMechOID(@Cause Throwable cause);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 8, value = "receive_reply: got SAS reply, type %d")
//    void traceReceiveReply(int type);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 9, value = "receive_exception: got SAS reply, type %d")
//    void traceReceiveException(int type);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 10, value = "receive_request: %s")
//    void traceReceiveRequest(String operation);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 11, value = "Received client authentication token")
//    void authTokenReceived();
//
//    @LogMessage(level = TRACE)
//    @Message(id = 12, value = "Received identity token")
//    void identityTokenReceived();
//
//    @LogMessage(level = TRACE)
//    @Message(id = 13, value = "send_reply: %s")
//    void traceSendReply(String operation);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 14, value = "send_exception: %s")
//    void traceSendException(String operation);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 15, value = "Bound name: %s")
//    void debugBoundName(String name);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 16, value = "Bound context: %s")
//    void debugBoundContext(String context);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 17, value = "Unbound: %s")
//    void debugUnboundObject(String context);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 18, value = "Internal error")
//    void logInternalError(@Cause Exception cause);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 19, value = "Failed to create CORBA naming context")
//    void failedToCreateNamingContext(@Cause Exception cause);
//
//    @LogMessage(level = WARN)
//    @Message(id = 20, value = "Unbind failed for %s")
//    void failedToUnbindObject(String name);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 21, value = "Unable to obtain id from object")
//    void failedToObtainIdFromObject(@Cause Exception cause);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 22, value = "Getting default ORB %s")
//    void debugGetDefaultORB(ORB orb);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 23, value = "Creating server socket factory: %s")
//    void traceServerSocketFactoryCreation(String factoryClass);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 24, value = "Obtained JSSE security domain with name %s")
//    void debugJSSEDomainRetrieval(String securityDomain);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 25, value = "Failed to obtain JSSE security domain with name %s")
//    void failedToObtainJSSEDomain(String securityDomain);
//
//    @LogMessage(level = TRACE)
//    @Message(id = 26, value = "Creating socket factory: %s")
//    void traceSocketFactoryCreation(String factoryClass);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 27, value = "Starting service %s")
//    void debugServiceStartup(String serviceName);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 28, value = "Stopping service %s")
//    void debugServiceStop(String serviceName);
//
//    @LogMessage(level = INFO)
//    @Message(id = 29, value = "CORBA Naming Service started")
//    void corbaNamingServiceStarted();
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 30, value = "Naming: [%s]")
//    void debugNamingServiceIOR(String ior);
//
//    @LogMessage(level = INFO)
//    @Message(id = 31, value = "CORBA ORB Service started")
//    void corbaORBServiceStarted();
//
//    @LogMessage(level = TRACE)
//    @Message(id = 32, value = "Intercepting receive_request_service_contexts, operation: %s")
//    void traceReceiveRequestServiceContexts(String operation);
//
//    @LogMessage(level = WARN)
//    @Message(id = 33, value = "Compatibility problem: Class javax.rmi.CORBA.ClassDesc does not conform to the Java(TM) Language to IDL Mapping Specification (01-06-07), section 1.3.5.11")
//    void warnClassDescDoesNotConformToSpec();
//
//    @LogMessage(level = WARN)
//    @Message(id = 34, value = "Could not deactivate IR object")
//    void warnCouldNotDeactivateIRObject(@Cause Throwable cause);
//
//    @LogMessage(level = DEBUG)
//    @Message(id = 35, value = "Exception converting CORBA servant to reference")
//    void debugExceptionConvertingServantToReference(@Cause Throwable cause);
//
//    @LogMessage(level = WARN)
//    @Message(id = 36, value = "Could not deactivate anonymous IR object")
//    void warnCouldNotDeactivateAnonIRObject(@Cause Throwable cause);
//
//
//    @Message(id = 37, value = "SSL support has been enabled but no security domain has been specified")
//    OperationFailedException noSecurityDomainSpecified();

    @Message(id = 38, value = "Illegal initializer value: %s. Should be one of [security,transactions]")
    XMLStreamException invalidInitializerConfig(String value, @Param Location location);

    @Message(id = 39, value = "Illegal SSL config option: %s. Should be one of [0.20,40,60]")
    XMLStreamException invalidSSLConfig(String value, @Param Location location);

//    @Message(id = 40, value = "SASCurrent narrow failed")
//    BAD_PARAM sasCurrentNarrowFailed();
//
//    @Message(id = 41, value = "Unexpected exception")
//    RuntimeException unexpectedException(@Param Throwable cause);
//
//    @Message(id = 42, value = "Unexpected ContextError in SAS reply")
//    NO_PERMISSION unexpectedContextErrorInSASReply(@Param int minorCode, @Param CompletionStatus status);
//
//    @Message(id = 43, value = "Could not parse SAS reply: %s")
//    MARSHAL errorParsingSASReply(Exception e, @Param int minorCode, @Param CompletionStatus status);
//
//    @Message(id = 44, value = "Could not register initial reference for SASCurrent")
//    RuntimeException errorRegisteringSASCurrentInitRef(@Param Throwable cause);
//
//    @Message(id = 45, value = "SAS context does not exist")
//    NO_PERMISSION missingSASContext();
//
//    @Message(id = 46, value = "Could not decode initial context token")
//    NO_PERMISSION errorDecodingInitContextToken();
//
//    @Message(id = 47, value = "Could not decode target name in initial context token")
//    NO_PERMISSION errorDecodingTargetInContextToken();
//
//    @Message(id = 48, value = "Could not decode incoming principal name")
//    NO_PERMISSION errorDecodingPrincipalName();
//
//    @Message(id = 49, value = "Exception decoding context data in %s")
//    RuntimeException errorDecodingContextData(String interceptorName, @Param Throwable e);
//
//    @Message(id = 50, value = "Batch size not numeric: %s")
//    IllegalArgumentException illegalBatchSize(String batch);
//
//    @Message(id = 51, value = "Error getting binding list")
//    NamingException errorGettingBindingList();
//
//    @Message(id = 52, value = "Error generating object via object factory")
//    NamingException errorGeneratingObjectViaFactory();
//
//    @Message(id = 53, value = "Error constructing context: either ORB or NamingContext must be supplied")
//    ConfigurationException errorConstructingCNCtx();
//
//    @Message(id = 54, value = "%s does not name a NamingContext")
//    ConfigurationException notANamingContext(String name);
//
//    @Message(id = 55, value = "Cannot convert IOR to NamingContext: %s")
//    ConfigurationException errorConvertingIORToNamingCtx(String ior);
//
//    @Message(id = 56, value = "ORB.resolve_initial_references(\"NameService\") does not return a NamingContext")
//    ConfigurationException errorResolvingNSInitRef();
//
//    @Message(id = 57, value = "COS Name Service not registered with ORB under the name 'NameService'")
//    NamingException cosNamingNotRegisteredCorrectly();
//
//    @Message(id = 58, value = "Cannot connect to ORB")
//    NamingException errorConnectingToORB();
//
//    @Message(id = 59, value = "Invalid IOR or URL: %s")
//    NamingException invalidURLOrIOR(String ior);
//
//    @Message(id = 60, value = "Invalid object reference:  %s")
//    NamingException invalidObjectReference(String ior);
//
//    @Message(id = 61, value = "%s does not contain an IOR")
//    ConfigurationException urlDoesNotContainIOR(String url);
//
//    @Message(id = 62, value = "Only instances of org.omg.CORBA.Object can be bound")
//    IllegalArgumentException notACorbaObject();
//
//    @Message(id = 63, value = "No object reference bound for specified name")
//    NamingException noReferenceFound();
//
//    @Message(id = 64, value = "Invalid empty name")
//    InvalidNameException invalidEmptyName();
//
//    @Message(id = 65, value = "%s: unescaped \\ at end of component")
//    InvalidNameException unescapedCharacter(String cnString);
//
//    @Message(id = 66, value = "%s: Invalid character being escaped")
//    InvalidNameException invalidEscapedCharacter(String cnString);
//
//    @Message(id = 67, value = "Invalid %s URL: %s")
//    MalformedURLException invalidURL(String protocol, String url);
//
//    @Message(id = 68, value = "Problem with PortableRemoteObject.toStub(); object not exported or stub not found")
//    ConfigurationException problemInvokingPortableRemoteObjectToStub();
//
//    @Message(id = 69, value = "Cannot invoke javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
//    ConfigurationException cannotInvokePortableRemoteObjectToStub();
//
//    @Message(id = 70, value = "No method definition for javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
//    IllegalStateException noMethodDefForPortableRemoteObjectToStub();
//
//    @Message(id = 71, value = "Problem invoking javax.rmi.CORBA.Stub.connect()")
//    ConfigurationException problemInvokingStubConnect();
//
//    @Message(id = 72, value = "Cannot invoke javax.rmi.CORBA.Stub.connect()")
//    ConfigurationException cannotInvokeStubConnect();
//
//    @Message(id = 73, value = "No method definition for javax.rmi.CORBA.Stub.connect(org.omg.CORBA.ORB)")
//    IllegalStateException noMethodDefForStubConnect();
//
//    @Message(id = 74, value = "Invalid IIOP URL version: %s")
//    MalformedURLException invalidIIOPURLVersion(String version);
//
//    @Message(id = 75, value = "javax.rmi packages not available")
//    ConfigurationException unavailableRMIPackages();
//
//    @Message(id = 76, value = "ISO-Latin-1 decoder unavailable")
//    MalformedURLException unavailableISOLatin1Decoder();
//
//    @Message(id = 77, value = "Invalid URI encoding: %s")
//    MalformedURLException invalidURIEncoding(String encoding);
//
//    @Message(id = 80, value = "keyManager[] is null for security domain %s")
//    IOException errorObtainingKeyManagers(String securityDomain);
//
//    @Message(id = 81, value = "Failed to get SSL context")
//    IOException failedToGetSSLContext(@Param Throwable cause);
//
//    @Message(id = 82, value = "Failed to start the JBoss Corba Naming Service")
//    StartException failedToStartJBossCOSNaming(@Param Throwable cause);
//
//    @Message(id = 83, value = "Foreign Transaction")
//    UnsupportedOperationException foreignTransaction();
//
//    @Message(id = 84, value = "Exception raised during encoding")
//    RuntimeException errorEncodingContext(@Param Throwable cause);
//
//    @Message(id = 85, value = "Exception getting slot in TxServerInterceptor")
//    RuntimeException errorGettingSlotInTxInterceptor(@Param Throwable cause);
//
//    @Message(id = 86, value = "Exception setting slot in TxServerInterceptor")
//    RuntimeException errorSettingSlotInTxInterceptor(@Param Throwable cause);
//
//    @Message(id = 87, value = "Cannot analyze a null class")
//    IllegalArgumentException cannotAnalyzeNullClass();
//
//    @Message(id = 88, value = "Bad type for a constant: %s")
//    IllegalArgumentException badConstantType(String type);
//
//    @Message(id = 89, value = "Cannot analyze special class: %s")
//    IllegalArgumentException cannotAnalyzeSpecialClass(String type);
//
//    @Message(id = 90, value = "Not an accessor: %s")
//    IllegalArgumentException notAnAccessor(String name);
//
//    @Message(id = 91, value = "Not a class or interface: %s")
//    IllegalArgumentException notAnClassOrInterface(String name);
//
//    @Message(id = 92, value = "Class %s is not an interface")
//    IllegalArgumentException notAnInterface(String name);
//
//    @Message(id = 93, value = "Not a primitive type: %s")
//    IllegalArgumentException notAPrimitive(String type);
//
//    @Message(id = 97, value = "Name cannot be null, empty or qualified")
//    IllegalArgumentException nameCannotBeNullEmptyOrQualified();
//
//    @Message(id = 98, value = "Primitive types have no IR IDs")
//    IllegalArgumentException primitivesHaveNoIRIds();
//
//    @Message(id = 99, value = "No SHA message digest available")
//    RuntimeException unavailableSHADigest(@Param Throwable cause);
//
//    @Message(id = 100, value = "Unknown primitive type: %s")
//    RuntimeException unknownPrimitiveType(String type);
//
//    @Message(id = 101, value = "Cannot analyze java.lang.String: it is a special case")
//    IllegalArgumentException cannotAnalyzeStringType();
//
//    @Message(id = 102, value = "Cannot analyze java.lang.Class: it is a special case")
//    IllegalArgumentException cannotAnalyzeClassType();
//
//    @Message(id = 105, value = "Error loading class %s")
//    RuntimeException errorLoadingClass(String type, @Param Throwable cause);
//
//    @Message(id = 106, value = "No read method in helper class %s")
//    RuntimeException noReadMethodInHelper(String type, @Param Throwable cause);
//
//    @Message(id = 107, value = "No write method in helper class %s")
//    RuntimeException noWriteMethodInHelper(String type, @Param Throwable cause);
//
//    @Message(id = 108, value = "Error unmarshaling %s")
//    RuntimeException errorUnmarshaling(Class<?> type, @Param Throwable cause);
//
//    @Message(id = 109, value = "Error marshaling %s")
//    RuntimeException errorMarshaling(Class<?> type, @Param Throwable cause);
//
//    @Message(id = 110, value = "Cannot obtain exception repository id for %s")
//    RuntimeException cannotObtainExceptionRepositoryID(String type, @Param Throwable cause);
//
//    @Message(id = 111, value = "Cannot marshal parameter: unexpected number of parameters")
//    RuntimeException errorMashalingParams();
//
//    @Message(id = 112, value = "Cannot change RMI/IIOP mapping")
//    BAD_INV_ORDER cannotChangeRMIIIOPMapping();
//
//    @Message(id = 113, value = "Bad kind %d for TypeCode")
//    RuntimeException badKindForTypeCode(int kind);
//
//    @Message(id = 114, value = "UTF-8 encoding not supported")
//    RuntimeException unsupportedUTF8Encoding();
//
//    @Message(id = 117, value = "Invalid null class")
//    IllegalArgumentException invalidNullClass();
//
//    @Message(id = 123, value = "Cannot destroy RMI/IIOP mapping")
//    BAD_INV_ORDER cannotDestroyRMIIIOPMapping();
//
//    @Message(id = 127, value = "Failed to resolve initial reference %s")
//    StartException errorResolvingInitRef(String refName, @Param Throwable cause);
//
//    @Message(id = 128, value = "Failed to create POA from parent")
//    StartException errorCreatingPOAFromParent(@Param Throwable cause);
//
//    @Message(id = 129, value = "Unable to instantiate POA: either the running ORB or the parent POA must be specified")
//    StartException invalidPOACreationArgs();
//
//    @Message(id = 130, value = "Failed to activate POA")
//    StartException errorActivatingPOA(@Param Throwable cause);
//
//    @Message(id = 131, value = "Cannot use the value 'client' for 'security'. Instead set 'security' to be 'off' and set both the " +
//                "'org.omg.PortableInterceptor.ORBInitializerClass.org.jboss.as.jacorb.csiv2.CSIv2Initializer' " +
//                "and 'org.omg.PortableInterceptor.ORBInitializerClass.org.jboss.as.jacorb.csiv2.SASClientInitializer' properties to be \"\"")
//    String cannotUseSecurityClient();

    @Message(id = 132, value = "Properties %s cannot be emulated using OpenJDK ORB and are not supported")
    OperationFailedException cannotEmulateProperties(List<String> property);

    @LogMessage(level = WARN)
    @Message(id = 133, value = "JacORB is not used as an ORB implementation anymore. JacORB subsystem would be "
            + "emulated using the current OpenJDK ORB implementation. Ability to emulate legacy JacORB configurations "
            + "using OpenJDK ORB will be removed in future.")
    void jacorbEmulationWarning();


    @Message(id = 134, value = "Properties %s cannot be emulated using OpenJDK ORB and are not supported")
    String cannotEmulatePropertiesWarning(List<String> property);

    @Message(id = 135, value = "Migration failed, see results for more details.")
    String migrationFailed();

    @Message(id = 136, value = "The properties %s use expressions. Configuration properties that are used to resolve those expressions " +
            "should be transformed manually to the new iiop-openjdk subsystem format")
    String expressionMigrationWarning(String properties);

}
