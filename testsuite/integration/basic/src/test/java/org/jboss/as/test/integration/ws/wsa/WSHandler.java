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

import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public class WSHandler implements SOAPHandler<SOAPMessageContext> {

    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleFault(SOAPMessageContext context) {
        logToSystemOut(context);
        return true;
    }

    public boolean handleMessage(SOAPMessageContext context) {
        logToSystemOut(context);
        return true;
    }

    private void logToSystemOut(SOAPMessageContext smc) {
        Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty.booleanValue()) {
            System.out.println("Outgoing message:");
        } else {
            System.out.println("Incoming message:");
        }

        System.out.println("-----------");
        SOAPMessage message = smc.getMessage();
        try {
            message.writeTo(System.out);
            System.out.println("");
        } catch (Exception e) {
            System.out.println("Exception in handler: " + e);
        }
        System.out.println("-----------");
    }

    public void close(MessageContext context) {
    }
}
