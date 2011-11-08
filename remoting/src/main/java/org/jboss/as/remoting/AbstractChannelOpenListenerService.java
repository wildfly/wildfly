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
 * Abstract service responsible for listening for channel open requests.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractChannelOpenListenerService<T> implements Service<Void>, OpenListener {

    protected final Logger log = Logger.getLogger("org.jboss.as.remoting");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();

    protected final String channelName;
    private final OptionMap optionMap;
    private final Set<T> channels = Collections.synchronizedSet(new HashSet<T>());

    private volatile Registration registration;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AbstractChannelOpenListenerService(final String channelName, OptionMap optionMap) {
        this.channelName = channelName;
        this.optionMap = optionMap;
    }

    public ServiceName getServiceName(ServiceName endpointName) {
        return RemotingServices.channelServiceName(endpointName, channelName);
    }

    public InjectedValue<Endpoint> getEndpointInjector(){
        return endpointValue;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            closed.set(false);
            log.debugf("Registering channel listener for %s", channelName);
            registration = endpointValue.getValue().registerService(channelName, this, optionMap);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (registration != null) {
            registration.close();
        }
        synchronized (channels) {
            // Copy off the set to avoid ConcurrentModificationException
            final Set<T> copy = new HashSet<T>(channels);
            for (T channel : copy) {
                closeChannelOnShutdown(channel);
            }
        }
    }

    @Override
    public void channelOpened(Channel channel) {
        final T createdChannel = createChannel(channel);
        channels.add(createdChannel);
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                channels.remove(createdChannel);
                log.tracef("Handling close for %s", createdChannel);
            }
        });
    }

    @Override
    public void registrationTerminated() {
    }

    protected abstract T createChannel(Channel channel);

    protected abstract void closeChannelOnShutdown(T channel);
}
