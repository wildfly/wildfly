/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.HandleableCloseable;
import org.jboss.threads.JBossThreadFactory;

/**
 * Operation handler responsible for requests coming in from server processes on the host controller.
 * The server side counterpart is {@link org.jboss.as.server.mgmt.domain.HostControllerClient}
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class ServerToHostOperationHandlerFactoryService implements ManagementChannelInitialization, Service<ManagementChannelInitialization> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("management", "server", "to", "host", "controller");

    private final ExecutorService executorService;
    private final InjectedValue<ServerInventory> serverInventory = new InjectedValue<ServerInventory>();
    private final ServerToHostProtocolHandler.OperationExecutor operationExecutor;
    private final DomainController domainController;
    private final ExpressionResolver expressionResolver;

    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("server-registration-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private volatile ExecutorService registrations;

    ServerToHostOperationHandlerFactoryService(ExecutorService executorService, ServerToHostProtocolHandler.OperationExecutor operationExecutor, DomainController domainController, ExpressionResolver expressionResolver) {
        this.executorService = executorService;
        this.operationExecutor = operationExecutor;
        this.domainController = domainController;
        this.expressionResolver = expressionResolver;
    }

    public static void install(final ServiceTarget serviceTarget, final ServiceName serverInventoryName, ExecutorService executorService, ServerToHostProtocolHandler.OperationExecutor operationExecutor, DomainController domainController,
            ExpressionResolver expressionResolver) {
        final ServerToHostOperationHandlerFactoryService serverToHost = new ServerToHostOperationHandlerFactoryService(executorService, operationExecutor, domainController, expressionResolver);
        serviceTarget.addService(ServerToHostOperationHandlerFactoryService.SERVICE_NAME, serverToHost)
            .addDependency(serverInventoryName, ServerInventory.class, serverToHost.serverInventory)
            .install();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        this.registrations = Executors.newSingleThreadExecutor(threadFactory);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        final ExecutorService executorService = this.registrations;
        this.registrations = null;
        if(executorService != null) {
            executorService.shutdown();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ManagementChannelInitialization getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public HandleableCloseable.Key startReceiving(final Channel channel) {
        final ManagementChannelHandler channelHandler = new ManagementChannelHandler(channel, executorService);
        final ServerToHostProtocolHandler registrationHandler = new ServerToHostProtocolHandler(serverInventory.getValue(), operationExecutor, domainController, channelHandler, registrations, expressionResolver);
        channelHandler.addHandlerFactory(registrationHandler);
        channel.receiveMessage(channelHandler.getReceiver());
        return null;
    }

}
