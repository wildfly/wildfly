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

import static org.jboss.as.protocol.StreamUtils.writeUTFZBytes;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.remote.ModelControllerOperationHandler;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.mgmt.ManagementHeaderMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Client used to interact with the local {@link HostController}.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class HostControllerServerClient implements Service<HostControllerServerClient> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");
    private final InjectedValue<Connection> smConnection = new InjectedValue<Connection>();
    private final InjectedValue<NewModelController> controller = new InjectedValue<NewModelController>();
    private final String serverName;
    private volatile ModelControllerOperationHandler modelControllerOperationHandler;
    private final MessageHandler initialMessageHandler = new ManagementHeaderMessageHandler() {

        @Override
        protected MessageHandler getHandlerForId(byte handlerId) {
            return modelControllerOperationHandler;
        }
    };

    public HostControllerServerClient(final String serverName) {
        this.serverName = serverName;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final Connection smConnection = this.smConnection.getValue();
        try {
            new ServerRegisterRequest().executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(smConnection));
        } catch (Exception e) {
            throw new StartException("Failed to send registration message to host controller", e);
        }
        throw new IllegalStateException("Domain mode is disabled until remoting is integrated");
        //modelControllerOperationHandler = ModelControllerOperationHandler.Factory.create(controller.getValue(), initialMessageHandler);
        //smConnection.setMessageHandler(initialMessageHandler);
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
    }

    public String getServerName(){
        return serverName;
    }

    /** {@inheritDoc} */
    public HostControllerServerClient getValue() throws IllegalStateException {
        return this;
    }

    public Injector<Connection> getSmConnectionInjector() {
        return smConnection;
    }

    public Injector<NewModelController> getServerControllerInjector() {
        return controller;
    }



    private class ServerRegisterRequest extends ManagementRequest<Void> {
        @Override
        protected byte getHandlerId() {
            return DomainServerProtocol.SERVER_TO_HOST_CONTROLLER_OPERATION;
        }

        @Override
        protected byte getRequestCode() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.REGISTER_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            writeUTFZBytes(output, serverName);
        }
    }
}
