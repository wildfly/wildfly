package org.jboss.as.messaging.hornetq;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.journal.impl.AIOSequentialFileFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class HornetQService implements Service<HornetQServer> {
    private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

    /** */
    private static final String HOST = "host";
    private static final String PORT = "port";
    /**
     * The name of the SocketBinding reference to use for HOST/PORT
     * configuration
     */
    private static final String SOCKET_REF = "socket-ref";

    private Configuration configuration;

    private HornetQServer server;
    private Map<String, String> paths = new HashMap<String, String>();
    private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();

    public Injector<String> getPathInjector(String name) {
        return new PathInjector(name);
    }

    public Injector<SocketBinding> getSocketBindingInjector(String name) {
        SocketBindingInjector injector = new SocketBindingInjector(name);
        return injector;
    }

    /**
     * {@inheritDoc}
     */
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

        // Setup paths
        configuration.setBindingsDirectory(paths.get("bindings"));
        configuration.setLargeMessagesDirectory("largemessages");
        configuration.setJournalDirectory(paths.get("journal"));
        configuration.setPagingDirectory(paths.get("paging"));

        try {
            // Update the acceptor/connector port/host values from the
            // Map the socket bindings onto the connectors/acceptors
            Collection<TransportConfiguration> acceptors = configuration.getAcceptorConfigurations();
            Collection<TransportConfiguration> connectors = configuration.getConnectorConfigurations().values();
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

            // Now start the server
            server = new HornetQServerImpl(configuration);
            // HornetQ expects the TCCL to be set to something that can find the
            // log factory class.
            ClassLoader loader = getClass().getClassLoader();
            SecurityActions.setContextClassLoader(loader);
            log.info("Starting the HornetQServer...");
            server.start();
        } catch (Exception e) {
            throw new StartException("Failed to start service", e);
        } finally {
            SecurityActions.setContextClassLoader(origTCCL);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stop(final StopContext context) {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to shutdown HornetQ server", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized HornetQServer getValue() throws IllegalStateException {
        if (server == null)
            throw new IllegalStateException();
        return server;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration hqConfig) {
        this.configuration = hqConfig;
    }

    /**
     * An injector that manages a name/SocketBinding pair mapping in
     * #socketBindings
     */
    class SocketBindingInjector implements Injector<SocketBinding>, Value<SocketBinding> {
        private String name;
        private SocketBinding value;

        SocketBindingInjector(String name) {
            this.name = name;
        }

        @Override
        public void inject(SocketBinding value) throws InjectionException {
            this.value = value;
            socketBindings.put(name, value);
        }

        @Override
        public void uninject() {
            socketBindings.remove(name);
        }

        @Override
        public SocketBinding getValue() throws IllegalStateException {
            return value;
        }
    }

    class PathInjector implements Injector<String>, Value<String> {
        private final String name;
        private String value;

        public PathInjector(String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        public String getValue() throws IllegalStateException {
            return value;
        }

        /** {@inheritDoc} */
        public void inject(String value) throws InjectionException {
            HornetQService.this.paths.put(name, value);
        }

        /** {@inheritDoc} */
        public void uninject() {
            HornetQService.this.paths.remove(name);
        }
    }
}
