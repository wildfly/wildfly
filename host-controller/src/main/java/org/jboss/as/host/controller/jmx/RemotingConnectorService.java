/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.host.controller.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remotingjmx.RemotingConnectorServer;

/**
 * The remote connector services
 *
 * Copied from the JMX subsystem to avoid bringing in a new dependency.
 *
 * @author Stuart Douglas
 */
public class RemotingConnectorService implements Service<RemotingConnectorServer> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jmx", "remoting-connector-ref");

    private RemotingConnectorServer server;

    private final InjectedValue<Endpoint> endpoint = new InjectedValue<Endpoint>();

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        server = new RemotingConnectorServer(ManagementFactory.getPlatformMBeanServer(), endpoint.getValue());
        try {
            server.start();
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        try {
            server.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized RemotingConnectorServer getValue() throws IllegalStateException, IllegalArgumentException {
        return server;
    }

    public static ServiceController<?> addService(final ServiceTarget target,
            final ServiceVerificationHandler verificationHandler) {
        final RemotingConnectorService service = new RemotingConnectorService();
        final ServiceBuilder<RemotingConnectorServer> builder = target.addService(SERVICE_NAME, service);

        builder.addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT, Endpoint.class, service.endpoint);

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        return builder.install();
    }

}
