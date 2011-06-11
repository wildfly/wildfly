/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.protocol.mgmt;

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ManagementChannel extends ProtocolChannel implements ManagementBatchIdManager {

    private final CountDownLatch completedActiveRequestsLatch = new CountDownLatch(1);

    private final RequestReceiver requestReceiver = new RequestReceiver();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();

    private volatile ManagementBatchIdManager batchIdManager;

    //GuardedBy(this)
    private final Set<Integer> outgoingBatches = new HashSet<Integer>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    ManagementChannel(String name, Channel channel) {
        super(name, channel);
    }

    public void setOperationHandler(final ManagementOperationHandler handler) {
        requestReceiver.setOperationHandler(handler);
    }

    public void setBatchIdManager(ManagementBatchIdManager batchIdManager) {
        this.batchIdManager = batchIdManager;
    }

    @Override
    protected void doHandle(final Channel channel, final MessageInputStream message) {
        final SimpleDataInput input = new SimpleDataInput(Marshalling.createByteInput(message));
        try {
            ManagementProtocolHeader header = ManagementProtocolHeader.parse(input);
            if (header.isRequest()) {
                requestReceiver.handleRequest((ManagementRequestHeader)header, input);
            } else {
                responseReceiver.handleResponse((ManagementResponseHeader)header, input);
            }
        } catch (IOException e) {
            //TODO handle properly
            e.printStackTrace();
        } finally {
            IoUtils.safeClose(input);
            IoUtils.safeClose(message);
        }
    }

    void executeRequest(ManagementRequest<?> request, ManagementResponseHandler<?> responseHandler) throws IOException {
        responseReceiver.registerResponseHandler(request.getCurrentRequestId(), responseHandler);
        FlushableDataOutputImpl output = FlushableDataOutputImpl.create(this.writeMessage());
        try {
            final ManagementRequestHeader managementRequestHeader = new ManagementRequestHeader(ManagementProtocol.VERSION, request.getCurrentRequestId(), request.getBatchId(), request.getRequestCode());
            managementRequestHeader.write(output);

            request.writeRequest(this, output);
        } finally {
            IoUtils.safeClose(output);
        }
    }


    @Override
    public synchronized int createBatchId() {
        ManagementBatchIdManager batchIdManager = this.batchIdManager;
        if (batchIdManager == null) {
            batchIdManager = new RemoteBatchIdManager();
        }
        final int batchId = batchIdManager.createBatchId();
        //System.out.println("--- Start outgoing batch " + batchId);
        outgoingBatches.add(batchId);
        return batchId;
    }

    @Override
    public synchronized void freeBatchId(int id) {
        ManagementBatchIdManager batchIdManager = this.batchIdManager;
        if (batchIdManager == null) {
            batchIdManager = new RemoteBatchIdManager();
        }
        batchIdManager.freeBatchId(id);
        outgoingBatches.remove(id);
        if ((getClosed() || getWritesShutdown()) && outgoingBatches.size() == 0) {
            completedActiveRequestsLatch.countDown();
        }
        //System.out.println("--- End outgoing execution " + executionId);
    }

    protected void waitUntilClosable() throws InterruptedException {
        boolean wait = false;
        synchronized (this) {
            wait = outgoingBatches.size() > 0;
        }
        if (wait) {
            completedActiveRequestsLatch.await();
        }
    }

    synchronized FlushableDataOutputImpl writeMessage(int executionId) throws IOException {
        if (getClosed() || getWritesShutdown()) {
            if (!outgoingBatches.contains(executionId)) {
                throw new IOException("Can't accept new requests in a closed channel");
            }
        }
        return FlushableDataOutputImpl.create(writeMessage());
    }

    void throwFormattedException(Exception e) throws IOException {
        //e.printStackTrace();
        if (e instanceof IOException) {
            throw (IOException)e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
        }
        throw new IOException(e);

    }

    private class RequestReceiver {
        private volatile ManagementOperationHandler operationHandler;

        private void handleRequest(final ManagementRequestHeader header, final DataInput input) throws IOException {
            final FlushableDataOutputImpl output = writeMessage(header.getBatchId());

            Exception error = null;
            try {
                final ManagementRequestHandler requestHandler;
                try {
                    //Read request
                    requestHandler = getRequestHandler(header);
                    requestHandler.setContext(new ManagementRequestContext(ManagementChannel.this, header));
                    requestHandler.readRequest(input);
                    expectHeader(input, ManagementProtocol.REQUEST_END);
                } catch (Exception e) {
                    error = e;
                    throw e;
                } finally {
                    writeResponseHeader(header, output, error);
                    if (error != null) {
                        output.writeByte(ManagementProtocol.RESPONSE_END);
                    }
                }
                requestHandler.writeResponse(output);
                output.writeByte(ManagementProtocol.RESPONSE_END);
            } catch (Exception e) {
                formatException(e);
            } finally {
                IoUtils.safeClose(output);
            }
        }

        private ManagementRequestHandler getRequestHandler(final ManagementRequestHeader header) throws IOException {
            try {
                switch (header.getOperationId()) {
                case ManagementProtocol.CREATE_BATCH_ID_REQUEST:
                    if (batchIdManager == null) {
                        throw new IOException("No local batch id manager to service remote CREATE_BATCH_ID_REQUEST");
                    }
                    return new CreateBatchIdRequestHandler();
                case ManagementProtocol.FREE_BATCH_ID_REQUEST:
                    if (batchIdManager == null) {
                        throw new IOException("No local batch id manager to service remote FREE_BATCH_ID_REQUEST");
                    }
                    return new FreeBatchIdRequestHandler();
                }

                ManagementOperationHandler operationHandler = this.operationHandler;
                if (operationHandler == null) {
                    throw new IOException("No operation handler set");
                }
                ManagementRequestHandler requestHandler = operationHandler.getRequestHandler(header.getOperationId());
                if (requestHandler == null) {
                    throw new IOException("No request handler found with id " + header.getOperationId() + " in operation handler " + operationHandler);
                }
                return requestHandler;
            } catch (IOException e) {
                e.printStackTrace();//TODO remove
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        private void writeResponseHeader(final ManagementRequestHeader header, DataOutput output, Exception exception) throws IOException {
            final int workingVersion = Math.min(ManagementProtocol.VERSION, header.getVersion());
            try {
                // Now write the response header
                final ManagementResponseHeader responseHeader = new ManagementResponseHeader(workingVersion, header.getRequestId(), formatException(exception));
                responseHeader.write(output);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to write management response headers", t);
            }
        }

        private void setOperationHandler(final ManagementOperationHandler operationHandler) {
            this.operationHandler = operationHandler;
        }

        private String formatException(Exception exception) {
            if (exception == null) {
                return null;
            }
            return exception.getMessage();
        }
    }

    private class ResponseReceiver {

        private final Map<Integer, ManagementResponseHandler<?>> responseHandlers = Collections.synchronizedMap(new HashMap<Integer, ManagementResponseHandler<?>>());

        private void registerResponseHandler(final int requestId, final ManagementResponseHandler<?> handler) throws IOException {
            if (responseHandlers.put(requestId, handler) != null) {
                throw new IOException("Response handler already registered for request");
            }
        }

        private void handleResponse(ManagementResponseHeader header, DataInput input) throws IOException {
            ManagementResponseHandler<?> responseHandler = responseHandlers.get(header.getResponseId());
            if (responseHandler == null) {
                throw new IOException("No response handler for request " + header.getResponseId());
            }
            responseHandler.setContext(new ManagementResponseContext(ManagementChannel.this, header));

            try {
                responseHandler.readResponse(input);
                expectHeader(input, ManagementProtocol.RESPONSE_END);
            } catch (Exception e) {
                throwFormattedException(e);
            }
        }
    }

    private class RemoteBatchIdManager implements ManagementBatchIdManager {

        @Override
        public int createBatchId() {
            try {
                return new ManagementRequest<Integer>(){

                    @Override
                    protected byte getRequestCode() {
                        return ManagementProtocol.CREATE_BATCH_ID_REQUEST;
                    }

                    @Override
                    protected Integer readResponse(DataInput input) throws IOException {
                        ProtocolUtils.expectHeader(input, ManagementProtocol.PARAM_BATCH_ID);
                        return input.readInt();
                    }
                }.executeForResult(executor, ManagementClientChannelStrategy.create(ManagementChannel.this));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void freeBatchId(final int id) {
            try {
                new ManagementRequest<Void>(id){

                    @Override
                    protected byte getRequestCode() {
                        return ManagementProtocol.FREE_BATCH_ID_REQUEST;
                    }

                    @Override
                    protected Void readResponse(DataInput input) throws IOException {
                        return null;
                    }

                }.executeForResult(executor, ManagementClientChannelStrategy.create(ManagementChannel.this));
            } catch (Exception ignore) {
            }
        }
    }

    private class CreateBatchIdRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(DataInput input) throws IOException {
        }

        @Override
        protected void writeResponse(FlushableDataOutput output) throws IOException {
            output.write(ManagementProtocol.PARAM_BATCH_ID);
            output.writeInt(batchIdManager.createBatchId());
        }
    }


    private class FreeBatchIdRequestHandler extends ManagementRequestHandler {

        @Override
        protected void readRequest(DataInput input) throws IOException {
            batchIdManager.freeBatchId(getContext().getHeader().getBatchId());
        }

        @Override
        protected void writeResponse(FlushableDataOutput output) throws IOException {
        }
    }
}
