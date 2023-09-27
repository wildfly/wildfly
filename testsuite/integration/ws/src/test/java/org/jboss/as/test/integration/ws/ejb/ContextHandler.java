/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.ejb;

import jakarta.xml.ws.handler.MessageContext;

import org.jboss.ws.api.handler.GenericSOAPHandler;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public class ContextHandler extends GenericSOAPHandler {

    @Override
    protected boolean handleInbound(MessageContext msgContext) {
        msgContext.put("invoked", "ContextHandler:handleInbound()");
        msgContext.setScope("invoked", MessageContext.Scope.APPLICATION);
        return true;
    }
}
