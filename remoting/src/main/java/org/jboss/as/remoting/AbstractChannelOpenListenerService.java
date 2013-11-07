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

import static org.jboss.as.remoting.RemotingMessages.MESSAGES;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.as.remoting.management.ManagementChannelRegistryService;
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
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractChannelOpenListenerService implements Service<Void>, OpenListener {

    /** How long we wait for active operations to clear before allowing channel close to proceed */
    protected static final int CHANNEL_SHUTDOWN_TIMEOUT;

    static {
        String prop = null;
        int timeout;
        try {
            prop = SecurityActions.getSystemProperty("jboss.as.management.channel.close.timeout", "15000");
            timeout = Integer.parseInt(prop);
        } catch (NumberFormatException e) {
            ControllerLogger.ROOT_LOGGER.invalidChannelCloseTimeout(e, "jboss.as.management.channel.close.timeout", prop);
            timeout = 15000;
        }
        CHANNEL_SHUTDOWN_TIMEOUT = timeout;
    }

    protected final Logger log = Logger.getLogger("org.jboss.as.remoting");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();
    private final InjectedValue<ManagementChannelRegistryService> registry = new InjectedValue<ManagementChannelRegistryService>();

    protected final String channelName;
    private final OptionMap optionMap;
    private final Set<ManagementChannelInitialization.ManagementChannelShutdownHandle> handles = Collections.synchronizedSet(new HashSet<ManagementChannelInitialization.ManagementChannelShutdownHandle>());

    private volatile boolean closed = true;

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

    public InjectedValue<ManagementChannelRegistryService> getRegistry() {
        return registry;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            closed = false;
            log.debugf("Registering channel listener for %s", channelName);
            final Registration registration = endpointValue.getValue().registerService(channelName, this, optionMap);
            // Add to global registry
            registry.getValue().register(registration);
        } catch (Exception e) {
            throw MESSAGES.couldNotStartChanelListener(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        closed = true;
        // Copy off the set to avoid ConcurrentModificationException
        final Set<ManagementChannelInitialization.ManagementChannelShutdownHandle> handlesCopy = copyHandles();
        for (final ManagementChannelInitialization.ManagementChannelShutdownHandle handle : handlesCopy) {
            handle.shutdown();
        }
        final Runnable shutdownTask = new Runnable() {
            @Override
            public void run() {
                final long end = System.currentTimeMillis() + CHANNEL_SHUTDOWN_TIMEOUT;
                boolean interrupted = Thread.currentThread().isInterrupted();
                try {
                    for (final ManagementChannelInitialization.ManagementChannelShutdownHandle handle : handlesCopy) {
                        final long remaining = end - System.currentTimeMillis();
                        try {
                            if (!interrupted && !handle.awaitCompletion(remaining, TimeUnit.MILLISECONDS)) {
                                ControllerLogger.ROOT_LOGGER.gracefulManagementChannelHandlerShutdownTimedOut(CHANNEL_SHUTDOWN_TIMEOUT);
                            }
                        } catch (InterruptedException e) {
                            interrupted = true;
                            ControllerLogger.ROOT_LOGGER.gracefulManagementChannelHandlerShutdownFailed(e);
                        } catch (Exception e) {
                            ControllerLogger.ROOT_LOGGER.gracefulManagementChannelHandlerShutdownFailed(e);
                        } finally {
                            handle.shutdownNow();
                        }
                    }
                } finally {
                    context.complete();
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        // Execute async shutdown task
        context.asynchronous();
        execute(shutdownTask);
    }

    @Override
    public void channelOpened(Channel channel) {
        // When the server/host is stopping we don't accept new connections
        // this should be using the graceful shutdown control
        if(closed) {
            log.debugf("server shutting down, closing channel %s.", channel);
            channel.closeAsync();
            return;
        }
        final ManagementChannelInitialization.ManagementChannelShutdownHandle handle = handleChannelOpened(channel);
        handles.add(handle);
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                handles.remove(handle);
                handle.shutdownNow();
                log.tracef("Handling close for %s", handle);
            }
        });
    }

    @Override
    public void registrationTerminated() {
        // Copy off the set to avoid ConcurrentModificationException
        final Set<ManagementChannelInitialization.ManagementChannelShutdownHandle> copy = copyHandles();
        for (final ManagementChannelInitialization.ManagementChannelShutdownHandle channel : copy) {
            channel.shutdownNow();
        }
    }

    /**
     * Handle a channel open event.
     *
     * @param channel the opened channel
     * @return the shutdown handle
     */
    protected abstract ManagementChannelInitialization.ManagementChannelShutdownHandle handleChannelOpened(Channel channel);

    /**
     * Execute the shutdown task.
     *
     * @param runnable the runnable
     */
    protected abstract void execute(Runnable runnable);

    private Set<ManagementChannelInitialization.ManagementChannelShutdownHandle> copyHandles() {
        // Must synchronize on Collections.synchronizedSet when iterating
        synchronized (handles) {
            return new HashSet<ManagementChannelInitialization.ManagementChannelShutdownHandle>(handles);
        }
    }

}
