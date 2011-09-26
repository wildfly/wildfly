/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting.management;

import java.io.IOException;

import org.jboss.as.controller.remote.ManagementOperationHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.remoting.AbstractChannelOpenListenerService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.xnio.OptionMap;

/**
 * Service responsible for listening for channel open requests and associating the channel with a {@link ManagementOperationHandler}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ManagementChannelOpenListenerService extends AbstractChannelOpenListenerService<ManagementChannel> {

    private final InjectedValue<ManagementOperationHandlerFactory> operationHandlerFactoryValue = new InjectedValue<ManagementOperationHandlerFactory>();


    ManagementChannelOpenListenerService(String channelName, OptionMap optionMap) {
        super(channelName, optionMap);
    }

    public InjectedValue<ManagementOperationHandlerFactory> getOperationHandlerInjector(){
        return operationHandlerFactoryValue;
    }

    @Override
    protected ManagementChannel createChannel(Channel channel) {
        final ManagementOperationHandler handler = operationHandlerFactoryValue.getValue().createOperationHandler();
        final ManagementChannel managementChannel = new ManagementChannelFactory(handler).create(channelName, channel);
        log.tracef("Opened %s: %s with handler %s", channelName, managementChannel, handler);
        managementChannel.startReceiving();
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                try {
                    managementChannel.sendByeBye();
                } catch (IOException ignore) {
                }
                log.tracef("Handling close for %s", managementChannel);
            }
        });
        return managementChannel;
    }

    @Override
    protected void closeChannelOnShutdown(ManagementChannel channel) {
        try {
            channel.sendByeBye();
        } catch (IOException e) {
        }
    }

}
