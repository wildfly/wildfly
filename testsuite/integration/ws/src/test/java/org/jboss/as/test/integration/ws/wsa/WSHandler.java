/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsa;

import java.io.ByteArrayOutputStream;
import java.util.Set;
import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public class WSHandler implements SOAPHandler<SOAPMessageContext> {
    private static final Logger LOGGER = Logger.getLogger(WSHandler.class);

    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleFault(SOAPMessageContext context) {
        log(context);
        return true;
    }

    public boolean handleMessage(SOAPMessageContext context) {
        log(context);
        return true;
    }

    private void log(SOAPMessageContext smc) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty) {
            LOGGER.trace("Outgoing message:");
        } else {
            LOGGER.trace("Incoming message:");
        }

        LOGGER.trace("-----------");
        SOAPMessage message = smc.getMessage();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            message.writeTo(baos);
            LOGGER.trace(baos.toString());
            LOGGER.trace("");
        } catch (Exception e) {
            LOGGER.error("Exception in handler: " + e);
        }
        LOGGER.trace("-----------");
    }

    public void close(MessageContext context) {
    }
}
