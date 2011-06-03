/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol.mgmt;

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.protocol.ProtocolChannelReceiver;
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
public class ManagementChannelReceiver extends ProtocolChannelReceiver {

    private final RequestReceiver requestReceiver = new RequestReceiver();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();

    ManagementChannelReceiver() {
    }

    public void setOperationHandler(final ManagementOperationHandler handler) {
        requestReceiver.setOperationHandler(handler);
    }

    public void registerResponseHandler(final int requestId, final ManagementResponseHandler<?> handler) throws IOException {
        responseReceiver.registerResponseHandler(requestId, handler);
    }

    @Override
    public void doHandle(final Channel channel, final MessageInputStream message) {
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


    private class RequestReceiver {
        private ManagementOperationHandler operationHandler;

        private void handleRequest(final ManagementRequestHeader header, final DataInput input) throws IOException {
            // Work with the lowest protocol version
            ManagementOperationHandler operationHandler = getOperationHandler(header);
            final FlushableDataOutputImpl output = FlushableDataOutputImpl.create(getChannel().writeMessage());
            try {
                //Read request
                expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
                ManagementRequestHandler requestHandler = getRequestHandler(operationHandler, input);
                expectHeader(input, ManagementProtocol.REQUEST_START);
                expectHeader(input, ManagementProtocol.REQUEST_BODY);
                requestHandler.setChannel(getChannel());
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
