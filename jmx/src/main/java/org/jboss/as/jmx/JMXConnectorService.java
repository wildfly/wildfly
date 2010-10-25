/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jmx;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

import org.jboss.as.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Set up JSR-160 connector
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jason T. Greene
 * @version $Revision: 1.1 $
 */
public class JMXConnectorService implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("mbean", "connector");
    private static final String RMI_BIND_NAME = "jmxrmi";

    private static final int BACKLOG = 50;

    private final Logger log = Logger.getLogger(JMXConnectorService.class);
    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<SocketBinding> registryPortBinding = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> serverPortBinding = new InjectedValue<SocketBinding>();

    private RMIConnectorServer adapter;
    private RMIJRMPServerImpl rmiServer;
    private Registry registry;

    public static void addService(final BatchBuilder batchBuilder) {
        JMXConnectorService jmxConnectorService = new JMXConnectorService();
        BatchServiceBuilder<Void> serviceBuilder = batchBuilder.addService(JMXConnectorService.SERVICE_NAME, jmxConnectorService);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, jmxConnectorService.getMBeanServerServiceInjector());
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("jmx-connector-registry"), SocketBinding.class, jmxConnectorService.getRegistryPortBinding());
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("jmx-connector-server"), SocketBinding.class, jmxConnectorService.getServerPortBinding());
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.info("Starting remote JMX connector");
        try {
            RMIServerSocketFactory serverSocketFactory = new JMXServerSocketFactory(getRmiRegistryAddress());

            registry = LocateRegistry.createRegistry(getRmiRegistryPort(), null, serverSocketFactory);
            HashMap<String, Object> env = new HashMap<String, Object>();

            rmiServer = new RMIJRMPServerImpl(getRmiServerPort(), null, serverSocketFactory, env);
            JMXServiceURL url = buildJMXServiceURL();
            adapter = new RMIConnectorServer(url, env, rmiServer, injectedMBeanServer.getValue());
            adapter.start();
            registry.rebind(RMI_BIND_NAME, rmiServer.toStub());
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    public void stop(StopContext context) {
        try {
            registry.unbind(RMI_BIND_NAME);
        } catch (Exception e) {
            log.error("Could not unbind jmx connector from registry", e);
        } finally {
            try {
                adapter.stop();
            } catch (Exception e) {
                log.error("Could not stop connector server", e);
            } finally {
                try {
                    UnicastRemoteObject.unexportObject(registry, true);
                } catch (Exception e) {
                    log.error("Could not shutdown rmi registry");
                }
            }
        }

        log.info("JMX remote connector stopped");
    }

    private JMXServiceURL buildJMXServiceURL() throws MalformedURLException {
        String host = getRmiRegistryAddressString();
        if (host.indexOf(':') != -1) { // is this a IPV6 literal address? if yes, surround with square brackets
                                       // as per rfc2732.
                                       // IPV6 literal addresses have one or more colons
                                       // IPV4 addresses/hostnames have no colons
            host = "[" + host + "]";

        }
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + host);
        return url;
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public InjectedValue<MBeanServer> getMBeanServerServiceInjector() {
        return injectedMBeanServer;
    }

    public InjectedValue<SocketBinding> getRegistryPortBinding() {
        return registryPortBinding;
    }

    public InjectedValue<SocketBinding> getServerPortBinding() {
        return serverPortBinding;
    }

    private int getRmiRegistryPort() {
        return registryPortBinding.getValue().getSocketAddress().getPort();
    }

    private int getRmiServerPort() {
        return serverPortBinding.getValue().getSocketAddress().getPort();
    }

    private InetAddress getRmiRegistryAddress() {
        return registryPortBinding.getValue().getSocketAddress().getAddress();
    }

    private String getRmiRegistryAddressString() {
        return registryPortBinding.getValue().getSocketAddress().getAddress().getHostAddress();
    }

    private static class JMXServerSocketFactory implements RMIServerSocketFactory, Serializable {
        private static final long serialVersionUID = 1564081885379700777L;
        private final InetAddress address;

        public JMXServerSocketFactory(InetAddress address) {
            this.address = address;
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port, BACKLOG, address);
        }
    }
}
