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

package org.jboss.as.xts;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface XtsAsMessages {

    /**
     * The messages
     */
    XtsAsMessages MESSAGES = Messages.getBundle(XtsAsMessages.class);

    /**
     * Creates an exception indicating that the TxBridge inbound recovery service failed to start.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 18400, value = "TxBridge inbound recovery service start failed")
    StartException txBridgeInboundRecoveryServiceFailedToStart();

    /**
     * Creates an exception indicating that the TxBridge outbound recovery service failed to start.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 18401, value = "TxBridge outbound recovery service start failed")
    StartException txBridgeOutboundRecoveryServiceFailedToStart();

    /**
     * Creates an exception indicating that the XTS service failed to start.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 18402, value = "XTS service start failed")
    StartException xtsServiceFailedToStart();

    /**
     * Creates an exception indicating that this operation can not be performed when the XTS service is not started.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 18403, value = "Service not started")
    IllegalStateException xtsServiceIsNotStarted();

}
