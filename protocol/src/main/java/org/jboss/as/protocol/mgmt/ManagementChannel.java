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

import org.jboss.as.protocol.ProtocolChannel;
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
public class ManagementChannel extends ProtocolChannel implements ManagementRequestExecutionCallback {

    private final CountDownLatch completedActiveRequestsLatch = new CountDownLatch(1);

    private final RequestReceiver requestReceiver = new RequestReceiver();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();

    //GuardedBy(this)
    private final Set<Integer> outgoingExecutions = new HashSet<Integer>();

    ManagementChannel(String name, Channel channel) {
        super(name, channel);
    }

    public void setOperationHandler(final ManagementOperationHandler handler) {
        requestReceiver.setOperationHandler(handler);
    }

    @Override
    protected void doHandle(final Channel channel, final MessageInputStream message) {
        final SimpleDataInput input = new SimpleDataInput(Marshalling.createByteInput(message)) {

        };
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
            IoUtils.safeClose(message);
        }
    }

    void executeRequest(ManagementRequest<?> request, ManagementResponseHandler<?> responseHandler) throws IOException {
        responseReceiver.registerResponseHandler(request.getCurrentRequestId(), responseHandler);
        FlushableDataOutputImpl output = FlushableDataOutputImpl.create(this.writeMessage());
        try {
            request.writeRequest(this, output);
        } finally {
            IoUtils.safeClose(output);
        }
    }

    @Override
    public synchronized void registerOutgoingExecution(int executionId) {
        //System.out.println("--- Start outgoing execution " + executionId);
        outgoingExecutions.add(executionId);
    }

    @Override
    public synchronized void completeOutgoingExecution(int executionId) {
        outgoingExecutions.remove(executionId);
        if ((getClosed() || getWritesShutdown()) && outgoingExecutions.size() == 0) {
            completedActiveRequestsLatch.countDown();
        }
        //System.out.println("--- End outgoing execution " + executionId);
    }

    protected void waitUntilClosable() throws InterruptedException {
        boolean wait = false;
        synchronized (this) {
            wait = outgoingExecutions.size() > 0;
        }
        if (wait) {
            completedActiveRequestsLatch.await();
        }
    }

    synchronized FlushableDataOutputImpl writeMessage(int executionId) throws IOException {
        if (getClosed() || getWritesShutdown()) {
            if (!outgoingExecutions.contains(executionId)) {
                throw new IOException("Can't accept new requests in a closed channel");
            }
        }
        return FlushableDataOutputImpl.create(writeMessage());
    }

    private class RequestReceiver {
        private ManagementOperationHandler operationHandler;

        private void handleRequest(final ManagementRequestHeader header, final DataInput input) throws IOException {
            // Work with the lowest protocol version
            ManagementOperationHandler operationHandler = getOperationHandler(header);
            //System.out.println("Handling " + header.getRequestId());
            FlushableDataOutputImpl output;
            try {
                output = writeMessage(header.getExecutionId());
            } catch (IOException e) {
                // AutoGenerated
                throw new IOException("Error handling " + header.getRequestId(), e);
            }
            try {
                //Read request
                expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
                ManagementRequestHandler requestHandler = getRequestHandler(operationHandler, input);
                expectHeader(input, ManagementProtocol.REQUEST_START);
                expectHeader(input, ManagementProtocol.REQUEST_BODY);
                requestHandler.setContext(new ManagementRequestContext(ManagementChannel.this, header));
                requestHandler.readRequest(input);
                expectHeader(input, ManagementProtocol.REQUEST_END);

                //Write response
                writeResponseHeader(header, output);
                output.writeByte(ManagementProtocol.RESPONSE_START);
                output.writeByte(ManagementProtocol.RESPONSE_BODY);
                requestHandler.writeResponse(output);
                output.writeByte(ManagementProtocol.RESPONSE_END);
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof IOException) {
                    throw (IOException)e;
                }
                throw new IOException(e);
            } finally {
                IoUtils.safeClose(output);
            }
        }

        private ManagementOperationHandler getOperationHandler(final ManagementRequestHeader header) throws IOException {
            try {
                if (operationHandler == null) {
                    throw new IOException("No operation handler set");
                }
                return operationHandler;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        private ManagementRequestHandler getRequestHandler(final ManagementOperationHandler operationHandler, final DataInput input) throws IOException {
            try {
                final byte requestHandlerId = input.readByte();
                ManagementRequestHandler requestHandler = operationHandler.getRequestHandler(requestHandlerId);
                if (requestHandler == null) {
                    throw new IOException("No request handler found with id " + requestHandlerId + " in operation handler " + operationHandler);
                }
                return requestHandler;
            } catch (IOException e) {
                e.printStackTrace();//TODO remove
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        private void writeResponseHeader(final ManagementRequestHeader header, DataOutput output) throws IOException {
            final int workingVersion = Math.min(ManagementProtocol.VERSION, header.getVersion());
            try {
                // Now write the response header
                final ManagementResponseHeader responseHeader = new ManagementResponseHeader(workingVersion, header.getRequestId());
                responseHeader.write(output);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to write management response headers", t);
            }
        }

        private synchronized void setOperationHandler(final ManagementOperationHandler operationHandler) {
            this.operationHandler = operationHandler;
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
            expectHeader(input, ManagementProtocol.RESPONSE_START);
            expectHeader(input, ManagementProtocol.RESPONSE_BODY);
            responseHandler.readResponse(input);
            expectHeader(input, ManagementProtocol.RESPONSE_END);
        }

    }
}
