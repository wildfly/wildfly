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

package org.jboss.as.iiop;

import java.net.MalformedURLException;

import javax.naming.ConfigurationException;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.jboss.as.iiop.rmi.RMIIIOPViolationException;
import org.jboss.as.iiop.rmi.ir.IRConstructionException;
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
 *
 * 12900 - 13100
 */
@MessageBundle(projectCode = "JBAS")
public interface IIOPMessages {

    IIOPMessages MESSAGES = Messages.getBundle(IIOPMessages.class);

    @Message(id = 12900, value = "Unexpected exception")
    RuntimeException unexpectedException(@Param Throwable cause);

    @Message(id = 12901, value = "Cannot analyze a null class")
    IllegalArgumentException cannotAnalyzeNullClass();

    @Message(id = 12902, value = "Bad type for a constant: %s")
    IllegalArgumentException badConstantType(String type);

    @Message(id = 12903, value = "Cannot analyze special class: %s")
    IllegalArgumentException cannotAnalyzeSpecialClass(String type);

    @Message(id = 12904, value = "Not an accessor: %s")
    IllegalArgumentException notAnAccessor(String name);

    @Message(id = 12905, value = "Not a class or interface: %s")
    IllegalArgumentException notAnClassOrInterface(String name);

    @Message(id = 12906, value = "Class %s is not an interface")
    IllegalArgumentException notAnInterface(String name);

    @Message(id = 12907, value = "Not a primitive type: %s")
    IllegalArgumentException notAPrimitive(String type);

    @Message(id = 12908, value = "Field %s of interface %s is a constant, but it is not primitive or String")
    RMIIIOPViolationException badRMIIIOPConstantType(String field, String intface, @Param String section);

    @Message(id = 12909, value = "Exception type %s must be a checked exception class")
    RMIIIOPViolationException badRMIIIOPExceptionType(String type, @Param String section);

    @Message(id = 12910, value = "All interface methods must throw javax.rmi.RemoteException but method %s of interface %s does not")
    RMIIIOPViolationException badRMIIIOPMethodSignature(String method, String intface, @Param String section);

    @Message(id = 12911, value = "Name cannot be null, empty or qualified")
    IllegalArgumentException nameCannotBeNullEmptyOrQualified();

    @Message(id = 12912, value = "Primitive types have no IR IDs")
    IllegalArgumentException primitivesHaveNoIRIds();

    @Message(id = 12913, value = "No SHA message digest available")
    RuntimeException unavailableSHADigest(@Param Throwable cause);

    @Message(id = 12914, value = "Unknown primitive type: %s")
    RuntimeException unknownPrimitiveType(String type);

    @Message(id = 12915, value = "Cannot analyze java.lang.String: it is a special case")
    IllegalArgumentException cannotAnalyzeStringType();

    @Message(id = 12916, value = "Cannot analyze java.lang.Class: it is a special case")
    IllegalArgumentException cannotAnalyzeClassType();

    @Message(id = 12917, value = "Value type %s cannot implement java.rmi.Remote")
    RMIIIOPViolationException valueTypeCantImplementRemote(String type, @Param String section);

    @Message(id = 12918, value = "Value type %s cannot be a proxy or inner class")
    RMIIIOPViolationException valueTypeCantBeProxy(String type);

    @Message(id = 12919, value = "Error loading class %s")
    RuntimeException errorLoadingClass(String type, @Param Throwable cause);

    @Message(id = 12920, value = "No read method in helper class %s")
    RuntimeException noReadMethodInHelper(String type, @Param Throwable cause);

    @Message(id = 12921, value = "No write method in helper class %s")
    RuntimeException noWriteMethodInHelper(String type, @Param Throwable cause);

    @Message(id = 12922, value = "Error unmarshaling %s")
    RuntimeException errorUnmarshaling(Class<?> type, @Param Throwable cause);

    @Message(id = 12923, value = "Error marshaling %s")
    RuntimeException errorMarshaling(Class<?> type, @Param Throwable cause);

    @Message(id = 12924, value = "Cannot obtain exception repository id for %s")
    RuntimeException cannotObtainExceptionRepositoryID(String type, @Param Throwable cause);

    @Message(id = 12925, value = "Cannot marshal parameter: unexpected number of parameters")
    RuntimeException errorMashalingParams();

    @Message(id = 12926, value = "Cannot change RMI/IIOP mapping")
    BAD_INV_ORDER cannotChangeRMIIIOPMapping();

    @Message(id = 12927, value = "Bad kind %d for TypeCode")
    RuntimeException badKindForTypeCode(int kind);

    @Message(id = 12928, value = "UTF-8 encoding not supported")
    RuntimeException unsupportedUTF8Encoding();

    @Message(id = 12929, value = "Wrong interface repository")
    IRConstructionException wrongInterfaceRepository();

    @Message(id = 12930, value = "Duplicate repository name")
    IRConstructionException duplicateRepositoryName();

    @Message(id = 12931, value = "Invalid null class")
    IllegalArgumentException invalidNullClass();

    @Message(id = 12932, value = "Bad class %s for a constant")
    IRConstructionException badClassForConstant(String className);

    @Message(id = 12933, value = "TypeCode for class %s is unknown")
    IRConstructionException unknownTypeCodeForClass(String className);

    @Message(id = 12934, value = "TypeCode for class %s already established")
    IRConstructionException duplicateTypeCodeForClass(String className);

    @Message(id = 12935, value = "Name collision while creating package")
    IRConstructionException collisionWhileCreatingPackage();

    @Message(id = 12936, value = "Class %s is not an array class")
    IRConstructionException classIsNotArray(String className);

    @Message(id = 12937, value = "Cannot destroy RMI/IIOP mapping")
    BAD_INV_ORDER cannotDestroyRMIIIOPMapping();

    @Message(id = 12938, value = "Bad kind for super valuetype of %s")
    IRConstructionException badKindForSuperValueType(String id);

    @Message(id = 12939, value = "ValueDef %s unable to resolve reference to implemented interface %s")
    IRConstructionException errorResolvingRefToImplementedInterface(String id, String intface);

    @Message(id = 12940, value = "ValueDef %s unable to resolve reference to abstract base valuetype %s")
    IRConstructionException errorResolvingRefToAbstractValuetype(String id, String valuetype);

    @Message(id = 12941, value = "Foreign Transaction")
    UnsupportedOperationException foreignTransaction();

    @Message(id = 12942, value = "Exception decoding context data in %s")
    RuntimeException errorDecodingContextData(String interceptorName, @Param Throwable e);

    @Message(id = 12943, value = "Exception setting slot in TxServerInterceptor")
    RuntimeException errorSettingSlotInTxInterceptor(@Param Throwable cause);

    @Message(id = 12944, value = "Exception getting slot in TxServerInterceptor")
    RuntimeException errorGettingSlotInTxInterceptor(@Param Throwable cause);

    @Message(id = 12945, value = "Exception raised during encoding")
    RuntimeException errorEncodingContext(@Param Throwable cause);

    @Message(id = 12946, value = "SASCurrent narrow failed")
    BAD_PARAM sasCurrentNarrowFailed();

    @Message(id = 12947, value = "Unexpected ContextError in SAS reply")
    NO_PERMISSION unexpectedContextErrorInSASReply(@Param int minorCode, @Param CompletionStatus status);

    @Message(id = 12948, value = "Could not parse SAS reply: %s")
    MARSHAL errorParsingSASReply(Exception e, @Param int minorCode, @Param CompletionStatus status);

    @Message(id = 12949, value = "Could not register initial reference for SASCurrent")
    RuntimeException errorRegisteringSASCurrentInitRef(@Param Throwable cause);

    @Message(id = 12950, value = "SAS context does not exist")
    NO_PERMISSION missingSASContext();

    @Message(id = 12951, value = "Could not decode initial context token")
    NO_PERMISSION errorDecodingInitContextToken();

    @Message(id = 12952, value = "Could not decode target name in initial context token")
    NO_PERMISSION errorDecodingTargetInContextToken();

    @Message(id = 12953, value = "Could not decode incoming principal name")
    NO_PERMISSION errorDecodingPrincipalName();

    @Message(id = 12954, value = "Failed to resolve initial reference %s")
    StartException errorResolvingInitRef(String refName, @Param Throwable cause);

    @Message(id = 12955, value = "Failed to create POA from parent")
    StartException errorCreatingPOAFromParent(@Param Throwable cause);

    @Message(id = 12956, value = "Unable to instantiate POA: either the running ORB or the parent POA must be specified")
    StartException invalidPOACreationArgs();

    @Message(id = 12957, value = "Failed to activate POA")
    StartException errorActivatingPOA(@Param Throwable cause);

    @Message(id = 12958, value = "Failed to start the JBoss Corba Naming Service")
    StartException failedToStartJBossCOSNaming(@Param Throwable cause);

    @Message(id = 12959, value = "Batch size not numeric: %s")
    IllegalArgumentException illegalBatchSize(String batch);

    @Message(id = 12960, value = "Error getting binding list")
    NamingException errorGettingBindingList();

    @Message(id = 12961, value = "Error generating object via object factory")
    NamingException errorGeneratingObjectViaFactory();

    @Message(id = 12962, value = "Error constructing context: either ORB or NamingContext must be supplied")
    ConfigurationException errorConstructingCNCtx();

    @Message(id = 12963, value = "%s does not name a NamingContext")
    ConfigurationException notANamingContext(String name);

    @Message(id = 12964, value = "Cannot convert IOR to NamingContext: %s")
    ConfigurationException errorConvertingIORToNamingCtx(String ior);

    @Message(id = 12965, value = "ORB.resolve_initial_references(\"NameService\") does not return a NamingContext")
    ConfigurationException errorResolvingNSInitRef();

    @Message(id = 12966, value = "COS Name Service not registered with ORB under the name 'NameService'")
    NamingException cosNamingNotRegisteredCorrectly();

    @Message(id = 12967, value = "Cannot connect to ORB")
    NamingException errorConnectingToORB();

    @Message(id = 12968, value = "Invalid IOR or URL: %s")
    NamingException invalidURLOrIOR(String ior);

    @Message(id = 12969, value = "Invalid object reference:  %s")
    NamingException invalidObjectReference(String ior);

    @Message(id = 12970, value = "%s does not contain an IOR")
    ConfigurationException urlDoesNotContainIOR(String url);

    @Message(id = 12971, value = "Only instances of org.omg.CORBA.Object can be bound")
    IllegalArgumentException notACorbaObject();

    @Message(id = 12972, value = "No object reference bound for specified name")
    NamingException noReferenceFound();

    @Message(id = 12973, value = "Invalid empty name")
    InvalidNameException invalidEmptyName();

    @Message(id = 12974, value = "Invalid %s URL: %s")
    MalformedURLException invalidURL(String protocol, String url);

    @Message(id = 12975, value = "Problem with PortableRemoteObject.toStub(); object not exported or stub not found")
    ConfigurationException problemInvokingPortableRemoteObjectToStub();

    @Message(id = 12976, value = "Cannot invoke javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
    ConfigurationException cannotInvokePortableRemoteObjectToStub();

    @Message(id = 12977, value = "Invalid IIOP URL version: %s")
    MalformedURLException invalidIIOPURLVersion(String version);

    @Message(id = 12978, value = "javax.rmi packages not available")
    ConfigurationException unavailableRMIPackages();

    @Message(id = 12979, value = "ISO-Latin-1 decoder unavailable")
    MalformedURLException unavailableISOLatin1Decoder();

    @Message(id = 12980, value = "Invalid URI encoding: %s")
    MalformedURLException invalidURIEncoding(String encoding);

    @Message(id = 12981, value = "%s: unescaped \\ at end of component")
    InvalidNameException unescapedCharacter(String cnString);

    @Message(id = 12982, value = "%s: Invalid character being escaped")
    InvalidNameException invalidEscapedCharacter(String cnString);

    @Message(id = 12983, value = "No method definition for javax.rmi.PortableRemoteObject.toStub(java.rmi.Remote)")
    IllegalStateException noMethodDefForPortableRemoteObjectToStub();

    @Message(id = 12984, value = "Problem invoking javax.rmi.CORBA.Stub.connect()")
    ConfigurationException problemInvokingStubConnect();

    @Message(id = 12985, value = "Cannot invoke javax.rmi.CORBA.Stub.connect()")
    ConfigurationException cannotInvokeStubConnect();

    @Message(id = 12986, value = "No method definition for javax.rmi.CORBA.Stub.connect(org.omg.CORBA.ORB)")
    IllegalStateException noMethodDefForStubConnect();

}
