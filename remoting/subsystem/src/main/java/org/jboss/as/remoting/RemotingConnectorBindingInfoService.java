/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that publishes socket binding information for remoting connectors
 *
 * @author Stuart Douglas
 */
public class RemotingConnectorBindingInfoService implements Service<RemotingConnectorBindingInfoService.RemotingConnectorInfo> {

    private static final ServiceName SERVICE_NAME = RemotingServices.REMOTING_BASE.append("remotingConnectorInfoService");

    private final RemotingConnectorInfo binding;

    public RemotingConnectorBindingInfoService(RemotingConnectorInfo binding) {
        this.binding = binding;
    }

    public static ServiceName serviceName(final String connectorName) {
        return SERVICE_NAME.append(connectorName);
    }

    public static void install(final ServiceTarget target, final String connectorName, final SocketBinding binding, final String protocol) {
        target.addService(serviceName(connectorName), new RemotingConnectorBindingInfoService(new RemotingConnectorInfo(binding, protocol))).install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public RemotingConnectorInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return binding;
    }

    public static final class RemotingConnectorInfo {
        private final SocketBinding socketBinding;
        private final String protocol;

        public RemotingConnectorInfo(SocketBinding socketBinding, String protocol) {
            this.socketBinding = socketBinding;
            this.protocol = protocol;
        }

        public SocketBinding getSocketBinding() {
            return socketBinding;
        }

        public String getProtocol() {
            return protocol;
        }
    }
}
