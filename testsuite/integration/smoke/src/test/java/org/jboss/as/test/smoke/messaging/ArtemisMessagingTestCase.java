/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * [TODO]
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@ExtendWith(ArquillianExtension.class)
public class ArtemisMessagingTestCase {
    private static final String QUEUE_EXAMPLE_QUEUE = "queue.exampleQueue";

    static final Logger log = Logger.getLogger(ArtemisMessagingTestCase.class);

    private static final String BODY = "msg.body";

    private ClientSessionFactory sf;
    private ClientSession session;


    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "messaging-example.jar");
        jar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n" +
                "Dependencies: org.apache.activemq.artemis.client, org.jboss.dmr, org.jboss.as.controller-client\n"), "MANIFEST.MF");
        jar.addClass(ArtemisMessagingTestCase.class);
        return jar;
    }

    @BeforeEach
    public void start() throws Exception {
        //Not using JNDI so we use the core services directly
        sf = ActiveMQClient.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName())).createSessionFactory();
        session = sf.createSession();

        //Create a queue
        ClientSession coreSession = sf.createSession();
        coreSession.createQueue(QUEUE_EXAMPLE_QUEUE, QUEUE_EXAMPLE_QUEUE, false);
        coreSession.close();

        session = sf.createSession();
        session.start();
    }

    @AfterEach
    public void stop() throws Exception {
        if (session != null) {
            session.close();
        }

        if (sf != null) {
            ClientSession coreSession = sf.createSession();
            coreSession.deleteQueue(QUEUE_EXAMPLE_QUEUE);
            coreSession.close();

            sf.close();
        }
    }

    @Test
    public void testMessaging() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientMessage> message = new AtomicReference<ClientMessage>();

        ClientConsumer consumer = session.createConsumer(QUEUE_EXAMPLE_QUEUE);
        consumer.setMessageHandler(new MessageHandler() {
            @Override
            public void onMessage(ClientMessage m) {
                try {
                    m.acknowledge();
                    message.set(m);
                    latch.countDown();
                } catch (ActiveMQException e) {
                    e.printStackTrace();
                }
            }
        });

        String text = UUID.randomUUID().toString();

        sendMessage(text);

        assertTrue(latch.await(1, SECONDS));
        assertEquals(text, message.get().getStringProperty(BODY));
    }

    private void sendMessage(String text) throws Exception {
        ClientProducer producer = session.createProducer(QUEUE_EXAMPLE_QUEUE);
        ClientMessage message = session.createMessage(false);

        message.putStringProperty(BODY, text);
        log.trace("-----> Sending message");
        producer.send(message);
    }
}
