/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.DataInput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.support.ChannelServer;
import org.jboss.as.protocol.mgmt.support.SimpleHandlers;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.OpenListener;
import org.junit.Ignore;
import org.junit.Test;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

/**
 * Not really a test, more a util to play with how to shut down tests
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class CloseChannels {


    @Test
    @Ignore
    public void testChannelClose() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool();
        final ChannelServer.Configuration serverConfig = new ChannelServer.Configuration();
        serverConfig.setBindAddress(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 6999));
        serverConfig.setEndpointName("Test");
        serverConfig.setUriScheme("testing");
        serverConfig.setExecutor(executor);
        ChannelServer server = ChannelServer.create(serverConfig);
        server.addChannelOpenListener("channel", new OpenListener() {

            @Override
            public void registrationTerminated() {
            }

            @Override
            public void channelOpened(Channel channel) {
                System.out.println("Opened channel");
                final AbstractMessageHandler handler = new AbstractMessageHandler(executor) {

                    @Override
                    protected ManagementRequestHandler<Integer, Void> getRequestHandler(ManagementRequestHeader header) {
                        return new ManagementRequestHandler<Integer, Void>() {
                            @Override
                            public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Integer> resultHandler, ManagementRequestContext<Void> context) throws IOException {
                                System.out.println("Reading request");
                                ProtocolUtils.expectHeader(input, 11);
                                final int i = input.readInt();
                                context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                                    @Override
                                    public void execute(ManagementRequestContext<Void> context) throws Exception {
                                        ProtocolUtils.writeResponse(new ProtocolUtils.ResponseWriter() {
                                            @Override
                                            public void write(final FlushableDataOutput output) throws IOException {
                                                System.out.println("Writing response " + i);
                                                output.write(22);
                                                output.writeInt(i);
                                            }
                                        }, context);
                                    }
                                });
                            }
                        };
                    }
                };

                channel.addCloseHandler(new CloseHandler<Channel>() {
                    public void handleClose(final Channel closed, final IOException exception) {
                        System.out.println("server close handler!!!");
                    }
                });
            }
        });
        try {

            for (int i = 0 ; i < 1000 ; i++) {
                ProtocolChannelClient.Configuration clientConfig = new ProtocolChannelClient.Configuration();
                clientConfig.setEndpointName("Test");
                clientConfig.setUri(new URI("testing://127.0.0.1:6999"));
                clientConfig.setUriScheme("testing");
                ProtocolChannelClient protocolClient = ProtocolChannelClient.create(clientConfig);
                final int val = i;
                final Connection connection = protocolClient.connectSync(null); // TODO
                System.out.println("Opening channel");
                final Channel clientChannel = connection.openChannel("channel", OptionMap.EMPTY).get();
                clientChannel.addCloseHandler(new CloseHandler<Channel>() {
                    public void handleClose(final Channel closed, final IOException exception) {
                        System.out.println("client close handler");
                    }
                });
                final SimpleHandlers.SimpleClient client = SimpleHandlers.SimpleClient.create(clientChannel, executor);
                try {
                    final ManagementRequest<Integer, Void> request = new AbstractManagementRequest<Integer, Void>() {

                        @Override
                        public byte getOperationType() {
                            return 66; //Doesn't matter in this case
                        }

                        @Override
                        protected void sendRequest(ActiveOperation.ResultHandler<Integer> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
                            System.out.println("Writing request");
                            output.write(11);
                            output.writeInt(val);
                        }

                        @Override
                        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Integer> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
                            System.out.println("Reading response");
                            ProtocolUtils.expectHeader(input, 22);
                            resultHandler.done(input.readInt());
                        }
                    };
                    int result = client.executeForResult(request);
                    Assert.assertEquals(val, result);
                } finally {
                    IoUtils.safeClose(protocolClient);
                }
            }
        } finally {
            server.close();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
    }


}
