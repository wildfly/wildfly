/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.weld.deployment;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 3/26/19.
 */
@RunWith(Arquillian.class)
public class WebsocketApplicationScopedTestCase {
    private static final String CLIENT_STANDALONE = "standalone";
    private static final HashMap<String, LinkedBlockingDeque<String>> queues = new HashMap<>();
    private static final long TIMEOUT = TimeoutUtil.adjust(20000);

    @Deployment(testable = false, name = CLIENT_STANDALONE)
    public static WebArchive createThinDeployment() {
        return ShrinkWrap.create(WebArchive.class, CLIENT_STANDALONE + ".war")
                .addClasses(ChatWebsocketResource.class)
                .addAsManifestResource(new StringAsset("io.undertow.websockets.jsr.UndertowContainerProvider"),
                        "services/jakarta.websocket.ContainerProvider");
    }

    @Test
    @OperateOnDeployment(CLIENT_STANDALONE)
    @RunAsClient
    public void testClientStandalone(@ArquillianResource URL webapp) throws Exception {
        URI uriUser1 = createUri(webapp,"/chat/user1");
        URI uriUser2 = createUri(webapp,"/chat/user2");

        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uriUser1)) {
            //Wait until the client is initialized e.q. the OnOpen is executed
            waitForClientInitialization(queues, 1);

            //Check if the user1 is connected
            Assert.assertEquals("CONNECT", queues.get(session.getId()).poll(10, TimeUnit.SECONDS));
            Assert.assertEquals("User user1 joined", queues.get(session.getId()).poll(10, TimeUnit.SECONDS));
            session.getAsyncRemote().sendText("hello world");
            Assert.assertEquals(">> user1: hello world", queues.get(session.getId()).poll(10, TimeUnit.SECONDS));

            try (Session sessioUser2 = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uriUser2)) {
                //Wait until the client is initialized e.q. the OnOpen is executed
                waitForClientInitialization(queues, 2);

                //Assert that the sessioUser2 got the messages
                Assert.assertEquals("CONNECT", queues.get(sessioUser2.getId()).poll(10, TimeUnit.SECONDS));
                Assert.assertEquals("User user2 joined", queues.get(sessioUser2.getId()).poll(10, TimeUnit.SECONDS));
                sessioUser2.getAsyncRemote().sendText("hello world");
                Assert.assertEquals(">> user2: hello world", queues.get(sessioUser2.getId()).poll(10, TimeUnit.SECONDS));

                //Assert that the user1's session got user2's messages
                Assert.assertEquals("User user2 joined", queues.get(session.getId()).poll(10, TimeUnit.SECONDS));
                Assert.assertEquals(">> user2: hello world", queues.get(session.getId()).poll(10, TimeUnit.SECONDS));
            }
        }
    }

    private void waitForClientInitialization(HashMap<String, LinkedBlockingDeque<String>> queues, int expectedQueueSize) throws InterruptedException {
        long end = System.currentTimeMillis() + TIMEOUT;
        while (end > System.currentTimeMillis()) {
            if (queues.size() > expectedQueueSize-1) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(queues.size() > expectedQueueSize-1);
    }

    private URI createUri(URL webapp, String path) throws URISyntaxException {
        return new URI("ws", "", TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getHttpPort(), webapp.getPath() + path, "", "");
    }

    @ClientEndpoint
    public static class Client {

        @OnOpen
        public void open(Session session) {
            queues.put(session.getId(), new LinkedBlockingDeque<>());
            queues.get(session.getId()).add("CONNECT");
        }

        @OnMessage
        void message(Session session, String msg) {
            queues.get(session.getId()).add(msg);
        }

    }

}



