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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

import org.jboss.as.network.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Set up JSR-160 connector
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jason T. Greene
 */
public class JMXConnectorService implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("mbean", "connector");
    private static final String SERVER_HOSTNAME = "java.rmi.server.hostname";
    private static final String RMI_BIND_NAME = "jmxrmi";

    private static final int BACKLOG = 50;

    private final Logger log = Logger.getLogger(JMXConnectorService.class);
    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<SocketBinding> registryPortBinding = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> serverPortBinding = new InjectedValue<SocketBinding>();

    private String passwordFile;
    private String accessFile;

    private RMIConnectorServer adapter;
    private RMIJRMPServerImpl rmiServer;
    private Registry registry;

    public static ServiceController<?> addService(final ServiceTarget target, final String serverBinding, final String registryBinding, final String passwordFile, final String accessFile, final ServiceListener<Object>... listeners) {
        JMXConnectorService jmxConnectorService = new JMXConnectorService();
        jmxConnectorService.passwordFile = passwordFile;
        jmxConnectorService.accessFile = accessFile;

        return target.addService(JMXConnectorService.SERVICE_NAME, jmxConnectorService)
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, jmxConnectorService.getMBeanServerServiceInjector())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(registryBinding), SocketBinding.class, jmxConnectorService.getRegistryPortBinding())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(serverBinding), SocketBinding.class, jmxConnectorService.getServerPortBinding())
                .addListener(listeners)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.info("Starting remote JMX connector");
        setRmiServerProperty(serverPortBinding.getValue().getAddress().getHostAddress());

        if((passwordFile != null && accessFile == null)||(passwordFile==null&&accessFile!=null)) {
            throw new StartException("Need both password-file and access-file to start in secure mode");
        }

        try {
            SocketBinding registryBinding = registryPortBinding.getValue();
            RMIServerSocketFactory registrySocketFactory = new JMXServerSocketFactory(registryBinding);
            SocketBinding rmiServerBinding = serverPortBinding.getValue();
            RMIServerSocketFactory serverSocketFactory = new JMXServerSocketFactory(rmiServerBinding);

            registry = LocateRegistry.createRegistry(getRmiRegistryPort(), null, registrySocketFactory);
            HashMap<String, Object> env = new HashMap<String, Object>();

            if(passwordFile != null) {
                // Provide the password file used by the connector server to
                // perform user authentication. The password file is a properties
                // based text file specifying username/password pairs.
                //
                env.put("jmx.remote.x.password.file", replaceEnvVars(passwordFile));

                // Provide the access level file used by the connector server to
                // perform user authorization. The access level file is a properties
                // based text file specifying username/access level pairs where
                // access level is either "readonly" or "readwrite" access to the
                // MBeanServer operations.
                //
                env.put("jmx.remote.x.access.file", replaceEnvVars(accessFile));
            }

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
        private final SocketBinding socketBinding;

        public JMXServerSocketFactory(final SocketBinding socketBinding) {
            this.socketBinding = socketBinding;
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            int fixed = socketBinding.isFixedPort() ? 0 : 1;
            int configuredPort = socketBinding.getPort() + (socketBinding.getSocketBindings().getPortOffset() * fixed);
            if (port != configuredPort) {
                throw new IllegalStateException(String.format("Received request for server socket %s on port [%d] but am configured for port [%d]",
                        socketBinding.getName(), port, configuredPort));
            }
            return socketBinding.createServerSocket(BACKLOG);
        }
    }

    private void setRmiServerProperty(final String address) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    System.setProperty(SERVER_HOSTNAME, address);
                    return null;
                }
            });
        } else {
            System.setProperty(SERVER_HOSTNAME, address);
        }
    }

    private String replaceEnvVars(String attrValue) {
        StringBuilder src = new StringBuilder(attrValue);
        StringBuilder res = new StringBuilder();
        while(src.length() > 0) {
            int i = src.indexOf("${");
            if(i > 0) {
                res.append(src.substring(0, i));
                src.delete(0, i);
            } else if(i < 0) {
                if(src.length() > 0) {
                    res.append(src);
                    src.delete(0, src.length());
                }
            } else {
                int j = src.indexOf("}");
                String envVar = src.substring(2, j);
                String value = System.getProperty(envVar);
                if(value == null) {
                    value = System.getenv(envVar);
                }
                if(value != null) {
                    res.append(value);
                }
                src.delete(0, j+1);
            }
        }
        return res.toString();
    }
}
