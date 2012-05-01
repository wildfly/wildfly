/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ProtocolUtils;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A attachment proxy, lazily initializing the streams.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class OperationAttachmentsProxy implements OperationAttachments {

    final List<ProxiedInputStream> proxiedStreams;

    public OperationAttachmentsProxy(final ManagementChannelAssociation channelAssociation, final int batchId, final int size) {
        proxiedStreams = new ArrayList<ProxiedInputStream>(size);
        for (int i = 0 ; i < size ; i++) {
            proxiedStreams.add(new ProxiedInputStream(channelAssociation, batchId, i));
        }
    }

    static OperationAttachmentsProxy create(final ManagementChannelAssociation channelAssociation, final int batchId, final int size) {
        return new OperationAttachmentsProxy(channelAssociation, batchId, size);
    }

    @Override
    public boolean isAutoCloseStreams() {
        return false;
    }

    @Override
    public List<InputStream> getInputStreams() {
        List<InputStream> result = new ArrayList<InputStream>();
        result.addAll(proxiedStreams);
        return Collections.unmodifiableList(result);
    }

    @Override
    public void close() throws IOException {
        //
    }

    void shutdown(Exception error) {
        for (final ProxiedInputStream stream : proxiedStreams) {
            stream.shutdown(error);
        }
    }

    public static class ProxiedInputStream extends InputStream {
        static final int BUFFER_SIZE = 8192;

        private final int index;
        private final int batchId;
        private final PipedInputStream pipe = new PipedInputStream(BUFFER_SIZE);
        private final ManagementChannelAssociation channelAssociation;

        private volatile boolean initialized;
        private volatile Exception error;

        ProxiedInputStream(final ManagementChannelAssociation channelAssociation, final int batchId, final int index) {
            this.channelAssociation = channelAssociation;
            this.batchId = batchId;
            this.index = index;
        }


        @Override
        public int read() throws IOException {
            if(! initialized && error == null) {
                synchronized (this) {
                    if(! initialized) {
                        initializeBytes();
                    }
                }
            }
            if (error != null) {
                if (error instanceof IOException) {
                    throw (IOException)error;
                }
                throw new IOException(error);
            }
            return pipe.read();
        }

        void initializeBytes() {
            if (! initialized) {
                initialized = true;
                try {
                    final PipedOutputStream os = new PipedOutputStream(pipe);
                    // Execute the async request
                    channelAssociation.executeRequest(batchId, new AbstractManagementRequest<Object, Object>() {

                        @Override
                        public byte getOperationType() {
                            return ModelControllerProtocol.GET_INPUTSTREAM_REQUEST;
                        }

                        @Override
                        protected void sendRequest(ActiveOperation.ResultHandler<Object> resultHandler, ManagementRequestContext<Object> context, FlushableDataOutput output) throws IOException {
                            output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
                            output.writeInt(index);
                        }

                        @Override
                        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Object> resultHandler, ManagementRequestContext<Object> context) throws IOException {
                            try {
                                ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                                final int size = input.readInt();
                                ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
                                final byte[] buffer = new byte[BUFFER_SIZE];
                                int totalRead = 0;
                                while (totalRead < size) {
                                    int len = Math.min((int) (size - totalRead), buffer.length);
                                    input.readFully(buffer, 0, len);
                                    os.write(buffer, 0, len);
                                    totalRead += len;
                                }
                                os.close();
                            } catch(IOException e) {
                                shutdown(e);
                                throw e;
                            }
                        }
                    });
                } catch (IOException e) {
                    shutdown(e);
                }
            }
        }

        void shutdown(Exception error) {
            StreamUtils.safeClose(this);
            this.error = error;
        }

        @Override
        public void close() throws IOException {
            pipe.close();
        }
    }
}
