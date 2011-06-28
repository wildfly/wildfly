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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ManagementChannel extends ProtocolChannel {

    private final ManagementChannelPinger pinger = ManagementChannelPinger.getInstance();
    private final RequestReceiver requestReceiver = new RequestReceiver();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();
    private final AtomicBoolean byeByeSent = new AtomicBoolean();
    private volatile long lastResponseReceived;
    private AtomicBoolean awaitingPong = new AtomicBoolean();

    ManagementChannel(String name, Channel channel) {
        super(name, channel);
        pinger.addChannel(this);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        sendByeBye();
        super.close();
    }

    /**
     * {@inheritDoc}
     */
    public void writeShutdown() throws IOException {
        sendByeBye();
        super.close();
    }

    public void sendByeBye() throws IOException {
        if(!byeByeSent.compareAndSet(false, true)) {
            return;
        }
        log.tracef("Closing %s by sending bye bye", this);
        pinger.removeChannel(this);
        ManagementByeByeHeader byeByeHeader = new ManagementByeByeHeader(ManagementProtocol.VERSION);

        try {
            SimpleDataOutput out = new SimpleDataOutput(Marshalling.createByteOutput(writeMessage()));
            try {
                byeByeHeader.write(out);
            } catch (IOException ingore) {
            }finally {
                IoUtils.safeClose(out);
            }
        } finally {
            log.tracef("Invoking close on %s", this);
            super.close();
        }
    }

    public void setOperationHandler(final ManagementOperationHandler handler) {
        requestReceiver.setOperationHandler(handler);
    }

    @Override
    protected void doHandle(final MessageInputStream message) {
        log.tracef("%s handling incoming data", this);
        final SimpleDataInput input = new SimpleDataInput(Marshalling.createByteInput(message));
        Exception error = null;
        ManagementRequestHeader requestHeader = null;
        ManagementRequestHandler requestHandler = null;
        boolean wasPing = false;
        try {
            ManagementProtocolHeader header;
            header = ManagementProtocolHeader.parse(input);

            switch (header.getType()) {
            case ManagementProtocol.TYPE_REQUEST:
                requestHeader = (ManagementRequestHeader)header;
                requestHandler = requestReceiver.readRequest(requestHeader, input);
                break;
            case ManagementProtocol.TYPE_RESPONSE:
                gotIncomingResponse();
                responseReceiver.handleResponse((ManagementResponseHeader)header, input);
                break;
            case ManagementProtocol.TYPE_BYE_BYE:
                log.tracef("Received bye bye on %s, closing", this);
                close();
                break;
            case ManagementProtocol.TYPE_PING:
                wasPing = true;
                log.tracef("Received ping on %s", this);
                break;
            case ManagementProtocol.TYPE_PONG:
                log.tracef("Received pong on %s", this);
                gotIncomingResponse();
                break;
            }
        } catch (Exception e) {
            error = e;
            log.tracef(e, "%s error handling incoming data", this);
        } finally {
            log.tracef("%s done handling incoming data", this);
            try {
                //Consume the rest of the output if any
                while (input.read() != -1) {
                }

            } catch (IOException ignore) {
            }
            IoUtils.safeClose(input);
            IoUtils.safeClose(message);
        }

        if (requestHeader != null) {
            if (error == null) {
                try {
                    requestReceiver.processRequest(requestHeader, requestHandler);
                } catch (Exception e) {
                    error = e;
                }
            }

            if (error != null) {
                log.tracef(error, "Error processing request %s", this);
                //TODO temporary debug stack
                error.printStackTrace();
            }
            requestReceiver.writeResponse(requestHeader, requestHandler, error);
        } else if (wasPing) {
            log.tracef("Sending pong on %s", this);
            ManagementPongHeader pongHeader = new ManagementPongHeader(ManagementProtocol.VERSION);
            sendHeaderAndCloseOnError(pongHeader);
        }
    }

    private void gotIncomingResponse() {
        log.tracef("Resetting ping/response status on %s", this);
        lastResponseReceived = System.currentTimeMillis();
        awaitingPong.set(false);
    }

    void executeRequest(ManagementRequest<?> request, ManagementResponseHandler<?> responseHandler) throws IOException {
        addCloseHandler(request, responseHandler);
        responseReceiver.registerResponseHandler(request.getCurrentRequestId(), responseHandler);
        final FlushableDataOutputImpl output = FlushableDataOutputImpl.create(this.writeMessage());
        try {
            final ManagementRequestHeader managementRequestHeader = new ManagementRequestHeader(ManagementProtocol.VERSION, request.getCurrentRequestId(), request.getBatchId(), request.getRequestCode());
            managementRequestHeader.write(output);

            request.writeRequest(this, output);
        } catch (Exception e) {
            responseHandler.removeCloseHandler();
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            if (e instanceof IOException) throw (IOException)e;
            throw new IOException(e);
        } finally {
            IoUtils.safeClose(output);
        }
    }

    private void addCloseHandler(ManagementRequest<?> request, ManagementResponseHandler<?> responseHandler) {
        final CloseHandler<Channel> closeHandler = request.getRequestCloseHandler();
        if (closeHandler != null) {
            final Key closeKey = addCloseHandler(closeHandler);
            responseHandler.setCloseKey(closeKey);
        }
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

    void ping(long timeOut) {
        if (awaitingPong.get()) {
            if (System.currentTimeMillis() - lastResponseReceived > timeOut) {
                try {
                    //We received no ping within the timeout
                    log.tracef("Closing %s did not receive any pong within %dms", this, timeOut);
                    close();
                } catch (IOException ignore) {
                }
            }
        } else {
            log.tracef("No data received recently on %s, pinging to determine if the other end is alive", this);
            awaitingPong.set(true);
            ManagementPingHeader pingHeader = new ManagementPingHeader(ManagementProtocol.VERSION);
            sendHeaderAndCloseOnError(pingHeader);
        }
    }

    private void sendHeaderAndCloseOnError(ManagementProtocolHeader header) {
        boolean ok = false;
        try {
            SimpleDataOutput out = new SimpleDataOutput(Marshalling.createByteOutput(writeMessage()));
            try {
                header.write(out);
                ok = true;
            }finally {
                IoUtils.safeClose(out);
            }
        } catch (IOException ingore) {
        } finally {
            if (!ok) {
                log.tracef("Error sending 0x%X on %s, closing channel", header.getType(), this);
                IoUtils.safeClose(this);
            }
        }
    }

    private class RequestReceiver {
        private volatile ManagementOperationHandler operationHandler;

        private ManagementRequestHandler readRequest(final ManagementRequestHeader header, final DataInput input) throws IOException {
            log.tracef("%s reading request %d(%d)", ManagementChannel.this, header.getBatchId(), header.getRequestId());
            Exception error = null;
            try {
                final ManagementRequestHandler requestHandler;
                //Read request
                requestHandler = getRequestHandler(header);
                requestHandler.setContextInfo(ManagementChannel.this, header);
                requestHandler.readRequest(input);
                expectHeader(input, ManagementProtocol.REQUEST_END);
                return requestHandler;
            } finally {
                if (error == null) {
                    log.tracef("%s finished reading request %d", ManagementChannel.this, header.getBatchId());
                } else {
                    log.tracef(error, "%s finished reading request %d with error", ManagementChannel.this, header.getBatchId());
                }
            }
        }

        private void processRequest(ManagementRequestHeader requestHeader, ManagementRequestHandler requestHandler) throws RequestProcessingException {
            log.tracef("%s processing request %d", ManagementChannel.this, requestHeader.getBatchId());
            try {
                requestHandler.processRequest();
                log.tracef("%s finished processing request %d", ManagementChannel.this, requestHeader.getBatchId());
            } catch (Exception e) {
                log.tracef(e, "%s finished processing request %d with error", ManagementChannel.this, requestHeader.getBatchId());
            }
        }


        private void writeResponse(ManagementRequestHeader requestHeader, ManagementRequestHandler requestHandler, Exception error) {
            log.tracef("%s writing response %d", ManagementChannel.this, requestHeader.getBatchId());
            final FlushableDataOutputImpl output;
            try {
                output = FlushableDataOutputImpl.create(writeMessage());
            } catch (Exception e) {
                log.tracef(e, "%s could not open output stream for request %d", ManagementChannel.this, requestHeader.getBatchId());
                return;
            }
            try {
                writeResponseHeader(requestHeader, output, error);

                if (error == null && requestHandler != null) {
                    requestHandler.writeResponse(output);
                }
                output.writeByte(ManagementProtocol.RESPONSE_END);
            } catch (Exception e) {
                log.tracef(e, "%s finished writing response %d with error", ManagementChannel.this, requestHeader.getBatchId());
            } finally {
                log.tracef("%s finished writing response %d", ManagementChannel.this, requestHeader.getBatchId());
                IoUtils.safeClose(output);
            }
        }

        private ManagementRequestHandler getRequestHandler(final ManagementRequestHeader header) throws IOException {
            try {
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
            log.tracef("%s handling response %d", ManagementChannel.this, header.getResponseId());
            ManagementResponseHandler<?> responseHandler = responseHandlers.remove(header.getResponseId());
            if (responseHandler == null) {
                throw new IOException("No response handler for request " + header.getResponseId());
            }
            try {
                responseHandler.setContextInfo(header, ManagementChannel.this);
                responseHandler.readResponse(input);
                expectHeader(input, ManagementProtocol.RESPONSE_END);
            } catch (Exception e) {
                throwFormattedException(e);
            } finally {
                responseHandler.removeCloseHandler();
                log.tracef("%s handled response %d", ManagementChannel.this, header.getResponseId());
            }
        }
    }
}
