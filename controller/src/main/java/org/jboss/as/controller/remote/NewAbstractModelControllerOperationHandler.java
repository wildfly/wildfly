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

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.NewModelControllerProtocol;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.ProtocolUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class NewAbstractModelControllerOperationHandler implements ManagementOperationHandler {

    protected final ProtocolChannel channel;
    protected final ExecutorService executorService;
    protected final NewModelController controller;

    public NewAbstractModelControllerOperationHandler(final ProtocolChannel channel, final ExecutorService executorService, final NewModelController controller) {
        this.channel = channel;
        this.executorService = executorService;
        this.controller = controller;
    }

    @Override
    public abstract ManagementRequestHandler getRequestHandler(final byte id);

    protected ManagementClientChannelStrategy getChannelStrategy() {
        return ManagementClientChannelStrategy.create(channel);
    }

    /**
     * A proxy to the operation message handler on the remote caller
     */
    class OperationMessageHandlerProxy implements OperationMessageHandler {
        final int executionId;

        public OperationMessageHandlerProxy(final int executionId) {
            this.executionId = executionId;
        }

        @Override
        public void handleReport(final MessageSeverity severity, final String message) {
            try {
                //Invoke this synchronously so that the messages appear in the right order on the
                //remote caller
                new ManagementRequest<Void>() {

                    @Override
                    protected byte getRequestCode() {
                        return NewModelControllerProtocol.HANDLE_REPORT_REQUEST;
                    }

                    @Override
                    protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
                        output.write(NewModelControllerProtocol.PARAM_EXECUTION_ID);
                        output.writeInt(executionId);
                        output.write(NewModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
                        output.writeUTF(severity.toString());
                        output.write(NewModelControllerProtocol.PARAM_MESSAGE);
                        output.writeUTF(message);
                    }

                    @Override
                    protected Void readResponse(final DataInput input) throws IOException {
                        return null;
                    }
                }.executeForResult(executorService, getChannelStrategy());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class OperationAttachmentsProxy implements OperationAttachments {
        final List<InputStream> proxiedStreams;

        OperationAttachmentsProxy(final int executionId, final int size){
            proxiedStreams = new ArrayList<InputStream>(size);
            for (int i = 0 ; i < size ; i++) {
                proxiedStreams.add(new ProxiedInputStream(executionId, i));
            }
        }

        @Override
        public List<InputStream> getInputStreams() {
            return Collections.unmodifiableList(proxiedStreams);
        }
    }

    private class ProxiedInputStream extends InputStream {

        final int executionId;
        final int index;
        volatile byte[] bytes;
        volatile ByteArrayInputStream delegate;

        public ProxiedInputStream(final int executionId, final int index) {
            this.executionId = executionId;
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
                        delegate = new ByteArrayInputStream(bytes);
                        bytes = null;
                    }
                }
            }
            return delegate.read();
        }

        void initializeBytes() {
            if (bytes == null) {
                new ManagementRequest<Void>() {
                    @Override
                    protected byte getRequestCode() {
                        return NewModelControllerProtocol.GET_INPUTSTREAM_REQUEST;
                    }

                    @Override
                    protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
                        output.write(NewModelControllerProtocol.PARAM_EXECUTION_ID);
                        output.writeInt(executionId);
                        output.write(NewModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
                        output.writeInt(index);
                    }

                    @Override
                    protected Void readResponse(DataInput input) throws IOException {
                        synchronized (ProxiedInputStream.this) {
                            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                            final int size = input.readInt();
                            ProtocolUtils.expectHeader(input, NewModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);

                            final byte[] buf = new byte[size];
                            for (int i = 0 ; i < size ; i++) {
                                buf[i] = input.readByte();
                            }
                            bytes = buf;
                            ProxiedInputStream.this.notifyAll();
                        }
                        return null;
                    }
                }.execute(executorService, getChannelStrategy());
            }
        }
    }


}
