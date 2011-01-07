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
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import static org.jboss.as.protocol.StreamUtils.readByte;
import static org.jboss.as.protocol.StreamUtils.safeFinish;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;

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
import org.jboss.as.protocol.ProtocolUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
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

    private static MarshallingConfiguration CONFIG;

    static {
        CONFIG = new MarshallingConfiguration();
        final ClassLoader cl = ModelControllerOperationHandler.class.getClassLoader();
        CONFIG.setClassResolver(new SimpleClassResolver(cl));
    }

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName OPERATION_HANDLER_NAME_SUFFIX = ServiceName.of("operation", "handler");

    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();

    private volatile ModelController modelController;

    private final AtomicInteger currentAsynchronousRequestId = new AtomicInteger();

    private final Map<Integer, Cancellable> asynchOperations = Collections.synchronizedMap(new HashMap<Integer, Cancellable>());

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(CONFIG);
    }

    public InjectedValue<ModelController> getModelControllerValue() {
        return modelControllerValue;
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        modelController = modelControllerValue.getValue();
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
    }

    /** {@inheritDoc} */
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

    private abstract class ExecuteOperation extends ManagementResponse {
        ModelNode operation;

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(inputStream));
            try {
                expectHeader(unmarshaller, ModelControllerClientProtocol.PARAM_OPERATION);
                operation = unmarshal(unmarshaller, ModelNode.class);
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }
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
                throw new IOException(e);
            }
            final Marshaller marshaller = getMarshaller();
            try {
                marshaller.start(createByteOutput(outputStream));
                marshaller.writeByte(ModelControllerClientProtocol.PARAM_OPERATION);
                marshaller.writeObject(result);
                marshaller.finish();
            }
            finally {
                safeFinish(marshaller);
            }
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

            final Marshaller marshaller = getMarshaller();
            try {
                final ByteOutput output = createByteOutput(outputStream);
                marshaller.start(output);

                final CountDownLatch completeLatch = new CountDownLatch(1);
                final IOExceptionHolder holder = new IOExceptionHolder();
                Cancellable result = modelController.execute(operation, new ResultHandler() {
                    @Override
                    public void handleResultFragment(String[] location, ModelNode result) {
                        try {
                            synchronized (marshaller) {
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT);
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_LOCATION);
                                marshaller.writeObject(location);
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_OPERATION);
                                marshaller.writeObject(result);
                                marshaller.flush();
                            }
                        } catch (IOException e) {
                            handleIOException(e);
                        }
                    }

                    @Override
                    public void handleResultComplete(ModelNode compensatingOperation) {
                        try {
                            asynchOperations.remove(asynchronousRequestId);
                            synchronized (marshaller) {
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE);
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_OPERATION);
                                marshaller.writeObject(compensatingOperation);
                                marshaller.flush();
                            }
                            completeLatch.countDown();
                        } catch (IOException e) {
                            handleIOException(e);
                        }
                    }

                    public void handleFailed(final ModelNode failureDescription) {
                        try {
                            asynchOperations.remove(asynchronousRequestId);
                            synchronized (marshaller) {
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FAILED);
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_OPERATION);
                                marshaller.writeObject(failureDescription);
                                marshaller.flush();
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
                            synchronized (marshaller) {
                                marshaller.writeByte(ModelControllerClientProtocol.PARAM_HANDLE_CANCELLATION);
                                marshaller.flush();
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

//                    synchronized (marshaller) {
//                        marshaller.writeByte(ModelControllerClientProtocol.RTN_REQUEST_ID);
//                        marshaller.writeInt(-1);
//                    }
                } else {
                    //It was handled asynchronously
                    asynchOperations.put(Integer.valueOf(asynchronousRequestId), result);
                    synchronized (marshaller) {
                        marshaller.writeByte(ModelControllerClientProtocol.PARAM_REQUEST_ID);
                        marshaller.writeInt(asynchronousRequestId);
                        marshaller.flush();
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

                marshaller.finish();
            }
            finally {
                safeFinish(marshaller);
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
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(inputStream));
            int operationId = 0;
            try {
                expectHeader(unmarshaller, ModelControllerClientProtocol.PARAM_REQUEST_ID);
                operationId = unmarshaller.readInt();
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }

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
}
