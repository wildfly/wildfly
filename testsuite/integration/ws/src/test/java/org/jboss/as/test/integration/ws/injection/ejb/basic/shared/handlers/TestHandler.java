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
package org.jboss.as.test.integration.ws.injection.ejb.basic.shared.handlers;

import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.BeanIface;
import org.jboss.logging.Logger;
import org.jboss.ws.api.handler.GenericSOAPHandler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * This handler is initialized via injections.
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public final class TestHandler extends GenericSOAPHandler {
    // provide logging
    private static final Logger log = Logger.getLogger(TestHandler.class);

    @Resource(name = "boolean1")
    private Boolean boolean1;

    @EJB
    private BeanIface bean1;

    /**
     * Indicates whether handler is in correct state.
     */
    private boolean correctState;

    @PostConstruct
    private void init() {
        boolean correctInitialization = true;

        // verify @EJB annotation driven injection
        if (this.bean1 == null || !this.bean1.printString().equals("Injected hello message")) {
            log.error("Annotation driven initialization for bean1 failed");
            correctInitialization = false;
        } else {
            log.trace("@EJB annotation driven injection OK");
        }

        // verify @Resource annotation driven injection
        if (this.boolean1 == null || this.boolean1 != true) {
            log.error("Annotation driven initialization for boolean1 failed -- " + boolean1);
            correctInitialization = false;
        } else {
            log.trace("@Resource annotation driven injection OK");
        }

        this.correctState = correctInitialization;
    }

    @Override
    public boolean handleOutbound(MessageContext msgContext) {
        return ensureInjectionsAndInitialization(msgContext, "Outbound");
    }

    @Override
    public boolean handleInbound(MessageContext msgContext) {
        return ensureInjectionsAndInitialization(msgContext, "Inbound");
    }

    private boolean ensureInjectionsAndInitialization(MessageContext msgContext, String direction) {
        if (!this.correctState) {
            throw new WebServiceException("Unfunctional injections");
        }

        try {
            SOAPMessage soapMessage = ((SOAPMessageContext) msgContext).getMessage();
            SOAPElement soapElement = (SOAPElement) soapMessage.getSOAPBody().getChildElements().next();
            soapElement = (SOAPElement) soapElement.getChildElements().next();

            String oldValue = soapElement.getValue();
            String newValue = oldValue + ":" + direction + ":TestHandler";
            soapElement.setValue(newValue);

            log.debug("oldValue: " + oldValue);
            log.debug("newValue: " + newValue);

            return true;
        } catch (SOAPException ex) {
            throw new WebServiceException(ex);
        }
    }

}
