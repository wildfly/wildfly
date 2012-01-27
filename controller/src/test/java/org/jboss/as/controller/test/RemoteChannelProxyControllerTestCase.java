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
package org.jboss.as.controller.test;

import java.io.IOException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.controller.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.HandleableCloseable;
import org.junit.After;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteChannelProxyControllerTestCase extends AbstractProxyControllerTest {

    RemoteChannelPairSetup channels;

    @After
    public void stopChannels() throws Exception {
        channels.stopChannels();
        channels.shutdownRemoting();
        channels = null;
    }

    @Override
    protected ProxyController createProxyController(final ModelController proxiedController, final PathAddress proxyNodeAddress) {
        try {
            channels = new RemoteChannelPairSetup();
            channels.setupRemoting(new ManagementChannelInitialization() {
                @Override
                public HandleableCloseable.Key startReceiving(Channel channel) {
                    final ManagementChannelHandler support = new ManagementChannelHandler(channel, channels.getExecutorService());
                    support.addHandlerFactory(new TransactionalModelControllerOperationHandler(proxiedController, support));
                    channel.addCloseHandler(new CloseHandler<Channel>() {
                        @Override
                        public void handleClose(Channel closed, IOException exception) {
                            support.shutdownNow();
                        }
                    });
                    channel.receiveMessage(support.getReceiver());
                    return null;
                }
            });
            channels.startClientConnetion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Channel clientChannel = channels.getClientChannel();
        final ManagementChannelHandler support = new ManagementChannelHandler(clientChannel, channels.getExecutorService());
        final RemoteProxyController proxyController = RemoteProxyController.create(support, proxyNodeAddress, ProxyOperationAddressTranslator.SERVER);
        clientChannel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed, IOException exception) {
                support.shutdownNow();
            }
        });
        clientChannel.receiveMessage(support.getReceiver());
        return proxyController;
    }
}
