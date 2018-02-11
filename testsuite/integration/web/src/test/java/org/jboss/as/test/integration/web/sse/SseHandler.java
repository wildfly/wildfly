package org.jboss.as.test.integration.web.sse;

import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import io.undertow.servlet.sse.ServerSentEvent;

import java.io.IOException;

/**
 * @author Stuart Douglas
 */
@ServerSentEvent("/foo/{bar}")
public class SseHandler implements ServerSentEventConnectionCallback {
    @Override
    public void connected(ServerSentEventConnection connection, String lastEventId) {
        connection.send("Hello " + connection.getParameter("bar"));
        connection.send("msg2");
        connection.send("msg3", new ServerSentEventConnection.EventCallback() {
            @Override
            public void done(ServerSentEventConnection connection, String data, String event, String id) {
                try {
                    connection.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void failed(ServerSentEventConnection connection, String data, String event, String id, IOException e) {
                try {
                    connection.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
