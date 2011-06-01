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
package org.jboss.as.remoting;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiverFactory;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.xnio.OptionMap;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ChannelOpenListenerService implements Service<Void>, OpenListener {

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();

    private final InjectedValue<ManagementOperationHandler> operationHandlerValue = new InjectedValue<ManagementOperationHandler>();

    private final String channelName;
    private final OptionMap optionMap;
    private volatile Registration registration;
    private volatile ManagementChannelReceiver receiver;

    public ChannelOpenListenerService(final String channelName, OptionMap optionMap) {
        this.channelName = channelName;
        this.optionMap = optionMap;
    }

    public ServiceName getServiceName() {
        return RemotingServices.channelServiceName(channelName);
    }

    public InjectedValue<Endpoint> getEndpointInjector(){
        return endpointValue;
    }

    public InjectedValue<ManagementOperationHandler> getOperationHandlerInjector(){
        return operationHandlerValue;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            endpointValue.getValue().registerService(channelName, this, optionMap);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void channelOpened(Channel channel) {
        ProtocolChannel protocolChannel = ProtocolChannel.create(channelName, channel, new ManagementChannelReceiverFactory(operationHandlerValue.getValue()));
        protocolChannel.startReceiving();
        channel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed) {
                receiver.stop();
            }
        });
    }

    @Override
    public void registrationTerminated() {
    }

}
