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

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.remote.NewRemoteProxyController;
import org.jboss.as.controller.remote.NewTransactionalModelControllerOperationHandler;
import org.jboss.as.controller.support.RemoteChannelPairSetup;
import org.jboss.as.protocol.mgmt.ManagementBatchIdManager;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.junit.After;

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
    protected NewProxyController createProxyController(final NewModelController proxiedController, final PathAddress proxyNodeAddress) {
        try {
            channels = new RemoteChannelPairSetup();
            channels.setupRemoting();
            channels.startChannels();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ManagementChannel serverChannel = channels.getServerChannel();
        ManagementChannel clientChannel = channels.getClientChannel();
        clientChannel.startReceiving();

        NewTransactionalModelControllerOperationHandler operationHandler = new NewTransactionalModelControllerOperationHandler(channels.getExecutorService(), proxiedController);
        serverChannel.setOperationHandler(operationHandler);
        serverChannel.setBatchIdManager(ManagementBatchIdManager.DEFAULT);

        NewRemoteProxyController proxyController = NewRemoteProxyController.create(channels.getExecutorService(), proxyNodeAddress, channels.getClientChannel());
        clientChannel.setOperationHandler(proxyController);

        return proxyController;
    }
}
