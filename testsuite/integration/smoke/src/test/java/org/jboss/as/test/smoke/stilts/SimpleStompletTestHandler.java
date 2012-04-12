/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.smoke.stilts;

import static org.jboss.as.test.smoke.stilts.bundle.SimpleStomplet.DESTINATION_QUEUE_ONE;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.StompMessages;
import org.projectodd.stilts.stomp.client.ClientSubscription;
import org.projectodd.stilts.stomp.client.MessageHandler;
import org.projectodd.stilts.stomp.client.StompClient;
import org.projectodd.stilts.stomp.client.SubscriptionBuilder;
import org.projectodd.stilts.stomplet.Stomplet;

/**
 * A simple {@link Stomplet} test.
 *
 * @author thomas.diesler@jboss.com
 * @since 09-Sep-2010
 */
public class SimpleStompletTestHandler {

    static final String DEPLOYMENT_NAME = "simple-stomplet";

    public static void testSendWithNoTx() throws Exception {
            StompClient client = new StompClient("stomp://localhost");
            client.connect();

            final Set<String> outbound = new HashSet<String>();
            final CountDownLatch outboundLatch = new CountDownLatch(2);
            SubscriptionBuilder builder = client.subscribe(DESTINATION_QUEUE_ONE);
            builder.withMessageHandler(new MessageHandler() {
                public void handle(StompMessage message) {
                    String content = message.getContentAsString();
                    outbound.add(content);
                    outboundLatch.countDown();
                }
            });
            ClientSubscription subscription = builder.start();

            client.send(StompMessages.createStompMessage(DESTINATION_QUEUE_ONE, "msg1"));
            client.send(StompMessages.createStompMessage(DESTINATION_QUEUE_ONE, "msg2"));

            Assert.assertTrue("No latch timeout", outboundLatch.await(10, TimeUnit.SECONDS));
            Assert.assertTrue("Contains msg1", outbound.contains("msg1"));
            Assert.assertTrue("Contains msg2", outbound.contains("msg2"));

            subscription.unsubscribe();
            client.disconnect();
    }
}
