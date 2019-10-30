/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsa;

import java.io.ByteArrayOutputStream;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

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
