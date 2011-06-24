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
import org.jboss.as.protocol.old.ProtocolUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.OpenListener;
import org.junit.Test;
import org.xnio.IoUtils;

/**
 * Not really a test, more a util to play with how to shut down tests
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class CloseChannels {


    @Test
    public void testChannelClose() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        ChannelServer.Configuration serverConfig = new ChannelServer.Configuration();
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
                final ManagementChannel protocolChannel = new ManagementChannelFactory(new ManagementOperationHandler() {
                    @Override
                    public ManagementRequestHandler getRequestHandler(byte id) {
                        return new ManagementRequestHandler() {
                            int i;
                            @Override
                            protected void readRequest(DataInput input) throws IOException {
                                System.out.println("Reading request");
                                ProtocolUtils.expectHeader(input, 11);
                                i = input.readInt();
                            }

                            @Override
                            protected void writeResponse(FlushableDataOutput output) throws IOException {
                                System.out.println("Writing response " + i);
                                output.write(22);
                                output.writeInt(i);
                            }

                        };
                    }
                }).create("channel", channel);
                protocolChannel.startReceiving();
                channel.addCloseHandler(new CloseHandler<Channel>() {
                    public void handleClose(final Channel closed, final IOException exception) {
                        System.out.println("server close handler!!!");
                    }
                });
            }
        });
        try {


            for (int i = 0 ; i < 1000 ; i++) {
                ProtocolChannelClient.Configuration<ManagementChannel> clientConfig = new ProtocolChannelClient.Configuration<ManagementChannel>();
                clientConfig.setEndpointName("Test");
                clientConfig.setExecutor(executor);
                clientConfig.setUri(new URI("testing://127.0.0.1:6999"));
                clientConfig.setUriScheme("testing");
                clientConfig.setChannelFactory(new ManagementChannelFactory());
                ProtocolChannelClient<ManagementChannel> client = ProtocolChannelClient.create(clientConfig);
                final int val = i;
                client.connect(null); // TODO - FIXME
                System.out.println("Opening channel");
                final ManagementChannel clientChannel = client.openChannel("channel");
                clientChannel.addCloseHandler(new CloseHandler<Channel>() {
                    public void handleClose(final Channel closed, final IOException exception) {
                        System.out.println("client close handler");
                    }
                });
                clientChannel.startReceiving();
                try {
                    int result = new ManagementRequest<Integer>() {
                        @Override
                        protected byte getRequestCode() {
                            return 66; //Doesn't matter in this case
                        }

                        @Override
                        protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
                            System.out.println("Writing request");
                            output.write(11);
                            output.writeInt(val);
                        }

                        protected ManagementResponseHandler<Integer> getResponseHandler() {
                            return new ManagementResponseHandler<Integer>() {

                                @Override
                                protected Integer readResponse(DataInput input) throws IOException {
                                    System.out.println("Reading response");
                                    ProtocolUtils.expectHeader(input, 22);
                                    return input.readInt();
                                }
                            };
                        }

                    }.executeForResult(executor, ManagementClientChannelStrategy.create(clientChannel));
                    Assert.assertEquals(val, result);
                } finally {
                    IoUtils.safeClose(client);
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
