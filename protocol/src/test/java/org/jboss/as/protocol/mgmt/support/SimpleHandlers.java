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
package org.jboss.as.protocol.mgmt.support;

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.threads.AsyncFuture;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SimpleHandlers {

    public static final byte SIMPLE_REQUEST = 102;
    public static final byte REQUEST_WITH_NO_HANDLER = 103;
    public static final byte REQUEST_WITH_BAD_READ = 104;
    public static final byte REQUEST_WITH_BAD_WRITE = 105;
    public static final byte REQUEST_WITH_NO_RESPONSE = 106;

    public static class Request extends AbstractManagementRequest<Integer, Void> {
        final int sentData;
        final byte requestCode;

        public Request(byte requestCode, int sentData) {
            this.requestCode = requestCode;
            this.sentData = sentData;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<Integer> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
            output.writeInt(sentData);
        }

        @Override
        public byte getOperationType() {
            return requestCode;
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Integer> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            resultHandler.done(input.readInt());
        }
    }

    public static class OperationHandler extends AbstractMessageHandler {

        public OperationHandler() {
            super(Executors.newCachedThreadPool());
        }

        @Override
        protected ManagementRequestHeader validateRequest(ManagementProtocolHeader header) throws IOException {
            ManagementRequestHeader request = super.validateRequest(header);
            super.registerActiveOperation(request.getBatchId(), (Void) null);
            return request;
        }

        @Override
        protected ManagementRequestHandler<?, ?> getRequestHandler(final ManagementRequestHeader header) {
            final byte operationType = header.getOperationId();
            switch (operationType) {
                case SIMPLE_REQUEST:
                    return new RequestHandler();
                case REQUEST_WITH_BAD_READ:
                    return new BadReadRequestHandler();
                case REQUEST_WITH_BAD_WRITE:
                    return new BadWriteRequestHandler();
                case REQUEST_WITH_NO_RESPONSE:
                    return new NoResponseHandler();
                case REQUEST_WITH_NO_HANDLER:
                    //No handler for this
                default:
                    return super.getRequestHandler(header);
                }
        }
    }

    private abstract static class AbstractHandler implements ManagementRequestHandler<Void, Void> {

        abstract int readRequest(DataInput input) throws IOException;
        abstract void writeResponse(final FlushableDataOutput output, int data) throws IOException;

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler,
                                  final ManagementRequestContext<Void> context) throws IOException {

            final int data = readRequest(input);
            context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                @Override
                public void execute(ManagementRequestContext<Void> context) throws Exception {
                    ProtocolUtils.writeResponse(new ProtocolUtils.ResponseWriter() {
                        @Override
                        public void write(final FlushableDataOutput output) throws IOException {
                            writeResponse(output, data);
                        }
                    }, context);
                    resultHandler.done(null);
                }
            });
        }
    }

    public static class RequestHandler extends AbstractHandler {


        @Override
        public int readRequest(DataInput input) throws IOException {
            return input.readInt();
        }

        @Override
        public void writeResponse(FlushableDataOutput output, int data) throws IOException {
            output.writeInt(data * 2);
        }
    }

    public static class BadReadRequestHandler extends AbstractHandler {

        @Override
        public int readRequest(DataInput input) throws IOException {
            throw new IOException("BadReadRequest");
        }

        @Override
        void writeResponse(FlushableDataOutput output, int data) throws IOException {
            throw new IllegalStateException();
        }
    }

    public static class BadWriteRequestHandler extends AbstractHandler {

        @Override
        public int readRequest(DataInput input) throws IOException {
            return input.readInt();
        }

        @Override
        public void writeResponse(FlushableDataOutput output, int data) throws IOException {
            throw new IOException("BadWriteRequest");
        }
    }

    public static class NoResponseHandler implements ManagementRequestHandler<Void, Void> {
        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> voidResultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            input.readInt();
        }
    }

    public static class SimpleClient extends AbstractMessageHandler {

        private final Channel channel;
        SimpleClient(final Channel channel, final ExecutorService executorService) {
            super(executorService);
            this.channel = channel;
        }

        public Integer executeForResult(ManagementRequest<Integer, Void> request) throws ExecutionException, InterruptedException {
            return execute(request).get();
        }

        public AsyncFuture<Integer> execute(ManagementRequest<Integer, Void> request) {
            final ActiveOperation<Integer, Void> support = super.registerActiveOperation(null);
            return super.executeRequest(request, channel, support);
        }

        public static SimpleClient create(final Channel channel, final ExecutorService executorService) {
            final SimpleClient client = new SimpleClient(channel, executorService);
            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(Channel closed, IOException exception) {
                    client.shutdownNow();
                }
            });
            channel.receiveMessage(ManagementChannelReceiver.createDelegating(client));
            return client;
        }

        public void shutdown() {
            super.shutdown();
        }

        public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            return super.awaitCompletion(timeout, unit);
        }

        public static SimpleClient create(final RemotingChannelPairSetup setup) {
            final Channel channel = setup.getClientChannel();
            return create(channel, setup.getExecutorService());
        }

    }

}
