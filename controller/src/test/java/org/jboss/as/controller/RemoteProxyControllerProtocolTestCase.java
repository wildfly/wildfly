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
import static junit.framework.Assert.assertFalse;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewProxyController.ProxyOperationControl;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.NewModelControllerClient;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.NewRemoteProxyController;
import org.jboss.as.controller.remote.NewTransactionalModelControllerOperationHandler;
import org.jboss.as.controller.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.ManagementBatchIdManager;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFutureTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteProxyControllerProtocolTestCase {

    RemoteChannelPairSetup channels;
    @Before
    public void start() throws Exception {
        System.out.println("---- Test ----");
        channels = new RemoteChannelPairSetup();
        channels.setupRemoting();
        channels.startChannels();
    }

    @After
    public void stop() throws Exception {
        System.out.println("Closing channels");
        channels.stopChannels();
        channels.shutdownRemoting();
    }

    @Test
    public void testOperationMessageHandler() throws Exception {
        final MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                this.operation = operation;
                handler.handleReport(MessageSeverity.INFO, "Test1");
                handler.handleReport(MessageSeverity.INFO, "Test2");
                control.operationPrepared(new OperationTransaction() {

                    @Override
                    public void rollback() {
                    }

                    @Override
                    public void commit() {
                    }
                }, new ModelNode());
                return new ModelNode();
            }
        };
        final NewRemoteProxyController proxyController = setupProxyHandlers(controller);


        ModelNode operation = new ModelNode();
        operation.get("test").set("123");

        final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();

        CommitProxyOperationControl commitControl = new CommitProxyOperationControl();
        proxyController.execute(operation,
                new OperationMessageHandler() {

                    @Override
                    public void handleReport(MessageSeverity severity, String message) {
                        if (severity == MessageSeverity.INFO && message.startsWith("Test")) {
                            messages.add(message);
                        }
                    }
                },
                commitControl,
                null);
        assertEquals("123", controller.getOperation().get("test").asString());
        assertEquals("Test1", messages.take());
        assertEquals("Test2", messages.take());
        commitControl.latch.await();
    }

    @Test
    public void testOperationControlFailed() throws Exception {
        final MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                final ModelNode result = new ModelNode();
                result.get(OUTCOME).set(FAILED);
                result.get(FAILURE_DESCRIPTION).set("broken");
                return result;
            }
        };
        final NewRemoteProxyController proxyController = setupProxyHandlers(controller);

        ModelNode operation = new ModelNode();
        operation.get("test").set("123");

        final AtomicBoolean prepared = new AtomicBoolean();
        final AtomicBoolean completed = new AtomicBoolean();
        final TestFuture<ModelNode> failure = new TestFuture<ModelNode>();
        proxyController.execute(operation,
                null,
                new ProxyOperationControl() {

                    @Override
                    public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                        prepared.set(true);
                    }

                    @Override
                    public void operationFailed(ModelNode response) {
                        failure.done(response);
                    }

                    @Override
                    public void operationCompleted(ModelNode response) {
                        completed.set(true);
                    }
                },
                null);
        ModelNode result = failure.get();
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("broken", result.get(FAILURE_DESCRIPTION).asString());
        assertFalse(prepared.get());
        assertFalse(completed.get());
    }

    @Test
    public void testOperationControlExceptionInController() throws Exception {
        final MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {
                throw new RuntimeException("Crap");
            }
        };
        final NewRemoteProxyController proxyController = setupProxyHandlers(controller);

        ModelNode operation = new ModelNode();
        operation.get("test").set("123");

        final AtomicBoolean prepared = new AtomicBoolean();
        final AtomicBoolean completed = new AtomicBoolean();
        final TestFuture<ModelNode> failure = new TestFuture<ModelNode>();
        proxyController.execute(operation,
                null,
                new ProxyOperationControl() {

                    @Override
                    public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                        prepared.set(true);
                    }

                    @Override
                    public void operationFailed(ModelNode response) {
                        failure.done(response);
                    }

                    @Override
                    public void operationCompleted(ModelNode response) {
                        completed.set(true);
                    }
                },
                null);
        ModelNode result = failure.get();
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("java.lang.RuntimeException:Crap", result.get(FAILURE_DESCRIPTION).asString());
        assertFalse(prepared.get());
        assertFalse(completed.get());
    }

    @Test
    public void testTransactionCommit() throws Exception {

        final OperationTransaction tx = new OperationTransaction() {

            @Override
            public void rollback() {
            }

            @Override
            public void commit() {
            }
        };
        MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {

                ModelNode node = new ModelNode();
                node = new ModelNode();
                node.get(OUTCOME).set(SUCCESS);
                node.get(RESULT).set("prepared");
                control.operationPrepared(tx, node);

                node.get(RESULT).set("final");
                return node;
            }
        };

        final NewRemoteProxyController proxyController = setupProxyHandlers(controller);

        ModelNode operation = new ModelNode();
        operation.get("test").set("123");

        final AtomicBoolean failed = new AtomicBoolean();
        final TestFuture<ModelNode> prepared = new TestFuture<ModelNode>();
        final TestFuture<OperationTransaction> preparedTx = new TestFuture<OperationTransaction>();
        final TestFuture<ModelNode> result = new TestFuture<ModelNode>();
        proxyController.execute(operation,
                null,
                new ProxyOperationControl() {

                    @Override
                    public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                        prepared.done(result);
                        preparedTx.done(transaction);
                    }

                    @Override
                    public void operationFailed(ModelNode response) {
                        failed.set(true);
                    }

                    @Override
                    public void operationCompleted(ModelNode response) {
                        result.done(response);
                    }
                },
                null);

        ModelNode preparedResult = prepared.get();
        assertEquals(SUCCESS, preparedResult.get(OUTCOME).asString());
        assertEquals("prepared", preparedResult.get(RESULT).asString());
        assertFalse(failed.get());
        assertFalse(result.isDone());
        preparedTx.get().commit();

        ModelNode finalResult = result.get();
        assertEquals(SUCCESS, finalResult.get(OUTCOME).asString());
        assertEquals("final", finalResult.get(RESULT).asString());
    }

    @Test
    public void testTransactionRollback() throws Exception {
        final OperationTransaction tx = new OperationTransaction() {

            @Override
            public void rollback() {
            }

            @Override
            public void commit() {
            }
        };
        MockModelController controller = new MockModelController() {
            @Override
            public ModelNode execute(ModelNode operation, OperationMessageHandler handler, OperationTransactionControl control, OperationAttachments attachments) {

                ModelNode node = new ModelNode();
                node = new ModelNode();
                node.get(OUTCOME).set(SUCCESS);
                node.get(RESULT).set("prepared");
                control.operationPrepared(tx, node);

                return node;
            }
        };

        final NewRemoteProxyController proxyController = setupProxyHandlers(controller);

        ModelNode operation = new ModelNode();
        operation.get("test").set("123");

        final AtomicBoolean failed = new AtomicBoolean();
        final TestFuture<ModelNode> prepared = new TestFuture<ModelNode>();
        final TestFuture<OperationTransaction> preparedTx = new TestFuture<OperationTransaction>();
        final TestFuture<ModelNode> result = new TestFuture<ModelNode>();
        proxyController.execute(operation,
                null,
                new ProxyOperationControl() {

                    @Override
                    public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                        prepared.done(result);
                        preparedTx.done(transaction);
                    }

                    @Override
                    public void operationFailed(ModelNode response) {
                        failed.set(true);
                    }

                    @Override
                    public void operationCompleted(ModelNode response) {
                        result.done(response);
                    }
                },
                null);

        ModelNode preparedResult = prepared.get();
        assertEquals(SUCCESS, preparedResult.get(OUTCOME).asString());
        assertEquals("prepared", preparedResult.get(RESULT).asString());
        assertFalse(failed.get());
        assertFalse(result.isDone());
        preparedTx.get().rollback();
        assertEquals(SUCCESS, result.get().get(OUTCOME).asString());
        assertEquals("prepared", result.get().get(RESULT).asString());
    }

    @Test
    public void testAttachmentInputStreams() throws Exception {

        final byte[] firstBytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final byte[] secondBytes = new byte[] {10, 9, 8 , 7 , 6, 5, 4, 3, 2, 1};

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
                control.operationPrepared(new OperationTransaction() {

                    @Override
                    public void rollback() {
                    }

                    @Override
                    public void commit() {
                    }
                }, new ModelNode());
                return new ModelNode();
            }
        };

        final NewRemoteProxyController proxyController = setupProxyHandlers(controller);

        ModelNode operation = new ModelNode();
        operation.get("test").set("123");

        OperationAttachments attachments = new OperationAttachments() {

            @Override
            public List<InputStream> getInputStreams() {
                ArrayList<InputStream> streams = new ArrayList<InputStream>();
                streams.add(new ByteArrayInputStream(firstBytes));
                streams.add(new ByteArrayInputStream(secondBytes));
                streams.add(null);
                return streams;
            }
        };

        CommitProxyOperationControl commitControl = new CommitProxyOperationControl();
        proxyController.execute(operation,
                null,
                commitControl,
                attachments);
        assertEquals(3, size.get());
        assertArrays(firstBytes, firstResult.get());
        assertArrays(secondBytes, secondResult.get());
        assertArrays(new byte[0], thirdResult.get());
        commitControl.latch.await();
        System.out.println("--------- done");
    }

    private void assertArrays(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0 ; i < expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    private NewRemoteProxyController setupProxyHandlers(MockModelController controller) {
        ManagementChannel serverChannel = channels.getServerChannel();
        ManagementChannel clientChannel = channels.getClientChannel();
        clientChannel.startReceiving();

        NewTransactionalModelControllerOperationHandler operationHandler = new NewTransactionalModelControllerOperationHandler(channels.getExecutorService(), controller);
        serverChannel.setOperationHandler(operationHandler);
        serverChannel.setBatchIdManager(ManagementBatchIdManager.DEFAULT);

        NewRemoteProxyController proxyController = NewRemoteProxyController.create(channels.getExecutorService(), PathAddress.pathAddress(), channels.getClientChannel());
        clientChannel.setOperationHandler(proxyController);

        return proxyController;
    }

    private static abstract class MockModelController implements NewModelController {
        protected volatile ModelNode operation;

        ModelNode getOperation() {
            return operation;
        }

        @Override
        public NewModelControllerClient createClient(Executor executor) {
            return null;
        }

    }

    private static class CommitProxyOperationControl implements ProxyOperationControl {
        CountDownLatch latch = new CountDownLatch(1);
        @Override
        public void operationPrepared(OperationTransaction transaction, ModelNode result) {
            transaction.commit();
        }

        @Override
        public void operationFailed(ModelNode response) {
        }

        @Override
        public void operationCompleted(ModelNode response) {
            latch.countDown();
        }

    }

    private static class TestFuture<T> extends AsyncFutureTask<T>{
        protected TestFuture() {
            super(null);
        }

        void done(T result) {
            super.setResult(result);
        }
    }

}
