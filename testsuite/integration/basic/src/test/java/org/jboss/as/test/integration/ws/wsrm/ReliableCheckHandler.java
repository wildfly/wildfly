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
package org.jboss.as.test.integration.ws.wsrm;

import java.util.Iterator;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public class ReliableCheckHandler implements SOAPHandler<SOAPMessageContext> {

    int invoked = 0;
    private static Logger log = Logger.getLogger(ReliableCheckHandler.class);

    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleFault(SOAPMessageContext context) {
        handleContext(context);
        return true;
    }

    public boolean handleMessage(SOAPMessageContext context) {
        handleContext(context);
        return true;
    }

    private void handleContext(SOAPMessageContext smc) {
        Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        SOAPMessage message = smc.getMessage();

        invoked++;

        if (outboundProperty.booleanValue()) {
            log.debug("Outgoing message:");
        } else {
            log.debug("Incoming message:");
        }

        log.debug("-----------");
        try {
            message.writeTo(System.out);
            log.debug("");
        } catch (Exception e) {
            log.debug("Exception in handler: " + e);
        }
        log.debug("-----------");

        /*
         * 1 -- Body - CreateSequence
         * 2 -- Body - CreateSequenceResponse
         * 3 -- Header - wsrm:Sequence
         * 4 -- Header - wsrm:SequenceAcknowledgement
         */
        try {
            switch (invoked % 4) {
                case 1:
                    SOAPElement firstBodyElement = (SOAPElement) message.getSOAPBody().getChildElements().next();
                    if (!"CreateSequence".equals(firstBodyElement.getNodeName())) {
                        throw new WebServiceException("CreateSequence in soap body was expected, but it contains '"
                                + firstBodyElement.getElementName() + "'");
                    }
                    break;
                case 2:
                    firstBodyElement = (SOAPElement) message.getSOAPBody().getChildElements().next();
                    if (!"CreateSequenceResponse".equals(firstBodyElement.getNodeName())) {
                        throw new WebServiceException("CreateSequenceResponse in soap body was expected, but it contains '"
                                + firstBodyElement.getElementName() + "'");
                    }
                    break;
                case 3:
                    Iterator headerElements = message.getSOAPHeader().getChildElements();
                    boolean found = false;
                    while (headerElements.hasNext()) {
                        SOAPElement soapElement = (SOAPElement) headerElements.next();
                        if ("wsrm:Sequence".equals(soapElement.getNodeName())) {
                            found = true;
                        }
                    }
                    if (!found) {
                        throw new WebServiceException("wsrm:Sequence is not present in soap header");
                    }
                    break;
                case 0:
                    headerElements = message.getSOAPHeader().getChildElements();
                    found = false;
                    while (headerElements.hasNext()) {
                        SOAPElement soapElement = (SOAPElement) headerElements.next();
                        if ("wsrm:SequenceAcknowledgement".equals(soapElement.getNodeName())) {
                            found = true;
                        }
                    }
                    if (!found) {
                        throw new WebServiceException("wsrm:SequenceAcknowledgement is not present in soap header");
                    }
                    break;
            }
        } catch (SOAPException ex) {
            throw new WebServiceException(ex.getMessage(), ex);
        }
    }

    public void close(MessageContext context) {
    }
}
