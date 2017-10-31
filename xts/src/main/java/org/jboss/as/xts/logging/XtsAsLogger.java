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

package org.jboss.as.xts.logging;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
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
    XtsAsLogger ROOT_LOGGER = Logger.getMessageLogger(XtsAsLogger.class, "org.jboss.as.xts");

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
//     * Creates an exception indicating that the CDI extension could not be loaded.
//     *
//     * @return a {@link org.jboss.as.server.deployment.DeploymentUnitProcessingException} for the error.
//     */
    // @Message(id = 7, value = "Cannot load CDI Extension")
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
}
