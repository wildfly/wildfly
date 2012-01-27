/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.controller;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.ExistingChannelModelControllerClient;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandler;
import org.jboss.as.controller.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.HandleableCloseable;
import org.jboss.threads.AsyncFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xnio.IoUtils;
/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ModelControllerClientTestCase {

    Logger log = Logger.getLogger(ModelControllerClientTestCase.class);

    private RemoteChannelPairSetup channels;

    @Before
    public void start() throws Exception {
        channels = new RemoteChannelPairSetup();
    }

    @After
    public void stop() throws Exception {
        channels.stopChannels();
        channels.shutdownRemoting();
    }

    private ModelControllerClient setupTestClient(final ModelController controller) throws IOException {
        try {
            channels.setupRemoting(new ManagementChannelInitialization() {
                @Override
                public HandleableCloseable.Key startReceiving(Channel channel) {
                    final ManagementChannelHandler support = new ManagementChannelHandler(channel, channels.getExecutorService());
                    support.addHandlerFactory(new ModelControllerClientOperationHandler(controller, support));
                    channel.receiveMessage(support.getReceiver());
                    return null;
                }
            });
            channels.startClientConnetion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final Channel clientChannel = channels.getClientChannel();
        return ExistingChannelModelControllerClient.createReceiving(clientChannel, channels.getExecutorService());
    }

    @Test @Ignore("OperationMessageHandlerProxy turned off temporarily")
    public void testSynchronousOperationMessageHandler() throws Exception {

        final CountDownLatch executeLatch = new CountDownLatch(1);
        MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                this.operation = operation;
                handler.handleReport(MessageSeverity.INFO, "Test1");
                handler.handleReport(MessageSeverity.INFO, "Test2");
                executeLatch.countDown();
                ModelNode result = new ModelNode();
                result.get("testing").set(operation.get("test"));
                return result;
            }
        };
        final ModelControllerClient client = setupTestClient(controller);
        try {
            ModelNode operation = new ModelNode();
            operation.get("test").set("123");

            final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();

            ModelNode result = client.execute(operation,
                    new OperationMessageHandler() {

                        @Override
                        public void handleReport(MessageSeverity severity, String message) {
                            if (severity == MessageSeverity.INFO && message.startsWith("Test")) {
                                messages.add(message);
                            }
                        }
                    });
            assertEquals("123", controller.getOperation().get("test").asString());
            assertEquals("123", result.get("testing").asString());
            assertEquals("Test1", messages.take());
            assertEquals("Test2", messages.take());
        } finally {
            IoUtils.safeClose(client);
        }
    }

    @Test
    public void testSynchronousAttachmentInputStreams() throws Exception {

        final byte[] firstBytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final byte[] secondBytes = new byte[] {10, 9, 8 , 7 , 6, 5, 4, 3, 2, 1};
        final byte[] thirdBytes = new byte[] {1};

        final CountDownLatch executeLatch = new CountDownLatch(1);
        final AtomicInteger size = new AtomicInteger();
        final AtomicReference<byte[]> firstResult = new AtomicReference<byte[]>();
        final AtomicReference<byte[]> secondResult = new AtomicReference<byte[]>();
        final AtomicReference<byte[]> thirdResult = new AtomicReference<byte[]>();
        MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                int streamIndex = 0;
                for (InputStream in : attachments.getInputStreams()) {
                    try {
                        ArrayList<Integer> readBytes = new ArrayList<Integer>();
                        int b = in.read();
                        while (b != -1) {
                            readBytes.add(b);
                            b = in.read();
                        }

                        byte[] bytes = new byte[readBytes.size()];
                        for (int i = 0 ; i < bytes.length ; i++) {
                            bytes[i] = (byte)readBytes.get(i).intValue();
                        }

                        if (streamIndex == 0) {
                            firstResult.set(bytes);
                        } else if (streamIndex == 1) {
                            secondResult.set(bytes);
                        } else if (streamIndex == 2) {
                            thirdResult.set(bytes);
                        }
                        streamIndex++;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                size.set(streamIndex);
                executeLatch.countDown();
                return new ModelNode();
            }
        };
        // Set the handler
        final ModelControllerClient client = setupTestClient(controller);
        try {
            ModelNode operation = new ModelNode();
            operation.get("test").set("123");

            ModelNode op = new ModelNode();
            op.get("name").set(123);
            OperationBuilder builder = new OperationBuilder(op);
            builder.addInputStream(new ByteArrayInputStream(firstBytes));
            builder.addInputStream(new ByteArrayInputStream(secondBytes));
            builder.addInputStream(new ByteArrayInputStream(thirdBytes));
            client.execute(builder.build());
            executeLatch.await();
            assertEquals(3, size.get());
            assertArrays(firstBytes, firstResult.get());
            assertArrays(secondBytes, secondResult.get());
            assertArrays(new byte[] { 1 }, thirdResult.get());
        } finally {
            IoUtils.safeClose(client);
        }
    }

    @Test @Ignore("OperationMessageHandlerProxy turned off temporarily")
    public void testAsynchronousOperationWithMessageHandler() throws Exception {
        final CountDownLatch executeLatch = new CountDownLatch(1);
        MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                this.operation = operation;
                handler.handleReport(MessageSeverity.INFO, "Test1");
                handler.handleReport(MessageSeverity.INFO, "Test2");
                executeLatch.countDown();
                ModelNode result = new ModelNode();
                result.get("testing").set(operation.get("test"));
                return result;
            }
        };

        // Set the handler
        final ModelControllerClient client = setupTestClient(controller);
        try {

            ModelNode operation = new ModelNode();
            operation.get("test").set("123");

            final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();

            AsyncFuture<ModelNode> resultFuture = client.executeAsync(operation,
                    new OperationMessageHandler() {

                        @Override
                        public void handleReport(MessageSeverity severity, String message) {
                            if (severity == MessageSeverity.INFO && message.startsWith("Test")) {
                                messages.add(message);
                            }
                        }
                    });
            ModelNode result = resultFuture.get();
            assertEquals("123", controller.getOperation().get("test").asString());
            assertEquals("123", result.get("testing").asString());
            assertEquals("Test1", messages.take());
            assertEquals("Test2", messages.take());
        } finally {
            IoUtils.safeClose(client);
        }
    }

    @Test
    public void testCancelAsynchronousOperation() throws Exception {
        final CountDownLatch executeLatch = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                this.operation = operation;
                executeLatch.countDown();

                try {
                    log.debug("Waiting for interrupt");
                    //Wait for this operation to be cancelled
                    Thread.sleep(10000000);
                    ModelNode result = new ModelNode();
                    result.get("testing").set(operation.get("test"));
                    return result;
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    throw new RuntimeException(e);
                }
            }
        };

        // Set the handler
        final ModelControllerClient client = setupTestClient(controller);
        try {
            ModelNode operation = new ModelNode();
            operation.get("test").set("123");

            final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();

            AsyncFuture<ModelNode> resultFuture = client.executeAsync(operation,
                    new OperationMessageHandler() {

                        @Override
                        public void handleReport(MessageSeverity severity, String message) {
                            if (severity == MessageSeverity.INFO && message.startsWith("Test")) {
                                messages.add(message);
                            }
                        }
                    });
            executeLatch.await();
            resultFuture.cancel(false);
            interrupted.await();
        } finally {
            IoUtils.safeClose(client);
        }

    }

    private void assertArrays(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0 ; i < expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    private static abstract class MockModelController implements ModelController {
        protected volatile ModelNode operation;

        ModelNode getOperation() {
            return operation;
        }

        @Override
        public ModelControllerClient createClient(Executor executor) {
            return null;
        }

    }

}
