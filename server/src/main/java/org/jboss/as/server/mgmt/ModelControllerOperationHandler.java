/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.server.mgmt;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.StreamUtils.readByte;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClientProtocol;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ModelControllerOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler, Service<ManagementOperationHandler> {

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName OPERATION_HANDLER_NAME_SUFFIX = ServiceName.of("operation", "handler");

    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();

    private volatile ModelController modelController;

    private final AtomicInteger currentAsynchronousRequestId = new AtomicInteger();

    private final Map<Integer, Cancellable> asynchOperations = Collections.synchronizedMap(new HashMap<Integer, Cancellable>());

    public InjectedValue<ModelController> getModelControllerValue() {
        return modelControllerValue;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        modelController = modelControllerValue.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
    }

    /** {@inheritDoc} */
    @Override
    public ModelControllerOperationHandler getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(Connection connection, InputStream input) throws IOException {
        expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
        final byte commandCode = readByte(input);

        final AbstractMessageHandler operation = operationFor(commandCode);
        if (operation == null) {
            throw new IOException("Invalid command code " + commandCode + " received from standalone client");
        }
        log.debugf("Received operation [%s]", operation);

        operation.handle(connection, input);
    }

    /** {@inheritDoc} */
    @Override
    public byte getIdentifier() {
        return ModelControllerClientProtocol.HANDLER_ID;
    }

    private AbstractMessageHandler operationFor(final byte commandByte) {
        switch (commandByte) {
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

    private ModelNode readNode(InputStream in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private abstract class ExecuteOperation extends ManagementResponse {
        ModelNode operation;

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, ModelControllerClientProtocol.PARAM_OPERATION);
            operation = readNode(inputStream);
        }
    }

    private class ExecuteSynchronousOperation extends ExecuteOperation {
        @Override
        protected final byte getResponseCode() {
            return ModelControllerClientProtocol.EXECUTE_SYNCHRONOUS_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ModelNode result = null;
            try {
                result = modelController.execute(operation);
            } catch (OperationFailedException e) {
                result = new ModelNode().set(createErrorResult(e));
            }
            outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
            result.writeExternal(outputStream);
        }
    }

    private class ExecuteAsynchronousOperation extends ExecuteOperation {

        final int asynchronousRequestId = currentAsynchronousRequestId.incrementAndGet();

        @Override
        protected final byte getResponseCode() {
            return ModelControllerClientProtocol.EXECUTE_ASYNCHRONOUS_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            final CountDownLatch completeLatch = new CountDownLatch(1);
            final IOExceptionHolder holder = new IOExceptionHolder();
            Cancellable result = modelController.execute(operation, new ResultHandler() {
                @Override
                public void handleResultFragment(String[] location, ModelNode result) {
                    try {
                        synchronized (outputStream) {
                            outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT);
                            outputStream.write(ModelControllerClientProtocol.PARAM_LOCATION);
                            StreamUtils.writeInt(outputStream, location.length);
                            for (String loc : location) {
                                StreamUtils.writeUTFZBytes(outputStream, loc);
                            }
                            outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
                            result.writeExternal(outputStream);
                            outputStream.flush();
                        }
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }

                @Override
                public void handleResultComplete(ModelNode compensatingOperation) {
                    try {
                        asynchOperations.remove(asynchronousRequestId);
                        synchronized (outputStream) {
                            outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE);
                            outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
                            compensatingOperation.writeExternal(outputStream);
                            //outputStream.flush();
                        }
                        completeLatch.countDown();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }

                @Override
                public void handleFailed(final ModelNode failureDescription) {
                    try {
                        asynchOperations.remove(asynchronousRequestId);
                        synchronized (outputStream) {
                            outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FAILED);
                            outputStream.write(ModelControllerClientProtocol.PARAM_OPERATION);
                            failureDescription.writeExternal(outputStream);
                            //outputStream.flush();
                        }
                        completeLatch.countDown();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }

                @Override
                public void handleCancellation() {
                    try {
                        asynchOperations.remove(asynchronousRequestId);
                        synchronized (outputStream) {
                            outputStream.write(ModelControllerClientProtocol.PARAM_HANDLE_CANCELLATION);
                            //outputStream.flush();
                        }
                        completeLatch.countDown();
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }

                private void handleIOException(IOException e) {
                    asynchOperations.remove(asynchronousRequestId);
                    holder.setException(e);
                    completeLatch.countDown();
                }
            });

            if (completeLatch.getCount() == 0) {
                //It was handled synchronously or has completed by now
            } else {
                //It was handled asynchronously
                asynchOperations.put(Integer.valueOf(asynchronousRequestId), result);
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

                if (holder.getException() != null) {
                    throw holder.getException();
                }
            }
        }
    }

    private class CancelAsynchronousOperation extends ManagementResponse {
        @Override
        protected final byte getResponseCode() {
            return ModelControllerClientProtocol.CANCEL_ASYNCHRONOUS_OPERATION_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {

            expectHeader(inputStream, ModelControllerClientProtocol.PARAM_REQUEST_ID);
            int operationId = StreamUtils.readInt(inputStream);

            Cancellable operation = asynchOperations.get(Integer.valueOf(operationId));
            if (operation != null) {
                operation.cancel();
            }
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

    static ModelNode createErrorResult(OperationFailedException e) {
        return e.getFailureDescription();
    }
}
