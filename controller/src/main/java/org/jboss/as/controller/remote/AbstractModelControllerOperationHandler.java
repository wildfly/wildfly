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

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageOutputStream;
import org.jboss.threads.AsyncFuture;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractModelControllerOperationHandler<T, A> extends AbstractMessageHandler<T, A> {

    public AbstractModelControllerOperationHandler(ExecutorService executorService) {
        super(executorService);
    }


    /**
     * A proxy to the operation message handler on the remote caller
     */
    class OperationMessageHandlerProxy implements OperationMessageHandler {
        final Channel channel;
        final int batchId;

        public OperationMessageHandlerProxy(final Channel channel, final int batchId) {
            this.channel = channel;
            this.batchId = batchId;
        }

        @Override
        public void handleReport(final MessageSeverity severity, final String message) {
            if(true) {
                return;
            }
            getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // We don't expect any response, so just write the message
                        final MessageOutputStream os = channel.writeMessage();
                        try {
                            final FlushableDataOutput output = ProtocolUtils.wrapAsDataOutput(os);
                            final ManagementRequestHeader header = new ManagementRequestHeader(ManagementProtocol.VERSION, -1, batchId, ModelControllerProtocol.HANDLE_REPORT_REQUEST);
                            header.write(output);
                            output.write(ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
                            output.writeUTF(severity.toString());
                            output.write(ModelControllerProtocol.PARAM_MESSAGE);
                            output.writeUTF(message);
                            output.writeByte(ManagementProtocol.REQUEST_END);
                            output.close();
                        } finally {
                            StreamUtils.safeClose(os);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    class OperationAttachmentsProxy implements OperationAttachments {
        final List<ProxiedInputStream> proxiedStreams;

        OperationAttachmentsProxy(final Channel channel, final int batchId, final int size){
            proxiedStreams = new ArrayList<ProxiedInputStream>(size);
            for (int i = 0 ; i < size ; i++) {
                proxiedStreams.add(new ProxiedInputStream(channel, batchId, i));
            }
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
            for (ProxiedInputStream stream : proxiedStreams) {
                stream.shutdown(error);
            }
        }
    }

    private class ProxiedInputStream extends InputStream {
        final Channel channel;
        final int batchId;
        final int index;
        volatile byte[] bytes;
        volatile ByteArrayInputStream delegate;
        volatile Exception error;

        ProxiedInputStream(final Channel channel, final int batchId, final int index) {
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
                            throw MESSAGES.remoteCallerThreadInterrupted();
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
                final ActiveOperation<T, A> support = AbstractModelControllerOperationHandler.this.getActiveOperation(batchId);
                AbstractModelControllerOperationHandler.this.executeRequest(new AbstractManagementRequest<T, A>() {

                    @Override
                    public byte getOperationType() {
                        return ModelControllerProtocol.GET_INPUTSTREAM_REQUEST;
                    }

                    @Override
                    protected void sendRequest(ActiveOperation.ResultHandler<T> resultHandler, ManagementRequestContext<A> context, FlushableDataOutput output) throws IOException {
                        output.write(ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
                        output.writeInt(index);
                    }

                    @Override
                    public void handleRequest(DataInput input, ActiveOperation.ResultHandler<T> resultHandler, ManagementRequestContext<A> context) throws IOException {
                        // TODO execute async
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
                    }
                }, channel, support);
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
