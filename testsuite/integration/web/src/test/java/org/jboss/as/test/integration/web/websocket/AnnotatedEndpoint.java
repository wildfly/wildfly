package org.jboss.as.test.integration.web.websocket;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * @author Stuart Douglas
 */
@ServerEndpoint("/websocket/{name}")
public class AnnotatedEndpoint {

    @OnMessage
    public String message(String message, @PathParam("name") String name) {
        return message + " " + name;
    }

}
