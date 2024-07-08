/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts.logging;

import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.Logger.Level.ERROR;

import java.lang.invoke.MethodHandles;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.xts.XTSException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYXTS", length = 4)
public interface XtsAsLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    XtsAsLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), XtsAsLogger.class, "org.jboss.as.xts");

    /**
     * Creates an exception indicating that the TxBridge inbound recovery service failed to start.
     *
     * @return a {@link org.jboss.msc.service.StartException} for the error.
     */
    @Message(id = 1, value = "TxBridge inbound recovery service start failed")
    StartException txBridgeInboundRecoveryServiceFailedToStart();

    /**
     * Creates an exception indicating that the TxBridge outbound recovery service failed to start.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 2, value = "TxBridge outbound recovery service start failed")
    StartException txBridgeOutboundRecoveryServiceFailedToStart();

    /**
     * Creates an exception indicating that the XTS service failed to start.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 3, value = "XTS service start failed")
    StartException xtsServiceFailedToStart();

    /**
     * Creates an exception indicating that this operation can not be performed when the XTS service is not started.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 4, value = "Service not started")
    IllegalStateException xtsServiceIsNotStarted();

//    /**
//     * Creates an exception indicating that configuration service is not available.
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
    // @Message(id = 5, value = "Configuration service is not available")
    // IllegalStateException configurationServiceUnavailable();
//
//    /**
//     * Creates an exception indicating that common configuration is not available.
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 6, value = "Common configuration is not available")
//    IllegalStateException commonConfigurationUnavailable();

//    /**
//     * Creates an exception indicating that the Jakarta Contexts and Dependency Injection extension could not be loaded.
//     *
//     * @return a {@link org.jboss.as.server.deployment.DeploymentUnitProcessingException} for the error.
//     */
    // @Message(id = 7, value = "Cannot load Jakarta Contexts and Dependency Injection Extension")
    // DeploymentUnitProcessingException cannotLoadCDIExtension();

//    /**
//     * Warning that coordination context deserialization has failed
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 8, value = "Coordination context deserialization failed")
//    void coordinationContextDeserializationFailed(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 9, value = "Rejecting call because it is not part of any XTS transaction")
    void rejectingCallBecauseNotPartOfXtsTx();

    @LogMessage(level = ERROR)
    @Message(id = 10, value = "Cannot get transaction status on handling context %s")
    void cannotGetTransactionStatus(jakarta.xml.ws.handler.MessageContext ctx, @Cause Throwable cause);

    @Message(id = 11, value = "Unexpected bridge type: '%s'")
    XTSException unexpectedBridgeType(String bridgeType);

    @Message(id = 12, value = "Error processing endpoint '%s'")
    DeploymentUnitProcessingException errorProcessingEndpoint(String endpoint, @Cause Throwable cause);
}
