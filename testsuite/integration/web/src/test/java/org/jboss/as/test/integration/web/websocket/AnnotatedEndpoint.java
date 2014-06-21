package org.jboss.as.test.integration.web.websocket;

import javax.websocket.OnMessage;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

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
