package org.jboss.as.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.BroadcastGroupConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.journal.impl.AIOSequentialFileFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.jboss.as.network.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service configuring and starting the {@code HornetQService}.
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 */
class HornetQService implements Service<HornetQServer> {
    private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

    /** */
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String LOGGING_FACTORY = "org.jboss.as.messaging.HornetQLoggerFactory";

    /**
     * The name of the SocketBinding reference to use for HOST/PORT
     * configuration
     */
    private static final String SOCKET_REF = CommonAttributes.SOCKET_BINDING.getName();

    private Configuration configuration;

    private HornetQServer server;
    private Map<String, String> paths = new HashMap<String, String>();
    private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();
    private Map<String, SocketBinding> groupBindings = new HashMap<String, SocketBinding>();
    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();

    Injector<String> getPathInjector(String name) {
        return new MapInjector<String, String>(paths, name);
    }

    Injector<SocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(socketBindings, name);
    }

    Injector<SocketBinding> getGroupBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(groupBindings, name);
    }

    InjectedValue<MBeanServer> getMBeanServer() {
        return mbeanServer;
    }

    public synchronized void start(final StartContext context) throws StartException {
        ClassLoader origTCCL = SecurityActions.getContextClassLoader();
        // Validate whether the AIO native layer can be used
        JournalType jtype = configuration.getJournalType();
        if (jtype == JournalType.ASYNCIO) {
            boolean supportsAIO = AIOSequentialFileFactory.isSupported();

            if (supportsAIO == false) {
                log.warn("AIO wasn't located on this platform, it will fall back to using pure Java NIO. If your platform is Linux, install LibAIO to enable the AIO journal");
                configuration.setJournalType(JournalType.NIO);
            }
        }

        // Disable file deployment
        configuration.setFileDeploymentEnabled(false);
        // Setup Logging
        configuration.setLogDelegateFactoryClassName(LOGGING_FACTORY);
        // Setup paths
        configuration.setBindingsDirectory(paths.get("bindings"));
        configuration.setLargeMessagesDirectory(paths.get("largemessages"));
        configuration.setJournalDirectory(paths.get("journal"));
        configuration.setPagingDirectory(paths.get("paging"));

        // FIXME securityEnabled be true? (true is the default) and we
        // should use the constructor that takes a SecurityManager
        configuration.setSecurityEnabled(false);

        try {
            // Update the acceptor/connector port/host values from the
            // Map the socket bindings onto the connectors/acceptors
            Collection<TransportConfiguration> acceptors = configuration.getAcceptorConfigurations();
            Collection<TransportConfiguration> connectors = configuration.getConnectorConfigurations().values();
            Collection<BroadcastGroupConfiguration> broadcastGroups = configuration.getBroadcastGroupConfigurations();
            Map<String, DiscoveryGroupConfiguration> discoveryGroups = configuration.getDiscoveryGroupConfigurations();
            if (connectors != null) {
                for (TransportConfiguration tc : connectors) {
                    // If there is a socket binding set the HOST/PORT values
                    Object socketRef = tc.getParams().remove(SOCKET_REF);
                    if (socketRef != null) {
                        String name = socketRef.toString();
                        SocketBinding binding = socketBindings.get(name);
                        if (binding == null) {
                            throw new StartException("Failed to find SocketBinding for connector: " + tc.getName());
                        }
                        tc.getParams().put(HOST, binding.getSocketAddress().getHostName());
                        tc.getParams().put(PORT, "" + binding.getSocketAddress().getPort());
                    }
                }
            }
            if (acceptors != null) {
                for (TransportConfiguration tc : acceptors) {
                    // If there is a socket binding set the HOST/PORT values
                    Object socketRef = tc.getParams().remove(SOCKET_REF);
                    if (socketRef != null) {
                        String name = socketRef.toString();
                        SocketBinding binding = socketBindings.get(name);
                        if (binding == null) {
                            throw new StartException("Failed to find SocketBinding for connector: " + tc.getName());
                        }
                        tc.getParams().put(HOST, binding.getSocketAddress().getHostName());
                        tc.getParams().put(PORT, "" + binding.getSocketAddress().getPort());
                    }
                }
            }
            if(broadcastGroups != null) {
                final List<BroadcastGroupConfiguration> newConfigs = new ArrayList<BroadcastGroupConfiguration>();
                for(final BroadcastGroupConfiguration config : broadcastGroups) {
                    final String name = config.getName();
                    final SocketBinding binding = groupBindings.get("broadcast" + name);
                    if (binding == null) {
                        throw new StartException("Failed to find SocketBinding for broadcast binding: " + name);
                    }
                    newConfigs.add(BroadcastGroupAdd.createBroadcastGroupConfiguration(name, config, binding));
                }
            }
            if(discoveryGroups != null) {
                configuration.setDiscoveryGroupConfigurations(new HashMap<String, DiscoveryGroupConfiguration>());
                for(final Map.Entry<String, DiscoveryGroupConfiguration> entry : discoveryGroups.entrySet()) {
                    final String name = entry.getKey();
                    final SocketBinding binding = groupBindings.get("discovery" + name);
                    if (binding == null) {
                        throw new StartException("Failed to find SocketBinding for discovery binding: " + entry.getKey());
                    }
                    final DiscoveryGroupConfiguration config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, entry.getValue(), binding);
                    configuration.getDiscoveryGroupConfigurations().put(name, config);
                }
            }

            // Now start the server
            server = new HornetQServerImpl(configuration, mbeanServer.getOptionalValue(), null);

            // FIXME started by the JMSService
            // HornetQ expects the TCCL to be set to something that can find the
            // log factory class.
            // ClassLoader loader = getClass().getClassLoader();
            // SecurityActions.setContextClassLoader(loader);
            // server.start();
        } catch (Exception e) {
            throw new StartException("Failed to start service", e);
        } finally {
            SecurityActions.setContextClassLoader(origTCCL);
        }
    }

    public synchronized void stop(final StopContext context) {
        try {
            if (server != null) {
                // FIXME stopped by the JMSService
                // server.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to shutdown HornetQ server", e);
        }
    }

    public synchronized HornetQServer getValue() throws IllegalStateException {
        final HornetQServer server = this.server;
        if (server == null) {
            throw new IllegalStateException();
        }
        return server;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration hqConfig) {
        this.configuration = hqConfig;
    }
}
