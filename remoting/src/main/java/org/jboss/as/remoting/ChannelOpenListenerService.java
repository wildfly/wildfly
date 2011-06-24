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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.remote.ManagementOperationHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.logging.Logger;
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
 * Service responsible for listening for channel open requests and associating the channel with a {@link ManagementOperationHandler}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ChannelOpenListenerService implements Service<Void>, OpenListener {

    private final Logger log = Logger.getLogger("org.jboss.as.remoting");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();

    private final InjectedValue<ManagementOperationHandlerFactory> operationHandlerFactoryValue = new InjectedValue<ManagementOperationHandlerFactory>();

    private final String channelName;
    private final OptionMap optionMap;
    private final Set<ManagementChannel> channels = Collections.synchronizedSet(new HashSet<ManagementChannel>());

    private volatile Registration registration;
    private final AtomicBoolean closed = new AtomicBoolean();

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

    public InjectedValue<ManagementOperationHandlerFactory> getOperationHandlerInjector(){
        return operationHandlerFactoryValue;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            log.debugf("Registering channel listener for %s", channelName);
            registration = endpointValue.getValue().registerService(channelName, this, optionMap);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.close();
        }
        synchronized (channels) {
            for (ManagementChannel channel : channels) {
                try {
                    channel.sendByeBye();
                } catch (IOException e) {
                }
            }

        }
    }

    @Override
    public void channelOpened(Channel channel) {
        final ManagementOperationHandler handler = operationHandlerFactoryValue.getValue().createOperationHandler();
        final ManagementChannel managementChannel = new ManagementChannelFactory(handler).create(channelName, channel);
        channels.add(managementChannel);
        log.tracef("Opened %s: %s with handler %s", channelName, managementChannel, handler);
        managementChannel.startReceiving();
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                channels.remove(managementChannel);
                try {
                    managementChannel.sendByeBye();
                } catch (IOException ignore) {
                }
                log.tracef("Handling close for %s", managementChannel);
            }
        });
    }

    @Override
    public void registrationTerminated() {
    }

}
