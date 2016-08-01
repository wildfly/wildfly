package org.jboss.as.test.integration.web.security.websocket;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

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
