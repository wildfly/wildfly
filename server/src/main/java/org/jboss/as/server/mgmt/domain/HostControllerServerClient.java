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

package org.jboss.as.server.mgmt.domain;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementResponseHandler;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Client used to interact with the local {@link HostController}.
 * The HC counterpart is ServerToHostOperationHandler
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class HostControllerServerClient implements Service<HostControllerServerClient> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");

    private final InjectedValue<ManagementChannel> hcChannel = new InjectedValue<ManagementChannel>();
    private final InjectedValue<ModelController> controller = new InjectedValue<ModelController>();

    private final String serverName;
    private final String serverProcessName;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public HostControllerServerClient(final String serverName, final String serverProcessName) {
        this.serverName = serverName;
        this.serverProcessName = serverProcessName;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        hcChannel.getValue().setOperationHandler(new TransactionalModelControllerOperationHandler(executor, controller.getValue()));

        try {
            new ServerRegisterRequest().executeForResult(executor, ManagementClientChannelStrategy.create(hcChannel.getValue()));
        } catch (Exception e) {
            throw new StartException("Failed to send registration message to host controller", e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
    }

    public String getServerName(){
        return serverName;
    }

    public String getServerProcessName() {
        return serverProcessName;
    }

    /** {@inheritDoc} */
    public HostControllerServerClient getValue() throws IllegalStateException {
        return this;
    }

    public Injector<ManagementChannel> getHcChannelInjector() {
        return hcChannel;
    }

    public Injector<ModelController> getServerControllerInjector() {
        return controller;
    }

    private class ServerRegisterRequest extends ManagementRequest<Void> {

        @Override
        protected byte getRequestCode() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            output.writeUTF(serverProcessName);
        }

        @Override
        protected ManagementResponseHandler<Void> getResponseHandler() {
            return ManagementResponseHandler.EMPTY_RESPONSE;
        }
    }
}
