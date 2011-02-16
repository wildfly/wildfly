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

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

/**
*
* @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
* @version $Revision: 1.1 $
*/
abstract class AbstractModelControllerClient implements ModelControllerClient {
    final Type type;
    final ThreadFactory threadFactory = Executors.defaultThreadFactory();
    final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

    public AbstractModelControllerClient(final Type type) {
        this.type = type;
    }

    @Override
    public OperationResult execute(final ModelNode operation, final ResultHandler handler) {
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

                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute operation ", e);
                }
            }
        });
        return new OperationResult() {
            public Cancellable getCancellable() {
                return result;
            }

            public ModelNode getCompensatingOperation() {
                return null;
            }
        };
    }

    @Override
    public ModelNode execute(final ModelNode operation) throws CancellationException, IOException {
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
            return type.getHandlerId();
        }
    }

    private class ExecuteSynchronousRequest extends ModelControllerRequest<ModelNode> {

        private final ModelNode operation;

        ExecuteSynchronousRequest(ModelNode operation) {
            this.operation = operation;
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
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(ModelControllerClientProtocol.PARAM_OPERATION);
            operation.writeExternal(output);
        }


        /** {@inheritDoc} */
        @Override
        protected ModelNode receiveResponse(InputStream input) throws IOException {
            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
            return readNode(input);
        }
    }

    private class ExecuteAsynchronousRequest extends ModelControllerRequest<Void> {

        private final AsynchronousOperation result;
        private final ModelNode operation;
        private final ResultHandler handler;

        ExecuteAsynchronousRequest(AsynchronousOperation result, ModelNode operation, ResultHandler handler) {
            this.result = result;
            this.operation = operation;
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
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(ModelControllerClientProtocol.PARAM_OPERATION);
            operation.writeExternal(output);
        }


        /** {@inheritDoc} */
        @Override
        protected Void receiveResponse(InputStream input) throws IOException {
            try {
                LOOP:
                while (true) {
                    int command = input.read();
                    switch (command) {
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
                            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
                            ModelNode node = readNode(input);
                            // FIXME need some sort of translation
                            handler.handleException(new RuntimeException(node.toString()));
                            break LOOP;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE:{
                            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
                            ModelNode node = readNode(input); // TODO: Where does this go
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
                handler.handleException(e);
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
        SimpleFuture<Integer> asynchronousId = new SimpleFuture<Integer>();

        @Override
        public boolean cancel() throws IOException {
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
