/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.test;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.remote.TransactionalProtocolHandlers;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.controller.remote.TransactionalProtocolClientImpl;
import org.jboss.as.controller.support.ChannelServer;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.security.PasswordClientCallbackHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Transactional protocol tests.
 *
 * @author Emanuel Muckenhuber
 */
public class TransactionalProtocolClientTestCase {

    private static final String ENDPOINT_NAME = "endpoint";
    private static final String URI_SCHEME = "test123";
    private static final String TEST_CHANNEL = "Test-Channel";
    private static final int PORT = 32123;
    private static final int CLIENTS = 10;

    private static final ModelNode SUCCESS = new ModelNode();
    private static final ModelNode FAILURE = new ModelNode();
    static {
        SUCCESS.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        SUCCESS.get(ModelDescriptionConstants.RESULT).set("test");
        FAILURE.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.FAILED);
        FAILURE.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).set("");
        FAILURE.get(ModelDescriptionConstants.ROLLED_BACK).set(true);
    }

    private ChannelServer channelServer;
    private IoFuture<Connection> futureConnection;
    private List<Channel> channels = new ArrayList<Channel>();

    // Only a single thread to be used on the client
    private final ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService remoteExecutors = Executors.newCachedThreadPool();
    private final BlockingQueue<MockController> transferQueue = new LinkedBlockingQueue<MockController>();

    @Before
    public void startChannelServer() throws Exception {
        final ChannelServer.Configuration configuration = new ChannelServer.Configuration();
        configuration.setEndpointName(ENDPOINT_NAME);
        configuration.setUriScheme(URI_SCHEME);
        configuration.setBindAddress(new InetSocketAddress("127.0.0.1", PORT));
        channelServer = ChannelServer.create(configuration);
        //
        channelServer.addChannelOpenListener(TEST_CHANNEL, new OpenListener() {
            @Override
            public void channelOpened(final Channel channel) {
                final MockController controller = new MockController();
                final ManagementChannelHandler channels = new ManagementChannelHandler(channel, remoteExecutors);
                TransactionalProtocolHandlers.addAsHandlerFactory(channels, controller);
                transferQueue.offer(controller);
                channel.receiveMessage(channels.getReceiver());
            }

            @Override
            public void registrationTerminated() {
                //
            }
        });
        final ProtocolChannelClient.Configuration connectionConfig = new ProtocolChannelClient.Configuration();
        connectionConfig.setEndpoint(channelServer.getEndpoint());
        connectionConfig.setUri(new URI("" + URI_SCHEME + "://127.0.0.1:" + PORT + ""));
        //
        final ProtocolChannelClient client = ProtocolChannelClient.create(connectionConfig);
        futureConnection = client.connect(new PasswordClientCallbackHandler("bob", ENDPOINT_NAME, "pass".toCharArray()));
    }

    @After
    public void stopChannels() throws Exception {
        for(final Channel channel : channels) {
            channel.close();
        }
        futureConnection.get().close();
        channelServer.close();
        channelServer = null;
    }

    @Test
    public void testSimpleRequest() throws Exception {
        //
        final TestUpdateWrapper update = createTestClient(0);
        final TransactionalProtocolClient client = update.getClient();
        final BlockingOperationListener listener = new BlockingOperationListener();
        client.execute(listener, update);
        //
        final TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper> prepared = listener.retrievePreparedOperation();
        assert ! prepared.isFailed();
        assert ! prepared.isDone();
        // Commit
        prepared.commit();
        // Block until we have the result
        final ModelNode result = prepared.getFinalResult().get();
        assert result.hasDefined(ModelDescriptionConstants.OUTCOME);
        assert prepared.isDone();
    }

    @Test
    public void testCancelBeforePrepared() throws Exception {

        final BlockingOperationListener listener = new BlockingOperationListener();
        final TestOperationHandler handler = new TestOperationHandler() {
            @Override
            public void execute(ModelNode operation, OperationMessageHandler handler, OperationAttachments attachments) throws Exception {
                try {
                    wait();
                } catch (InterruptedException e) {
                    //
                }
            }
        };
        //
        final TestUpdateWrapper wrapper = createTestClient(0, handler);
        final Future<ModelNode> futureResult = wrapper.execute(listener);
        // Now the server side should for latch to countDown
        futureResult.cancel(false);
        //
        OperationContext.ResultAction action = null;
        while(action == null) {
            action = wrapper.getResultAction();
            Thread.sleep(15);
        }
        wrapper.assertResultAction(OperationContext.ResultAction.ROLLBACK);
        final ModelNode result = futureResult.get();
        Assert.assertEquals(FAILURE, result);
    }

    @Test
    public void testConcurrentGroup() throws Exception {
        //
        final BlockingOperationListener listener = new BlockingOperationListener(CLIENTS);
        final List<TestUpdateWrapper> wrappers = createTestClients(CLIENTS);
        // First execute all operations
        for(final TestUpdateWrapper update : wrappers) {
            final TransactionalProtocolClient client = update.getClient();
            client.execute(listener, update);
        }
        // Now wait for all operations to be prepared
        final List<TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper>> preparedOps = new ArrayList<TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper>>();
        for(int i = 0; i < CLIENTS; i++) {
            // Wait for 10 prepared results
            final TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper> prepared = listener.retrievePreparedOperation();
            assert ! prepared.isFailed();
            assert ! prepared.isDone();

            // Check that the controller is locked in the prepared state
            final MockController controller = prepared.getOperation().getController();
            assert controller.lock.isLocked();

            // Commit all
            prepared.commit();
            preparedOps.add(prepared);
        }
        final ModelNode result = new ModelNode();
        // Now we just need to get the final results
        for(final TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper> prepared : preparedOps) {
            // Block until we have the result
            final ModelNode finalResult = prepared.getFinalResult().get();
            final MockController controller = prepared.getOperation().getController();
            assert prepared.isDone();
            assert ! controller.lock.isLocked();
            final int id = prepared.getOperation().getId();
            result.get("" + id).set(finalResult);
        }
        for(int i = 0; i < CLIENTS; i++) {
            assert result.hasDefined("" + i);
            assert result.get("" + i).hasDefined(ModelDescriptionConstants.OUTCOME);
        }
    }

    @Test
    public void testSequentialGroup() throws Exception {
        //
        final ModelNode result = new ModelNode();
        final BlockingOperationListener listener = new BlockingOperationListener(CLIENTS);
        final List<TestUpdateWrapper> wrappers = createTestClients(CLIENTS);
        for(final TestUpdateWrapper update : wrappers) {
            // Execute a single operation
            final TransactionalProtocolClient client = update.getClient();
            client.execute(listener, update);
            // Wait for the operation to reach the prepared state
            final TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper> prepared = listener.retrievePreparedOperation();
            assert ! prepared.isFailed();
            assert ! prepared.isDone();
            // Check that the controller is locked in the prepared state
            final MockController controller = update.getController();
            assert controller.lock.isLocked();
            // Commit
            prepared.commit();
            // Block until we have the result
            final ModelNode finalResult = prepared.getFinalResult().get();
            assert prepared.isDone();
            assert ! controller.lock.isLocked();
            final int id = prepared.getOperation().getId();
            result.get("" + id).set(finalResult);
            // onto to the next operation
        }
        for(int i = 0; i < CLIENTS; i++) {
            assert result.hasDefined("" + i);
            assert result.get("" + i).hasDefined(ModelDescriptionConstants.OUTCOME);
        }
    }

    /**
     * Create a bunch of test operation clients.
     *
     * @param count the amount of clients to be created
     * @return the create clients
     * @throws Exception
     */
    List<TestUpdateWrapper> createTestClients(final int count) throws Exception {
        final List<TestUpdateWrapper> wrappers = new ArrayList<TestUpdateWrapper>();
        for(int j = 0; j < count; j++) {
            wrappers.add(createTestClient(j));
        }
        return wrappers;
    }

    TestUpdateWrapper createTestClient(final int id) throws Exception {
        return createTestClient(id, null);
    }

    /**
     * Create a single test client.
     *
     * @param id the client id
     * @return the client
     * @throws Exception
     */
    TestUpdateWrapper createTestClient(final int id, final TestOperationHandler handler) throws Exception {
        final TransactionalProtocolClient client = createClient();
        final MockController controller = transferQueue.take();
        controller.handler = handler;
        return new TestUpdateWrapper(id, client, controller);
    }

    /**
     * Create the protocol client to talk to the remote controller.
     *
     * @return the client
     * @throws Exception
     */
    TransactionalProtocolClient createClient() throws Exception {
        final Connection connection = futureConnection.get();
        final IoFuture<Channel> channelIoFuture = connection.openChannel(TEST_CHANNEL, OptionMap.EMPTY);
        return createClient(channelIoFuture.get());
    }

    /**
     * Create the protocol client to talk to the remote controller.
     *
     * @param channel the remoting channel
     * @return the client
     * @throws Exception
     */
    TransactionalProtocolClient createClient(final Channel channel) {
        channels.add(channel);
        final ManagementChannelHandler channelAssociation = new ManagementChannelHandler(channel, clientExecutor);
        final TransactionalProtocolClientImpl handler = new TransactionalProtocolClientImpl(channelAssociation);
        channelAssociation.addHandlerFactory(handler);
        channel.receiveMessage(channelAssociation.getReceiver());
        return handler;
    }

    /**
     * A basic blocking operation listener implementation
     */
    static class BlockingOperationListener extends TransactionalProtocolHandlers.BlockingQueueOperationListener<TestUpdateWrapper> {

        BlockingOperationListener() {
            this(1);
        }

        BlockingOperationListener(int size) {
            super(new ArrayBlockingQueue<TransactionalProtocolClient.PreparedOperation<TestUpdateWrapper>>(size, true));
        }
    }

    /**
     * Operation wrapper.
     */
    static class TestUpdateWrapper extends TransactionalProtocolHandlers.OperationImpl {

        private final int id;
        private final MockController controller;
        private final TransactionalProtocolClient client;

        TestUpdateWrapper(final int id, final TransactionalProtocolClient client, final MockController controller) {
            super(SUCCESS, OperationMessageHandler.DISCARD, OperationAttachments.EMPTY);
            this.id = id;
            this.client = client;
            this.controller = controller;
        }

        public int getId() {
            return id;
        }

        public TransactionalProtocolClient getClient() {
            return client;
        }

        public MockController getController() {
            return controller;
        }

        OperationContext.ResultAction getResultAction() {
            return controller.action;
        }

        void assertResultAction(final OperationContext.ResultAction expected) {
            Assert.assertEquals(expected, getResultAction());
            controller.action = null;
        }

        Future<ModelNode> execute(TransactionalProtocolClient.OperationListener<TestUpdateWrapper> listener) throws IOException {
            return client.execute(listener, this);
        }

    }

    /**
     * A mock controller
     */
    private static class MockController implements ModelController {
        private final ReentrantLock lock = new ReentrantLock();
        private OperationContext.ResultAction action;
        private TestOperationHandler handler;

        @Override
        public ModelNode execute(final ModelNode operation, final OperationMessageHandler messageHandler,
                                 final OperationTransactionControl control, final OperationAttachments attachments) {
            lock.lock(); try {
                if(handler != null) {
                    try {
                        handler.execute(operation, messageHandler, attachments);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                control.operationPrepared(new OperationTransaction() {
                    @Override
                    public void commit() {
                        action = OperationContext.ResultAction.KEEP;
                    }

                    @Override
                    public void rollback() {
                        action = OperationContext.ResultAction.ROLLBACK;
                    }
                }, SUCCESS);
                return action == OperationContext.ResultAction.KEEP ? SUCCESS : FAILURE;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public ModelControllerClient createClient(Executor executor) {
            throw new IllegalStateException();
        }
    }

    private static interface TestOperationHandler {

        void execute(ModelNode operation, OperationMessageHandler handler, OperationAttachments attachments) throws Exception;

    }

}
