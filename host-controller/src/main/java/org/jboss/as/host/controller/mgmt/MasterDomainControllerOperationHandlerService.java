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
package org.jboss.as.host.controller.mgmt;

import org.jboss.as.controller.remote.ModelControllerClientOperationHandler;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;

import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementPongRequestHandler;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.threads.JBossThreadFactory;

import java.io.IOException;
import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Installs {@link MasterDomainControllerOperationHandlerImpl} which handles requests from slave DC to master DC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MasterDomainControllerOperationHandlerService extends AbstractModelControllerOperationHandlerFactoryService {

    public static final ServiceName SERVICE_NAME = DomainController.SERVICE_NAME.append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);

    private final DomainController domainController;
    private final HostControllerRegistrationHandler.OperationExecutor operationExecutor;
    private final ManagementPongRequestHandler pongRequestHandler = new ManagementPongRequestHandler();
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("slave-request-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private volatile ExecutorService slaveRequestExecutor;

    public MasterDomainControllerOperationHandlerService(final DomainController domainController, final HostControllerRegistrationHandler.OperationExecutor operationExecutor) {
        this.domainController = domainController;
        this.operationExecutor = operationExecutor;
    }

    protected String getThreadGroupName() {
        return "domain-mgmt-handler-thread";
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        pongRequestHandler.resetConnectionId();
        slaveRequestExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE,
                5L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                threadFactory);
        super.start(context);
    }

    @Override
    public synchronized void stop(StopContext context) {
        slaveRequestExecutor.shutdown();
        super.stop(context);
    }

    @Override
    public Channel.Key startReceiving(final Channel channel) {
        final ManagementChannelHandler handler = new ManagementChannelHandler(ManagementClientChannelStrategy.create(channel), getExecutor());
        // Assemble the request handlers for the domain channel
        handler.addHandlerFactory(new HostControllerRegistrationHandler(handler, domainController, operationExecutor, slaveRequestExecutor));
        handler.addHandlerFactory(new ModelControllerClientOperationHandler(getController(), handler));
        handler.addHandlerFactory(new MasterDomainControllerOperationHandlerImpl(domainController, slaveRequestExecutor));
        handler.addHandlerFactory(pongRequestHandler);
        final Channel.Key key = channel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(Channel closed, IOException exception) {
                handler.shutdown();
                boolean interrupted = false;
                try {
                    if (!handler.awaitCompletion(CHANNEL_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                        ROOT_LOGGER.gracefulManagementChannelHandlerShutdownTimedOut(CHANNEL_SHUTDOWN_TIMEOUT);
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                    ROOT_LOGGER.serviceShutdownIncomplete(e);
                } catch (Exception e) {
                    ROOT_LOGGER.serviceShutdownIncomplete(e);
                } finally {
                    handler.shutdownNow();
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        channel.receiveMessage(handler.getReceiver());
        return key;
    }

}
