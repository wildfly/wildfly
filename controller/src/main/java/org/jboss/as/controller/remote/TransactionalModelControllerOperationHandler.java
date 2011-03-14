/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.remote;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ControllerTransaction;
import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.TransactionalModelController;
import org.jboss.as.controller.client.ModelControllerClientProtocol;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.dmr.ModelNode;

/**
 * Extends {@link ModelControllerOperationHandlerImpl} to support transactional operations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TransactionalModelControllerOperationHandler extends ModelControllerOperationHandlerImpl {

    private final TransactionalModelController transactionalModelController;

    private final ConcurrentMap<ModelNode, ControllerTransaction> transactions = new ConcurrentHashMap<ModelNode, ControllerTransaction>();

    public static final byte HANDLER_ID = (byte)0x10;
    public static final byte EXECUTE_TRANSACTIONAL_REQUEST = (byte)0x21;
    public static final byte EXECUTE_TRANSACTIONAL_RESPONSE = (byte)0x22;
    public static final byte EXECUTE_TRANSACTIONAL_SYNCHRONOUS_REQUEST = (byte)0x23;
    public static final byte EXECUTE_TRANSACTIONAL_SYNCHRONOUS_RESPONSE = (byte)0x24;
    public static final byte TRANSACTION_COMMIT_REQUEST = (byte)0x25;
    public static final byte TRANSACTION_COMMIT_RESPONSE = (byte)0x26;
    public static final byte TRANSACTION_ROLLBACK_REQUEST = (byte)0x27;
    public static final byte TRANSACTION_ROLLBACK_RESPONSE = (byte)0x28;
    public static final byte TRANSACTION_ID = (byte)0x30;

    public TransactionalModelControllerOperationHandler(TransactionalModelController modelController, MessageHandler initiatingHandler) {
        super(modelController, initiatingHandler);
        this.transactionalModelController = modelController;
    }

    /** {@inheritDoc} */
    @Override
    public byte getIdentifier() {
        return HANDLER_ID;
    }

    @Override
    public ManagementResponse operationFor(final byte commandByte) {
        switch (commandByte) {
            case EXECUTE_TRANSACTIONAL_REQUEST:
                return new ExecuteTransactionalAsynchronousOperation();
            case EXECUTE_TRANSACTIONAL_SYNCHRONOUS_REQUEST:
                return new ExecuteTransactionalSynchronousOperation();
            case TRANSACTION_COMMIT_REQUEST:
                return new CommitTransactionOperation();
            case TRANSACTION_ROLLBACK_REQUEST:
                return new RollbackTransactionOperation();
            default:
                ManagementResponse rsp = super.operationFor(commandByte);
                return rsp;
        }
    }

    private ModelNode readNode(InputStream in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private abstract class ExecuteTransactionalOperation extends ManagementResponse {
        OperationBuilder builder;
        ModelNode txId;

        ExecuteTransactionalOperation() {
            super(getInitiatingHandler());
        }

        @Override
        protected void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, TRANSACTION_ID);
            txId = new ModelNode();
            txId.readExternal(inputStream);
            expectHeader(inputStream, ModelControllerClientProtocol.PARAM_OPERATION);
            builder = OperationBuilder.Factory.create(readNode(inputStream));

            int cmd = inputStream.read();
            if (cmd == ModelControllerClientProtocol.PARAM_REQUEST_END) {
                return;
            }
            if (cmd != ModelControllerClientProtocol.PARAM_INPUT_STREAM) {
                throw new IllegalArgumentException("Expected " + ModelControllerClientProtocol.PARAM_INPUT_STREAM + " received " + cmd);
            }
            while (cmd == ModelControllerClientProtocol.PARAM_INPUT_STREAM) {
                //Just copy the stream contents for now - remoting will handle this better
                int length = StreamUtils.readInt(inputStream);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (int i = 0 ; i < length ; i++) {
                    int b = inputStream.read();
                    if (b == -1) {
                        throw new IllegalArgumentException("Unexpected end of file");
                    }
                    bout.write(b);
                }
                builder.addInputStream(new ByteArrayInputStream(bout.toByteArray()));

                cmd = inputStream.read();
            }
        }

        protected ControllerTransaction getTransaction() {
            ControllerTransaction result = new ControllerTransaction(txId);
            ControllerTransaction existing = transactions.putIfAbsent(txId, result);
            if (existing != null) {
                result = existing;
            }
            return result;
        }
    }

    private class ExecuteTransactionalSynchronousOperation extends ExecuteTransactionalOperation {
        @Override
        protected final byte getResponseCode() {
            return EXECUTE_TRANSACTIONAL_SYNCHRONOUS_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ControllerTransactionContext tx = getTransaction();
            Operation operation = builder.build();
            ModelNode result = transactionalModelController.execute(operation, tx);
            outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
            result.writeExternal(outputStream);
        }
    }

    private class ExecuteTransactionalAsynchronousOperation extends ExecuteTransactionalOperation {

        final int asynchronousRequestId = getNextAsynchronousRequestId();

        @Override
        protected final byte getResponseCode() {
            return EXECUTE_TRANSACTIONAL_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            final CountDownLatch completeLatch = new CountDownLatch(1);
            final IOExceptionHolder exceptionHolder = new IOExceptionHolder();
            final FailureHolder failureHolder = new FailureHolder();
            final AtomicInteger status = new AtomicInteger(0);

            ControllerTransactionContext tx = getTransaction();
            Operation operation = builder.build();
            OperationResult result = transactionalModelController.execute(operation, new ResultHandler() {
                @Override
                public void handleResultFragment(String[] location, ModelNode fragment) {
                    try {
                        synchronized (outputStream) {
                            outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT);
                            outputStream.write(ModelControllerClientProtocol.PARAM_LOCATION);
                            StreamUtils.writeInt(outputStream, location.length);
                            for (String loc : location) {
                                StreamUtils.writeUTFZBytes(outputStream, loc);
                            }
                            outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
                            fragment.writeExternal(outputStream);
                            outputStream.flush();
                        }
                    } catch (IOException e) {
                        clearAsynchronousOperation(asynchronousRequestId);
                        exceptionHolder.setException(e);
                        completeLatch.countDown();
                    }
                }

                @Override
                public void handleResultComplete() {
                    clearAsynchronousOperation(asynchronousRequestId);
                    if(!status.compareAndSet(0, 1)) {
                        throw new RuntimeException("Result already set");
                    }
                    completeLatch.countDown();
                }

                @Override
                public void handleFailed(final ModelNode failureDescription) {
                    clearAsynchronousOperation(asynchronousRequestId);
                    if(!status.compareAndSet(0, 2)) {
                        throw new RuntimeException("Result already set");
                    }
                    failureHolder.setFailure(failureDescription);
                    completeLatch.countDown();
                }

                @Override
                public void handleCancellation() {
                    clearAsynchronousOperation(asynchronousRequestId);
                    if(!status.compareAndSet(0, 3)) {
                        throw new RuntimeException("Result already set");
                    }
                    completeLatch.countDown();
                }
            }, tx);

            synchronized (outputStream) {
                outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
                ModelNode compensating = result.getCompensatingOperation() != null ? result.getCompensatingOperation() : new ModelNode();
                compensating.writeExternal(outputStream);
                outputStream.flush();
            }

            if (completeLatch.getCount() == 0) {
                //It was handled synchronously or has completed by now
            } else {
                //It was handled asynchronously
                addAsynchronousOperation(asynchronousRequestId, result.getCancellable());
                synchronized (outputStream) {
                    outputStream.write(ModelControllerClientProtocol.PARAM_REQUEST_ID);
                    StreamUtils.writeInt(outputStream, asynchronousRequestId);
                    outputStream.flush();
                }

                while (true) {
                    try {
                        completeLatch.await();
                        break;
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (exceptionHolder.getException() != null) {
                throw exceptionHolder.getException();
            }

            switch (status.get()) {
                case 1: {
                    synchronized (outputStream) {
                        outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE);
                        outputStream.flush();
                    }
                    break;
                }
                case 2: {
                    synchronized (outputStream) {
                        outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FAILED);
                        failureHolder.getFailure().writeExternal(outputStream);
                        outputStream.flush();
                    }
                    break;
                }
                case 3: {
                    synchronized (outputStream) {
                        outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_CANCELLATION);
                        outputStream.flush();
                    }
                    break;
                }
                default: {
                    throw new IOException("Unknown status type " + status.get());
                }
            }
        }
    }

    private class CommitTransactionOperation extends ManagementResponse {

        private ModelNode  txId;

        CommitTransactionOperation() {
            super(getInitiatingHandler());
        }

        @Override
        protected final byte getResponseCode() {
            return TRANSACTION_COMMIT_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, TRANSACTION_ID);
            txId = new ModelNode();
            txId.readExternal(inputStream);
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ControllerTransaction tx = transactions.remove(txId);
            if (tx != null) {
                tx.commit();
            }
        }
    }

    private class RollbackTransactionOperation extends ManagementResponse {

        private ModelNode  txId;

        RollbackTransactionOperation() {
            super(getInitiatingHandler());
        }

        @Override
        protected final byte getResponseCode() {
            return TRANSACTION_ROLLBACK_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, TRANSACTION_ID);
            txId = new ModelNode();
            txId.readExternal(inputStream);
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ControllerTransaction tx = transactions.remove(txId);
            if (tx != null) {
                tx.setRollbackOnly();
                tx.commit();
            }
        }
    }

    private final class FailureHolder {
        ModelNode failure;

        public ModelNode getFailure() {
            return failure;
        }

        public void setFailure(ModelNode failure) {
            this.failure = failure;
        }
    }

    private final class IOExceptionHolder {
        IOException exception;

        public IOException getException() {
            return exception;
        }

        public void setException(IOException exception) {
            this.exception = exception;
        }
    }
}
