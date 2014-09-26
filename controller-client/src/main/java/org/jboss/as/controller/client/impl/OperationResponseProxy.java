/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.impl;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.dmr.ModelNode;

/**
 * An {@link org.jboss.as.controller.client.OperationResponse} that proxies back to a remote server
 * to read any attached response streams.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class OperationResponseProxy implements OperationResponse {

    private final ModelNode responseNode;
    private final Map<String, StreamEntry> proxiedStreams;

    private OperationResponseProxy(final ModelNode responseNode, final ManagementChannelAssociation channelAssociation, final int batchId, final ModelNode streamHeader) {
        this.responseNode = responseNode;
        int size = streamHeader.asInt();
        proxiedStreams = new LinkedHashMap<String, StreamEntry>(size);
        for (int i = 0; i < size; i++) {
            ModelNode headerElement =  streamHeader.get(i);
            final String uuid = headerElement.require("uuid").asString();
            final String mimeType = headerElement.require("mime-type").asString();
            proxiedStreams.put(uuid, new ProxiedInputStream(uuid, mimeType, channelAssociation, batchId, i));
        }
    }

    public static OperationResponseProxy create(final ModelNode responseNode, final ManagementChannelAssociation channelAssociation, final int batchId, final ModelNode streamHeader) {
        return new OperationResponseProxy(responseNode, channelAssociation, batchId, streamHeader);
    }

    @Override
    public ModelNode getResponseNode() {
        return responseNode;
    }

    @Override
    public List<StreamEntry> getInputStreams() {
        List<StreamEntry> result = new ArrayList<StreamEntry>();
        result.addAll(proxiedStreams.values());
        return Collections.unmodifiableList(result);
    }

    @Override
    public StreamEntry getInputStream(String uuid) {
        return proxiedStreams.get(uuid);
    }

    @Override
    public void close() throws IOException {
        for (StreamEntry se : proxiedStreams.values()) {
            se.getStream().close();
        }
    }

    private static class ProxiedInputStream extends InputStream implements StreamEntry {
        static final int BUFFER_SIZE = 8192;

        private final String uuid;
        private final String mimeType;
        private final int index;
        private final int batchId;
        private final Pipe pipe;
        private final ManagementChannelAssociation channelAssociation;
        private volatile boolean remoteClosed;
        private boolean remoteRead;
        private volatile Exception error;

        ProxiedInputStream(final String uuid, final String mimeType, final ManagementChannelAssociation channelAssociation,
                           final int batchId, final int index) {
            this.uuid = uuid;
            this.mimeType = mimeType;
            this.channelAssociation = channelAssociation;
            this.batchId = batchId;
            this.index = index;
            pipe = new Pipe(BUFFER_SIZE);
        }

        @Override
        public int read() throws IOException {
            if (available() < 1) {
                readRemote();
            }
            return pipe.getIn().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (available() < len) {
                readRemote();
            }
            return pipe.getIn().read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            IOException ex = null;
            try {
                closeRemote();
            } catch (IOException e) {
                ex = e;
            }
            try {
                pipe.getOut().close();
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                }
            }
            try {
                pipe.getIn().close();
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                }
            }
            if (ex != null) {
                throw ex;
            }
        }

        @Override
        public int available() throws IOException {
            return pipe.getIn().available();
        }

        private void readRemote() throws IOException {
            readInputStream();
            throwIfError();
        }

        private synchronized void readInputStream() {
            if (remoteRead || remoteClosed) {
                return;
            }

            final OutputStream os = pipe.getOut();
            // Execute the async request
            final ManagementRequest<Void, Void> getISRequest = new AbstractManagementRequest<Void, Void>() {

                @Override
                public byte getOperationType() {
                    return ModelControllerProtocol.GET_CHUNKED_INPUTSTREAM_REQUEST;
                }

                @Override
                protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
                    output.write(ModelControllerProtocol.PARAM_OPERATION);
                    output.writeInt(batchId);
                    output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
                    output.writeInt(index);
                }

                @Override
                public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> context) throws IOException {
                    try {
                        // Loop reading chunk until we get an end message
                        IOException pipeWriteException = null;
                        for (;;) {
                            byte header = input.readByte();
                            if (header == ModelControllerProtocol.PARAM_END) {
                                remoteClosed = true;
                                break;
                            }
                            ProtocolUtils.expectHeader(header, ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                            int size = input.readInt();
                            ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
                            final byte[] buffer = new byte[BUFFER_SIZE];
                            int totalRead = 0;
                            while (totalRead < size) {
                                int len = Math.min(size - totalRead, buffer.length);
                                input.readFully(buffer, 0, len);
                                if (pipeWriteException == null) {
                                    try {
                                        os.write(buffer, 0, len);
                                    } catch (IOException e) {
                                        // The ProxiedInputStream must have been closed
                                        // From now on we just read and discard the bytes
                                        pipeWriteException = e;
                                    }
                                } // else just read the bytes off the network and discard
                                totalRead += len;
                            }
                        }

                        os.close();
                        if (pipeWriteException != null) {
                            throw pipeWriteException;
                        }
                        resultHandler.done(null);
                    } catch (IOException e) {
                        shutdown(e);
                        resultHandler.failed(e);
                        throw e;
                    }
                }
            };

            try {
                channelAssociation.executeRequest(getISRequest, null);
                remoteRead = true;
            } catch (IOException e) {
                shutdown(e);
            }
        }

        private void closeRemote() throws IOException {
            if (!remoteClosed) {
                final ManagementRequest<Void, Void> closeRequest = new AbstractManagementRequest<Void, Void>() {

                    @Override
                    public byte getOperationType() {
                        return ModelControllerProtocol.CLOSE_INPUTSTREAM_REQUEST;
                    }

                    @Override
                    protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
                        output.write(ModelControllerProtocol.PARAM_OPERATION);
                        output.writeInt(batchId);
                        output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
                        output.writeInt(index);
                    }

                    @Override
                    public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> context) throws IOException {
                        remoteClosed = true;
                    }

                };

                channelAssociation.executeRequest(closeRequest, null);
            }
        }

        private void throwIfError() throws IOException {
            if (error != null) {
                if (error instanceof IOException) {
                    throw (IOException) error;
                }
                throw new IOException(error);
            }
        }

        private void shutdown(Exception error) {
            StreamUtils.safeClose(this);
            this.error = error;
        }

        @Override
        public String getUUID() {
            return uuid;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public InputStream getStream() {
            return this;
        }
    }
}
