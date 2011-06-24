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
package org.jboss.as.controller.remote;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementResponseHandler;
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractModelControllerOperationHandler implements ManagementOperationHandler {

    final Logger log = Logger.getLogger("org.jboss.as.controller.remote");

    protected final ExecutorService executorService;
    protected final ModelController controller;

    public AbstractModelControllerOperationHandler(final ExecutorService executorService, final ModelController controller) {
        this.executorService = executorService;
        this.controller = controller;
    }

    @Override
    public abstract ManagementRequestHandler getRequestHandler(final byte id);

    protected ManagementClientChannelStrategy getChannelStrategy(ManagementChannel channel) {
        return ManagementClientChannelStrategy.create(channel);
    }

    /**
     * A proxy to the operation message handler on the remote caller
     */
    class OperationMessageHandlerProxy implements OperationMessageHandler {
        final ManagementChannel channel;
        final int batchId;

        public OperationMessageHandlerProxy(final ManagementChannel channel, final int batchId) {
            this.channel = channel;
            this.batchId = batchId;
        }

        @Override
        public void handleReport(final MessageSeverity severity, final String message) {

            //TEMPORARILY DISABLE THE OPERATION MESSAGE HANDLER STUFF
            // Once reenabbled un-@Ignore tests in ModelControllerClientTestCase and RemoteProxyControllerProtocolTestCase
            if (true) {
                return;
            }

            try {
                //Invoke this synchronously so that the messages appear in the right order on the
                //remote caller
                new ManagementRequest<Void>(batchId) {

                    @Override
                    protected byte getRequestCode() {
                        return ModelControllerProtocol.HANDLE_REPORT_REQUEST;
                    }

                    @Override
                    protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
                        output.write(ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
                        output.writeUTF(severity.toString());
                        output.write(ModelControllerProtocol.PARAM_MESSAGE);
                        output.writeUTF(message);
                    }

                    @Override
                    protected ManagementResponseHandler<Void> getResponseHandler() {
                        return ManagementResponseHandler.EMPTY_RESPONSE;
                    }
                }.executeForResult(executorService, getChannelStrategy(channel));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class OperationAttachmentsProxy implements OperationAttachments {
        final List<ProxiedInputStream> proxiedStreams;

        OperationAttachmentsProxy(final ManagementChannel channel, final int batchId, final int size){
            proxiedStreams = new ArrayList<ProxiedInputStream>(size);
            for (int i = 0 ; i < size ; i++) {
                proxiedStreams.add(new ProxiedInputStream(channel, batchId, i));
            }
        }

        @Override
        public List<InputStream> getInputStreams() {
            List<InputStream> result = new ArrayList<InputStream>();
            result.addAll(proxiedStreams);
            return Collections.unmodifiableList(result);
        }

        void shutdown(Exception error) {
            for (ProxiedInputStream stream : proxiedStreams) {
                stream.shutdown(error);
            }
        }
    }

    private class ProxiedInputStream extends InputStream {
        final ManagementChannel channel;
        final int batchId;
        final int index;
        volatile byte[] bytes;
        volatile ByteArrayInputStream delegate;
        volatile Exception error;

        ProxiedInputStream(final ManagementChannel channel, final int batchId, final int index) {
            this.channel = channel;
            this.batchId = batchId;
            this.index = index;
        }


        @Override
        public int read() throws IOException {
            if (delegate == null) {
                synchronized (this) {
                    if (delegate == null) {
                        initializeBytes();
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread was interrupted waiting to read attachment input stream from remote caller");
                        }
                        if (error != null) {
                            if (error instanceof IOException) {
                                throw (IOException)error;
                            }
                            throw new IOException(error);
                        }
                        delegate = new ByteArrayInputStream(bytes);
                        bytes = null;
                    }
                }
            }
            return delegate.read();
        }

        void initializeBytes() {
            if (bytes == null) {
                new ManagementRequest<Void>(batchId) {
                    @Override
                    protected byte getRequestCode() {
                        return ModelControllerProtocol.GET_INPUTSTREAM_REQUEST;
                    }

                    @Override
                    protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
                        output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
                        output.writeInt(index);
                    }

                    @Override
                    protected ManagementResponseHandler<Void> getResponseHandler() {
                        return new ManagementResponseHandler<Void>() {
                            protected Void readResponse(DataInput input) throws IOException {
                                synchronized (ProxiedInputStream.this) {
                                    ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                                    final int size = input.readInt();
                                    ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);

                                    final byte[] buf = new byte[size];
                                    for (int i = 0 ; i < size ; i++) {
                                        buf[i] = input.readByte();
                                    }
                                    bytes = buf;
                                    ProxiedInputStream.this.notifyAll();
                                }
                                return null;
                            }
                        };
                    }
                }.execute(executorService, getChannelStrategy(channel));
            }
        }

        void shutdown(Exception error) {
            this.error = error;
            synchronized (this) {
                notifyAll();
            }
        }
    }


}
