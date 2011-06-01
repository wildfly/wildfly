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

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClientProtocol;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ModelControllerOperationHandlerImpl implements ModelControllerOperationHandler {

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    private final ModelController modelController;

    private final AtomicInteger currentAsynchronousRequestId = new AtomicInteger();

    private final Map<Integer, Cancellable> asynchOperations = Collections.synchronizedMap(new HashMap<Integer, Cancellable>());

    protected ModelControllerOperationHandlerImpl(ModelController modelController) {
        this.modelController = modelController;
    }

    public ManagementRequestHandler getRequestHandler(byte id) {
        switch (id) {
            case ModelControllerClientProtocol.EXECUTE_ASYNCHRONOUS_REQUEST:
                return new ExecuteAsynchronousOperation();
            case ModelControllerClientProtocol.EXECUTE_SYNCHRONOUS_REQUEST:
                return new ExecuteSynchronousOperation();
            case ModelControllerClientProtocol.CANCEL_ASYNCHRONOUS_OPERATION_REQUEST:
                return new CancelAsynchronousOperation();
            default:
                return null;
        }
    }

    protected ModelController getController() {
        return modelController;
    }

    protected int getNextAsynchronousRequestId() {
        return currentAsynchronousRequestId.incrementAndGet();
    }

    protected void addAsynchronousOperation(int id, Cancellable operation) {
        asynchOperations.put(id, operation);
    }

    protected Cancellable getAsynchronousOperation(int id) {
        return asynchOperations.get(id);
    }

    protected void clearAsynchronousOperation(int id) {
        asynchOperations.remove(id);
    }

    protected ModelNode readNode(DataInput in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private abstract class ExecuteOperation extends ManagementRequestHandler {
        OperationBuilder builder;

        ExecuteOperation() {
        }

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
            builder = OperationBuilder.Factory.create(readNode(input));

            byte cmd = input.readByte();
            if (cmd == ModelControllerClientProtocol.PARAM_REQUEST_END) {
                return;
            }
            if (cmd != ModelControllerClientProtocol.PARAM_INPUT_STREAM) {
                throw new IllegalArgumentException("Expected " + ModelControllerClientProtocol.PARAM_INPUT_STREAM + " received " + cmd);
            }
            while (cmd == ModelControllerClientProtocol.PARAM_INPUT_STREAM) {
                //Just copy the stream contents for now - remoting will handle this better
                int length = input.readInt();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (int i = 0 ; i < length ; i++) {
                    int b = input.readByte();
                    if (b == -1) {
                        throw new IllegalArgumentException("Unexpected end of file");
                    }
                    bout.write(b);
                }
                builder.addInputStream(new ByteArrayInputStream(bout.toByteArray()));

                cmd = input.readByte();
            }
            if (cmd != ModelControllerClientProtocol.PARAM_REQUEST_END) {
                throw new IllegalArgumentException("Expected " + ModelControllerClientProtocol.PARAM_REQUEST_END + " received " + cmd);
            }
        }
    }

    private class ExecuteSynchronousOperation extends ExecuteOperation {
//        @Override
//        protected final byte getResponseCode() {
//            return ModelControllerClientProtocol.EXECUTE_SYNCHRONOUS_RESPONSE;
//        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            ModelNode result = modelController.execute(builder.build());
            output.write(ModelControllerClientProtocol.PARAM_OPERATION);
            result.writeExternal(output);
        }
    }

    private class ExecuteAsynchronousOperation extends ExecuteOperation {

        final int asynchronousRequestId = getNextAsynchronousRequestId();

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            final CountDownLatch completeLatch = new CountDownLatch(1);
            final IOExceptionHolder exceptionHolder = new IOExceptionHolder();
            final FailureHolder failureHolder = new FailureHolder();
            final AtomicInteger status = new AtomicInteger(0);

            OperationResult result = modelController.execute(builder.build(), new ResultHandler() {
                @Override
                public void handleResultFragment(String[] location, ModelNode fragment) {
                    try {
                        synchronized (output) {
                            output.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT);
                            output.write(ModelControllerClientProtocol.PARAM_LOCATION);
                            output.writeInt(location.length);
                            for (String loc : location) {
                                output.writeUTF(loc);
                            }
                            output.write(ModelControllerClientProtocol.PARAM_OPERATION);
                            fragment.writeExternal(output);
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
            });

            //Do this blocking operation outside the synch block or our result handler will deadlock
            ModelNode compensating = result.getCompensatingOperation() != null ? result.getCompensatingOperation() : new ModelNode();

            synchronized (output) {
                output.write(ModelControllerClientProtocol.PARAM_OPERATION);
                compensating.writeExternal(output);
            }

            if (completeLatch.getCount() == 0) {
                //It was handled synchronously or has completed by now
            } else {
                //It was handled asynchronously
                addAsynchronousOperation(asynchronousRequestId, result.getCancellable());
                synchronized (output) {
                    output.write(ModelControllerClientProtocol.PARAM_REQUEST_ID);
                    output.writeInt(asynchronousRequestId);
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
                    synchronized (output) {
                        output.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE);
                    }
                    break;
                }
                case 2: {
                    synchronized (output) {
                        output.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FAILED);
                        failureHolder.getFailure().writeExternal(output);
                    }
                    break;
                }
                case 3: {
                    synchronized (output) {
                        output.write(ModelControllerClientProtocol.PARAM_HANDLE_CANCELLATION);
                    }
                    break;
                }
                default: {
                    throw new IOException("Unknown status type " + status.get());
                }
            }
        }
    }

    private class CancelAsynchronousOperation extends ManagementRequestHandler {

        private boolean cancelled;

        @Override
        public void readRequest(DataInput input) throws IOException {

            expectHeader(input, ModelControllerClientProtocol.PARAM_REQUEST_ID);
            int operationId = input.readInt();

            Cancellable operation = getAsynchronousOperation(operationId);
            cancelled = operation!= null && operation.cancel();
        }

        @Override
        public void writeResponse(final FlushableDataOutput output) throws IOException {
            output.writeBoolean(cancelled);
        }
    }

    private final class FailureHolder {
        ModelNode failure;

        public ModelNode getFailure() {
            final ModelNode failure = this.failure;
            if(failure == null) {
                return new ModelNode();
            }
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
