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

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.impl.AbstractModelControllerClient;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExistingChannelModelControllerClient extends AbstractModelControllerClient {

    private final ManagementChannelHandler handler;
    protected ExistingChannelModelControllerClient(final ManagementChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    protected ManagementChannelAssociation getChannelAssociation() throws IOException {
        return handler;
    }

    @Override
    public void close() throws IOException {
        handler.shutdown();
    }

    /**
     * Create and add model controller handler to a existing management channel handler.
     *
     * @param handler the channle hanlder
     * @return the created client
     */
    public static ModelControllerClient createAndAdd(final ManagementChannelHandler handler) {
        final ExistingChannelModelControllerClient client = new ExistingChannelModelControllerClient(handler);
        handler.addHandlerFactory(client);
        return client;
    }

    /**
     * Create a model controller client which is exclusively receiving messages on a existing channel.
     *
     * @param channel the channel
     * @param executorService a executor
     * @return the created client
     */
    public static ModelControllerClient createReceiving(final Channel channel, final ExecutorService executorService) {
        final ManagementChannelHandler handler = new ManagementChannelHandler(channel, executorService);
        final ExistingChannelModelControllerClient client = new ExistingChannelModelControllerClient(handler);
        handler.addHandlerFactory(client);
        channel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed, IOException exception) {
                handler.shutdown();
                try {
                    handler.awaitCompletion(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    handler.shutdownNow();
                }
            }
        });
        channel.receiveMessage(handler.getReceiver());
        return client;
    }

}
