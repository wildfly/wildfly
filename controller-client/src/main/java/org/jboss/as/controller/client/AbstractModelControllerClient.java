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
package org.jboss.as.controller.client;

import java.security.AccessController;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.JBossThreadFactory;

/**
 * Abstract superclass for {@link ModelControllerClient} implementations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class AbstractModelControllerClient implements ModelControllerClient {
    final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("ModelControllerClient-thread"), Boolean.FALSE, Thread.NORM_PRIORITY, "%G - %t", null, null, AccessController.getContext());
    final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

    public AbstractModelControllerClient() {
    }

    @Override
    public OperationResult execute(ModelNode operation, ResultHandler handler) {
        return execute(OperationBuilder.Factory.create(operation).build(), handler);
    }

    @Override
    public ModelNode execute(ModelNode operation) throws CancellationException, IOException {
        return execute(OperationBuilder.Factory.create(operation).build());
    }

    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler) {
        if (operation == null) {
            throw new IllegalArgumentException("Null operation");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Null handler");
        }

        final AsynchronousOperation result = new AsynchronousOperation();
        executorService.execute (new Runnable() {
            @Override
            public void run() {
                try {
                    Future<Void> f = new ExecuteAsynchronousRequest(result, operation, handler).execute(getConnectionStrategy());

                    while (true) {
                        try {
                            //Avoid this thread hanging forever if the client gets shut down
                            f.get(500, TimeUnit.MILLISECONDS);
                            break;
                        } catch (TimeoutException e) {
                            if (executorService.isShutdown()) {
                                break;
                            }
                        }
                    }

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    handler.handleFailed(new ModelNode().set("Failed to execute operation: " + cause.toString()));
                } catch (Exception e) {
                    handler.handleFailed(new ModelNode().set("Failed to execute operation: " + e.toString()));
                }
            }
        });
        return new OperationResult() {
            @Override
            public Cancellable getCancellable() {
                return result;
            }

            @Override
            public ModelNode getCompensatingOperation() {
                return result.getCompensatingOperation();
            }
        };
    }

    @Override
    public ModelNode execute(final Operation operation) throws CancellationException, IOException {
        if (operation == null) {
            throw new IllegalArgumentException("Null operation");
        }
        try {
            return new ExecuteSynchronousRequest(operation).executeForResult(getConnectionStrategy());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw new IOException(e);
            }
            if (e.getCause() instanceof CancellationException) {
                throw new CancellationException(e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to execute operation ", e);
        } catch (IOException e){
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute operation ", e);
        }
    }

    @Override
    public void close() throws IOException {
        executorService.shutdown();
    }

    abstract ManagementRequestConnectionStrategy getConnectionStrategy();

    private ModelNode readNode(InputStream in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private abstract class ModelControllerRequest<T> extends ManagementRequest<T>{
        @Override
        protected byte getHandlerId() {
            return ModelControllerClientProtocol.HANDLER_ID;
        }
    }

    private abstract class ExecuteRequest<T> extends ModelControllerRequest<T> {
        private final Operation operation;

        public ExecuteRequest(Operation executionContext) {
            this.operation = executionContext;
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(ModelControllerClientProtocol.PARAM_OPERATION);
            operation.getOperation().writeExternal(output);
            List<InputStream> streams = operation.getInputStreams();
            for (InputStream in : streams) {
                output.write(ModelControllerClientProtocol.PARAM_INPUT_STREAM);
                //Just copy the stream contents for now - remoting will handle this better
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        bout.write(buffer, 0, read);
                    }
                } finally {
                    StreamUtils.safeClose(in);
                }

                byte[] bytes = bout.toByteArray();
                StreamUtils.writeInt(output, bytes.length);
                try {
                    for (byte b : bytes) {
                        output.write(b);
                    }
                } finally {
                    StreamUtils.safeClose(in);
                }
            }
            output.write(ModelControllerClientProtocol.PARAM_REQUEST_END);
        }
    }

    private class ExecuteSynchronousRequest extends ExecuteRequest<ModelNode> {

        ExecuteSynchronousRequest(Operation operation) {
            super(operation);
        }

        @Override
        protected byte getRequestCode() {
            return ModelControllerClientProtocol.EXECUTE_SYNCHRONOUS_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return ModelControllerClientProtocol.EXECUTE_SYNCHRONOUS_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected ModelNode receiveResponse(InputStream input) throws IOException {
            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
            return readNode(input);
        }
    }

    private class ExecuteAsynchronousRequest extends ExecuteRequest<Void> {

        private final AsynchronousOperation result;
        private final ResultHandler handler;

        ExecuteAsynchronousRequest(final AsynchronousOperation result, final Operation operation, final ResultHandler handler) {
            super(operation);
            this.result = result;
            this.handler = handler;
        }

        @Override
        protected byte getRequestCode() {
            return ModelControllerClientProtocol.EXECUTE_ASYNCHRONOUS_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return ModelControllerClientProtocol.EXECUTE_ASYNCHRONOUS_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected Void receiveResponse(InputStream input) throws IOException {
            try {
                LOOP:
                while (true) {
                    int command = input.read();
                    switch (command) {
                        case ModelControllerClientProtocol.PARAM_OPERATION : {
                            result.setCompensatingOperation(readNode(input));
                            break;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT:{
                            expectHeader(input, ModelControllerClientProtocol.PARAM_LOCATION);
                            int length = StreamUtils.readInt(input);
                            String[] location = new String[length];
                            for (int i = 0 ; i < length ; i++) {
                                location[i] = StreamUtils.readUTFZBytes(input);
                            }
                            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
                            ModelNode node = readNode(input);
                            handler.handleResultFragment(location, node);
                            break;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_CANCELLATION:{
                            handler.handleCancellation();
                            break LOOP;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FAILED:{
                            ModelNode node = readNode(input);
                            handler.handleFailed(node);
                            break LOOP;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE:{
                            handler.handleResultComplete();
                            break LOOP;
                        }
                        case ModelControllerClientProtocol.PARAM_REQUEST_ID:{
                            result.setAsynchronousId(StreamUtils.readInt(input));
                            break;
                        }
                        default:{
                            throw new IllegalStateException("Unknown response code " + command);
                        }
                    }
                }
            } catch (Exception e) {
                handler.handleFailed(new ModelNode().set(e.toString()));
            }
            return null;
        }
    }

    private class CancelAsynchronousOperationRequest extends ModelControllerRequest<Boolean> {

        private final int asynchronousId;

        CancelAsynchronousOperationRequest(int asynchronousId) {
            this.asynchronousId = asynchronousId;
        }

        @Override
        protected byte getRequestCode() {
            return ModelControllerClientProtocol.CANCEL_ASYNCHRONOUS_OPERATION_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return ModelControllerClientProtocol.CANCEL_ASYNCHRONOUS_OPERATION_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(ModelControllerClientProtocol.PARAM_REQUEST_ID);
            StreamUtils.writeInt(output, asynchronousId);
        }


        /** {@inheritDoc} */
        @Override
        protected Boolean receiveResponse(InputStream input) throws IOException {
            return StreamUtils.readBoolean(input);
        }
    }



    private class AsynchronousOperation implements Cancellable {

        private SimpleFuture<ModelNode> compensatingOperation = new SimpleFuture<ModelNode>();
        // GuardedBy compensatingOperation
        private boolean compensatingOpSet = false;
        private SimpleFuture<Integer> asynchronousId = new SimpleFuture<Integer>();

        @Override
        public boolean cancel() throws IOException {
            setCompensatingOperation(new ModelNode());
            try {
                int i = asynchronousId.get().intValue();
                if (i >= 0) {
                    return new CancelAsynchronousOperationRequest(i).executeForResult(getConnectionStrategy());
                }
                else return false;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Could not cancel request ", e);
            }
        }

        void setAsynchronousId(int i) {
            asynchronousId.set(Integer.valueOf(i));
        }

        void setCompensatingOperation(ModelNode compensatingOp) {
            synchronized (compensatingOperation) {
                if (!compensatingOpSet) {
                    compensatingOperation.set(compensatingOp);
                    compensatingOpSet = true;
                }
            }
        }

        ModelNode getCompensatingOperation() {
            try {
                return compensatingOperation.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Could not obtain compensating operation", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Could not obtain compensating operation", e);
            }
        }
    }

    private static class SimpleFuture<V> implements Future<V> {

        private V value;
        private volatile boolean done;
        private final Lock lock = new ReentrantLock();
        private final Condition hasValue = lock.newCondition();

        /**
         * Always returns <code>false</code>
         *
         * @return <code>false</code>
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {

            lock.lock();
            try {
                while (!done) {
                    hasValue.await();
                }
                return value;
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            long deadline = unit.toMillis(timeout) + System.currentTimeMillis();
            lock.lock();
            try {
                while (!done) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        throw new TimeoutException();
                    }
                    hasValue.await(remaining, TimeUnit.MILLISECONDS);
                }
                return value;
            }
            finally {
                lock.unlock();
            }
        }

        /**
         * Always returns <code>false</code>
         *
         * @return <code>false</code>
         */
        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        public void set(V value) {
            lock.lock();
            try {
                this.value = value;
                done = true;
                hasValue.signalAll();
            }
            finally {
                lock.unlock();
            }
        }
    }
}
