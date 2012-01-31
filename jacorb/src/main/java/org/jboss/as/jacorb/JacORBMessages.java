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

package org.jboss.as.jacorb;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.naming.ConfigurationException;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jacorb.rmi.RMIIIOPViolationException;
import org.jboss.as.jacorb.rmi.ir.IRConstructionException;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.msc.service.StartException;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_PERMISSION;

/**
 * The jacorb subsystem is using message IDs in the range 16300-16499. This file is using the subset 16400-16499 for
 * exception messages.
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface JacORBMessages {

    JacORBMessages MESSAGES = Messages.getBundle(JacORBMessages.class);

    @Message(id = 16400, value = "SSL support has been enabled but no security domain has been specified")
    OperationFailedException noSecurityDomainSpecified();

    @Message(id = 16401, value = "Illegal initializer value: %s. Should be one of [security,transactions]")
    XMLStreamException invalidInitializerConfig(String value, @Param Location location);

    @Message(id = 16402, value = "Illegal SSL config option: %s. Should be one of [0.20,40,60]")
    XMLStreamException invalidSSLConfig(String value, @Param Location location);

    @Message(id = 16403, value = "SASCurrent narrow failed")
    BAD_PARAM sasCurrentNarrowFailed();

    @Message(id = 16404, value = "Unexpected exception")
    RuntimeException unexpectedException(@Param Throwable cause);

    @Message(id = 16405, value = "Unexpected ContextError in SAS reply")
    NO_PERMISSION unexpectedContextErrorInSASReply(@Param int minorCode, @Param CompletionStatus status);

    @Message(id = 16406, value = "Could not parse SAS reply: %s")
    MARSHAL errorParsingSASReply(Exception e, @Param int minorCode, @Param CompletionStatus status);

    @Message(id = 16407, value = "Could not register initial reference for SASCurrent")
    RuntimeException errorRegisteringSASCurrentInitRef(@Param Throwable cause);

    @Message(id = 16408, value = "SAS context does not exist")
    NO_PERMISSION missingSASContext();

    @Message(id = 16409, value = "Could not decode initial context token")
    NO_PERMISSION errorDecodingInitContextToken();

    @Message(id = 16410, value = "Could not decode target name in initial context token")
    NO_PERMISSION errorDecodingTargetInContextToken();

    @Message(id = 16411, value = "Could not decode incoming principal name")
    NO_PERMISSION errorDecodingPrincipalName();

    @Message(id = 16412, value = "Exception decoding context data in %s")
    RuntimeException errorDecodingContextData(String interceptorName, @Param Throwable e);

    @Message(id = 16413, value = "Batch size not numeric: %s")
    IllegalArgumentException illegalBatchSize(String batch);

    @Message(id = 16414, value = "Error getting binding list")
    NamingException errorGettingBindingList();

    @Message(id = 16415, value = "Error generating object via object factory")
    NamingException errorGeneratingObjectViaFactory();

    @Message(id = 16416, value = "Error constructing context: either ORB or NamingContext must be supplied")
    ConfigurationException errorConstructingCNCtx();

    @Message(id = 16417, value = "%s does not name a NamingContext")
    ConfigurationException notANamingContext(String name);

    @Message(id = 16418, value = "Cannot convert IOR to NamingContext: %s")
    ConfigurationException errorConvertingIORToNamingCtx(String ior);

    @Message(id = 16419, value = "ORB.resolve_initial_references(\"NameService\") does not return a NamingContext")
    ConfigurationException errorResolvingNSInitRef();

    @Message(id = 16420, value = "COS Name Service not registered with ORB under the name 'NameService'")
    NamingException cosNamingNotRegisteredCorrectly();

    @Message(id = 16421, value = "Cannot connect to ORB")
    NamingException errorConnectingToORB();

    @Message(id = 16422, value = "Invalid IOR or URL: %s")
    NamingException invalidURLOrIOR(String ior);

    @Message(id = 16423, value = "Invalid object reference:  %s")
    NamingException invalidObjectReference(String ior);

    @Message(id = 16424, value = "%s does not contain an IOR")
    ConfigurationException urlDoesNotContainIOR(String url);

    @Message(id = 16425, value = "Only instances of org.omg.CORBA.Object can be bound")
    IllegalArgumentException notACorbaObject();

    @Message(id = 16426, value = "No object reference bound for specified name")
    NamingException noReferenceFound();

    @Message(id = 16427, value = "Invalid empty name")
    InvalidNameException invalidEmptyName();

    @Message(id = 16428, value = "%s: unescaped \\ at end of component")
    InvalidNameException unescapedCharacter(String cnString);

    @Message(id = 16429, value = "%s: Invalid character being escaped")
    InvalidNameException invalidEscapedCharacter(String cnString);

    @Message(id = 16430, value = "Invalid %s URL: %s")
    MalformedURLException invalidURL(String protocol, String url);

    @Message(id = 16431, value = "Problem with PortableRemoteObject.toStub(); object not exported or stub not found")
    ConfigurationException problemInvokingPortableRemoteObjectToStub();

    @Message(id = 16432, value = "Cannot invoke javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
    ConfigurationException cannotInvokePortableRemoteObjectToStub();

    @Message(id = 16433, value = "No method definition for javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
    IllegalStateException noMethodDefForPortableRemoteObjectToStub();

    @Message(id = 16434, value = "Problem invoking javax.rmi.CORBA.Stub.connect()")
    ConfigurationException problemInvokingStubConnect();

    @Message(id = 16435, value = "Cannot invoke javax.rmi.CORBA.Stub.connect()")
    ConfigurationException cannotInvokeStubConnect();

    @Message(id = 16436, value = "No method definition for javax.rmi.CORBA.Stub.connect(org.omg.CORBA.ORB)")
    IllegalStateException noMethodDefForStubConnect();

    @Message(id = 16437, value = "Invalid IIOP URL version: %s")
    MalformedURLException invalidIIOPURLVersion(String version);

    @Message(id = 16438, value = "javax.rmi packages not available")
    ConfigurationException unavailableRMIPackages();

    @Message(id = 16439, value = "ISO-Latin-1 decoder unavailable")
    MalformedURLException unavailableISOLatin1Decoder();

    @Message(id = 16440, value = "Invalid URI encoding: %s")
    MalformedURLException invalidURIEncoding(String encoding);

    @Message(id = 16441, value = "Error configuring domain socket factory: security domain is null")
    org.jacorb.config.ConfigurationException errorConfiguringDomainSF();

    @Message(id = 16442, value = "Error configuring domain socket factory: failed to lookup JSSE security domain")
    org.jacorb.config.ConfigurationException failedToLookupJSSEDomain();

    @Message(id = 16443, value = "keyManager[] is null for security domain %s")
    IOException errorObtainingKeyManagers(String securityDomain);

    @Message(id = 16444, value = "Failed to get SSL context")
    IOException failedToGetSSLContext(@Param Throwable cause);

    @Message(id = 16445, value = "Failed to start the JBoss Corba Naming Service")
    StartException failedToStartJBossCOSNaming(@Param Throwable cause);

    @Message(id = 16446, value = "Foreign Transaction")
    UnsupportedOperationException foreignTransaction();

    @Message(id = 16447, value = "Exception raised during encoding")
    RuntimeException errorEncodingContext(@Param Throwable cause);

    @Message(id = 16448, value = "Exception getting slot in TxServerInterceptor")
    RuntimeException errorGettingSlotInTxInterceptor(@Param Throwable cause);

    @Message(id = 16449, value = "Exception setting slot in TxServerInterceptor")
    RuntimeException errorSettingSlotInTxInterceptor(@Param Throwable cause);

    @Message(id = 16450, value = "Cannot analyze a null class")
    IllegalArgumentException cannotAnalyzeNullClass();

    @Message(id = 16451, value = "Bad type for a constant: %s")
    IllegalArgumentException badConstantType(String type);

    @Message(id = 16452, value = "Cannot analyze special class: %s")
    IllegalArgumentException cannotAnalyzeSpecialClass(String type);

    @Message(id = 16453, value = "Not an accessor: %s")
    IllegalArgumentException notAnAccessor(String name);

    @Message(id = 16454, value = "Not a class or interface: %s")
    IllegalArgumentException notAnClassOrInterface(String name);

    @Message(id = 16455, value = "Class %s is not an interface")
    IllegalArgumentException notAnInterface(String name);

    @Message(id = 16456, value = "Not a primitive type: %s")
    IllegalArgumentException notAPrimitive(String type);

    @Message(id = 16457, value = "Field %s of interface %s is a constant, but it is not primitive or String")
    RMIIIOPViolationException badRMIIIOPConstantType(String field, String intface, @Param String section);

    @Message(id = 16458, value = "Exception type %s must be a checked exception class")
    RMIIIOPViolationException badRMIIIOPExceptionType(String type, @Param String section);

    @Message(id = 16459, value = "All interface methods must throw javax.rmi.RemoteException but method %s of interface %s does not")
    RMIIIOPViolationException badRMIIIOPMethodSignature(String method, String intface, @Param String section);

    @Message(id = 16460, value = "Name cannot be null, empty or qualified")
    IllegalArgumentException nameCannotBeNullEmptyOrQualified();

    @Message(id = 16461, value = "Primitive types have no IR IDs")
    IllegalArgumentException primitivesHaveNoIRIds();

    @Message(id = 16462, value = "No SHA message digest available")
    RuntimeException unavailableSHADigest(@Param Throwable cause);

    @Message(id = 16463, value = "Unknown primitive type: %s")
    RuntimeException unknownPrimitiveType(String type);

    @Message(id = 16464, value = "Cannot analyze java.lang.String: it is a special case")
    IllegalArgumentException cannotAnalyzeStringType();

    @Message(id = 16465, value = "Cannot analyze java.lang.Class: it is a special case")
    IllegalArgumentException cannotAnalyzeClassType();

    @Message(id = 16466, value = "Value type %s cannot implement java.rmi.Remote")
    RMIIIOPViolationException valueTypeCantImplementRemote(String type, @Param String section);

    @Message(id = 16467, value = "Value type %s cannot be a proxy or inner class")
    RMIIIOPViolationException valueTypeCantBeProxy(String type);

    @Message(id = 16468, value = "Error loading class %s")
    RuntimeException errorLoadingClass(String type, @Param Throwable cause);

    @Message(id = 16469, value = "No read method in helper class %s")
    RuntimeException noReadMethodInHelper(String type, @Param Throwable cause);

    @Message(id = 16470, value = "No write method in helper class %s")
    RuntimeException noWriteMethodInHelper(String type, @Param Throwable cause);

    @Message(id = 16471, value = "Error unmarshaling %s")
    RuntimeException errorUnmarshaling(Class<?> type, @Param Throwable cause);

    @Message(id = 16472, value = "Error marshaling %s")
    RuntimeException errorMarshaling(Class<?> type, @Param Throwable cause);

    @Message(id = 16473, value = "Cannot obtain exception repository id for %s")
    RuntimeException cannotObtainExceptionRepositoryID(String type, @Param Throwable cause);

    @Message(id = 16474, value = "Cannot marshal parameter: unexpected number of parameters")
    RuntimeException errorMashalingParams();

    @Message(id = 16475, value = "Cannot change RMI/IIOP mapping")
    BAD_INV_ORDER cannotChangeRMIIIOPMapping();

    @Message(id = 16476, value = "Bad kind %d for TypeCode")
    RuntimeException badKindForTypeCode(int kind);

    @Message(id = 16477, value = "UTF-8 encoding not supported")
    RuntimeException unsupportedUTF8Encoding();

    @Message(id = 16478, value = "Wrong interface repository")
    IRConstructionException wrongInterfaceRepository();

    @Message(id = 16479, value = "Duplicate repository name")
    IRConstructionException duplicateRepositoryName();

    @Message(id = 16480, value = "Invalid null class")
    IllegalArgumentException invalidNullClass();

    @Message(id = 16481, value = "Bad class %s for a constant")
    IRConstructionException badClassForConstant(String className);

    @Message(id = 16482, value = "TypeCode for class %s is unknown")
    IRConstructionException unknownTypeCodeForClass(String className);

    @Message(id = 16483, value = "TypeCode for class %s already established")
    IRConstructionException duplicateTypeCodeForClass(String className);

    @Message(id = 16484, value = "Name collision while creating package")
    IRConstructionException collisionWhileCreatingPackage();

    @Message(id = 16485, value = "Class %s is not an array class")
    IRConstructionException classIsNotArray(String className);

    @Message(id = 16486, value = "Cannot destroy RMI/IIOP mapping")
    BAD_INV_ORDER cannotDestroyRMIIIOPMapping();

    @Message(id = 16487, value = "Bad kind for super valuetype of %s")
    IRConstructionException badKindForSuperValueType(String id);

    @Message(id = 16488, value = "ValueDef %s unable to resolve reference to implemented interface %s")
    IRConstructionException errorResolvingRefToImplementedInterface(String id, String intface);

    @Message(id = 16489, value = "ValueDef %s unable to resolve reference to abstract base valuetype %s")
    IRConstructionException errorResolvingRefToAbstractValuetype(String id, String valuetype);

}
