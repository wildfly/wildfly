/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.injection.ejb.basic.shared.handlers;

import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.BeanIface;
import org.jboss.logging.Logger;
import org.jboss.ws.api.handler.GenericSOAPHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

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
