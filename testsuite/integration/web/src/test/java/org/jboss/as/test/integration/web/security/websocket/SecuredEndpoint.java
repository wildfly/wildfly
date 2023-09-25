/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.websocket;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * @author Stuart Douglas
 */
@ServerEndpoint("/websocket")
public class SecuredEndpoint {

    @OnMessage
    public String message(String message, Session session) {
        return message + " " + session.getUserPrincipal();
    }

}
