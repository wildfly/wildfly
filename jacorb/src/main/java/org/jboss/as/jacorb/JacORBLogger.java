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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.omg.CORBA.ORB;

/**
 * The jacorb subsystem is using message IDs in the range 16300-16499. This file is using the subset 16300-16399 for
 * logger messages.
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface JacORBLogger extends BasicLogger {

    JacORBLogger ROOT_LOGGER = Logger.getMessageLogger(JacORBLogger.class, JacORBLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 16300, value = "Activating JacORB Subsystem")
    void activatingSubsystem();

    @LogMessage(level = DEBUG)
    @Message(id = 16301, value = "IOR security config metadata: %s")
    void debugIORSecurityConfigMetaData(IORSecurityConfigMetaData metaData);

    @LogMessage(level = DEBUG)
    @Message(id = 16302, value = "CSIv2Policy not found in IORInfo")
    void csiv2PolicyNotFoundInIORInfo();

    @LogMessage(level = ERROR)
    @Message(id = 16303, value = "Error fetching CSIv2Policy")
    void failedToFetchCSIv2Policy(@Cause Throwable cause);

    @LogMessage(level = DEBUG)
    @Message(id = 16304, value = "Method createSSLTaggedComponent() called with null metadata")
    void createSSLTaggedComponentWithNullMetaData();

    @LogMessage(level = DEBUG)
    @Message(id = 16305, value = "Method createSecurityTaggedComponent() called with null metadata")
    void createSecurityTaggedComponentWithNullMetaData();

    @LogMessage(level = WARN)
    @Message(id = 16306, value = "Caught exception while encoding GSSUPMechOID")
    void caughtExceptionEncodingGSSUPMechOID(@Cause Throwable cause);

    @LogMessage(level = TRACE)
    @Message(id = 16307, value = "receive_reply: got SAS reply, type %d")
    void traceReceiveReply(int type);

    @LogMessage(level = TRACE)
    @Message(id = 16308, value = "receive_exception: got SAS reply, type %d")
    void traceReceiveException(int type);

    @LogMessage(level = TRACE)
    @Message(id = 16309, value = "receive_request: %s")
    void traceReceiveRequest(String operation);

    @LogMessage(level = TRACE)
    @Message(id = 16310, value = "Received client authentication token")
    void authTokenReceived();

    @LogMessage(level = TRACE)
    @Message(id = 16311, value = "Received identity token")
    void identityTokenReceived();

    @LogMessage(level = TRACE)
    @Message(id = 16312, value = "send_reply: %s")
    void traceSendReply(String operation);

    @LogMessage(level = TRACE)
    @Message(id = 16313, value = "send_exception: %s")
    void traceSendException(String operation);

    @LogMessage(level = DEBUG)
    @Message(id = 16314, value = "Bound name: %s")
    void debugBoundName(String name);

    @LogMessage(level = DEBUG)
    @Message(id = 16315, value = "Bound context: %s")
    void debugBoundContext(String context);

    @LogMessage(level = DEBUG)
    @Message(id = 16316, value = "Unbound: %s")
    void debugUnboundObject(String context);

    @LogMessage(level = ERROR)
    @Message(id = 16317, value = "Internal error")
    void logInternalError(@Cause Exception cause);

    @LogMessage(level = ERROR)
    @Message(id = 16318, value = "Failed to create CORBA naming context")
    void failedToCreateNamingContext(@Cause Exception cause);

    @LogMessage(level = WARN)
    @Message(id = 16319, value = "Unbind failed for %s")
    void failedToUnbindObject(String name);

    @LogMessage(level = DEBUG)
    @Message(id = 16320, value = "Unable to obtain id from object")
    void failedToObtainIdFromObject(@Cause Exception cause);

    @LogMessage(level = DEBUG)
    @Message(id = 16321, value = "Getting default ORB %s")
    void debugGetDefaultORB(ORB orb);

    @LogMessage(level = TRACE)
    @Message(id = 16322, value = "Creating server socket factory: %s")
    void traceServerSocketFactoryCreation(String factoryClass);

    @LogMessage(level = DEBUG)
    @Message(id = 16323, value = "Obtained JSSE security domain with name %s")
    void debugJSSEDomainRetrieval(String securityDomain);

    @LogMessage(level = ERROR)
    @Message(id = 16324, value = "Failed to obtain JSSE security domain with name %s")
    void failedToObtainJSSEDomain(String securityDomain);

    @LogMessage(level = TRACE)
    @Message(id = 16325, value = "Creating socket factory: %s")
    void traceSocketFactoryCreation(String factoryClass);

    @LogMessage(level = DEBUG)
    @Message(id = 16326, value = "Starting service %s")
    void debugServiceStartup(String serviceName);

    @LogMessage(level = DEBUG)
    @Message(id = 16327, value = "Stopping service %s")
    void debugServiceStop(String serviceName);

    @LogMessage(level = INFO)
    @Message(id = 16328, value = "CORBA Naming Service started")
    void corbaNamingServiceStarted();

    @LogMessage(level = DEBUG)
    @Message(id = 16329, value = "Naming: [%s]")
    void debugNamingServiceIOR(String ior);

    @LogMessage(level = INFO)
    @Message(id = 16330, value = "CORBA ORB Service started")
    void corbaORBServiceStarted();

    @LogMessage(level = TRACE)
    @Message(id = 16331, value = "Intercepting receive_request_service_contexts, operation: %s")
    void traceReceiveRequestServiceContexts(String operation);

    @LogMessage(level = WARN)
    @Message(id = 16332, value = "Compatibility problem: Class javax.rmi.CORBA.ClassDesc does not conform to the Java(TM) Language to IDL Mapping Specification (01-06-07), section 1.3.5.11")
    void warnClassDescDoesNotConformToSpec();

    @LogMessage(level = WARN)
    @Message(id = 16333, value = "Could not deactivate IR object")
    void warnCouldNotDeactivateIRObject(@Cause Throwable cause);

    @LogMessage(level = DEBUG)
    @Message(id = 16334, value = "Exception converting CORBA servant to reference")
    void debugExceptionConvertingServantToReference(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 16335, value = "Could not deactivate anonymous IR object")
    void warnCouldNotDeactivateAnonIRObject(@Cause Throwable cause);

}
