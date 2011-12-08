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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.controller.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.junit.After;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteChannelProxyControllerTestCase extends AbstractProxyControllerTest {

    static RemoteChannelPairSetup channels;

    @After
    public void stopChannels() throws Exception{
        channels.stopChannels();
        channels.shutdownRemoting();
    }

    @Override
    protected ProxyController createProxyController(final ModelController proxiedController, final PathAddress proxyNodeAddress) {
        TransactionalModelControllerOperationHandler operationHandler = new TransactionalModelControllerOperationHandler(proxiedController, Executors.newCachedThreadPool());
        try {
            channels = new RemoteChannelPairSetup();
            channels.setupRemoting(operationHandler);
            channels.startClientConnetion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Channel clientChannel = channels.getClientChannel();
        final RemoteProxyController proxyController = RemoteProxyController.create(channels.getExecutorService(), proxyNodeAddress, ProxyOperationAddressTranslator.SERVER, channels.getClientChannel());
        clientChannel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed, IOException exception) {
                proxyController.shutdownNow();
            }
        });
        clientChannel.receiveMessage(ManagementChannelReceiver.createDelegating(proxyController));
        return proxyController;
    }
}
