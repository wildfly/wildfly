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

/**
 *
 */
package org.jboss.as.server.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.LocalDomainControllerElement;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.RemoteDomainControllerElement;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.socket.ServerInterfaceElement;
import org.jboss.as.process.ProcessManagerSlave;
import org.jboss.as.process.RespawnPolicy;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLMapper;

/**
 * A ServerManager.
 *
 * @author Brian Stansberry
 */
public class ServerManager {

    private static final Logger log = Logger.getLogger("org.jboss.server.manager");

    private final ServerManagerEnvironment environment;
    private final StandardElementReaderRegistrar extensionRegistrar;
    private final File hostXML;
    private final MessageHandler messageHandler;
    private ProcessManagerSlave processManagerSlave;
    private HostModel hostConfig;
    private DomainModel domainConfig;
    private DirectServerCommunicationListener directServerCommunicationListener;
    private DomainControllerProcess localDomainControllerProcess;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
    private final AtomicBoolean serversStarted = new AtomicBoolean();

    // TODO figure out concurrency controls
//    private final Lock hostLock = new ReentrantLock();
//    private final Lock domainLock = new ReentrantLock();
    private final Map<String, Server> servers = Collections.synchronizedMap(new HashMap<String, Server>());

    ShutdownLatch shutdownLatch;

    public ServerManager(ServerManagerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        this.hostXML = new File(environment.getDomainConfigurationDir(), "host.xml");
        this.extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
        this.messageHandler = new MessageHandler(this);
    }

    /**
     * Starts the ServerManager. This brings this ServerManager to the point where
     * it has processed it's own configuration file, registered with the DomainController
     * (including starting one if the host configuration specifies that),
     * obtained the domain configuration, and launched any systems needed to make
     * this process manageable by remote clients.
     */
    public void start() {

        this.hostConfig = parseHost();

        // TODO set up logging for this process based on config in Host

        //Start listening for server communication on our socket
        launchDirectServerCommunicationHandler();

        // Start communication with the ProcessManager. This also
        // creates a daemon thread to keep this process alive
        launchProcessManagerSlave();

        if (hostConfig.getLocalDomainControllerElement() != null) {
            initiateDomainController();
        } else {
            // DC does not know about this host. Inform it of our existence
            registerWithDomainController();
        }
    }

    public void startServers() {

        // TODO figure out concurrency controls
//        hostLock.lock(); // should this be domainLock?
//        try {
        CommunicationVariables variables = new CommunicationVariables(environment, directServerCommunicationListener);
        ServerMaker serverMaker = new ServerMaker(environment, processManagerSlave, messageHandler, variables);
        for (ServerElement serverEl : hostConfig.getServers()) {
            // TODO take command line input on what servers to start
            if (serverEl.isStart()) {
                log.info("Starting server " + serverEl.getName());
                ServerModel serverConf = new ServerModel(domainConfig, hostConfig, serverEl.getName());
                JvmElement jvmElement = getServerJvmElement(domainConfig, hostConfig, serverEl.getName());
                try {

                    Server server = serverMaker.makeServer(serverConf, jvmElement, getRespawnPolicy(serverConf));
                    servers.put(getServerProcessName(serverConf), server);

                    //Now that the server is in the servers map we can start it
                    processManagerSlave.startProcess(server.getServerProcessName());
                } catch (IOException e) {
                    // FIXME handle failure to start server
                    log.error("Failed to start server " + serverEl.getName(), e);
                }
            }
            else log.info("Server " + serverEl.getName() + " is configured to not be started");
        }
//        }
//        finally {
//            hostLock.unlock();
//        }
    }

    private RespawnPolicy getRespawnPolicy(ServerModel serverConf) {
        //TODO JBAS-8390 Read respawn policy from host or domain?
        return RespawnPolicy.DefaultRespawnPolicy.INSTANCE;
    }

    /**
     * Sends the RECONNECT_SERVERS <SM_ADDRESS> <SM_PORT> command to the
     * servers following a restart of the server manager
     */
    private void reconnectServers() {
        try {
            processManagerSlave.reconnectServers(directServerCommunicationListener.getSmAddress(), directServerCommunicationListener.getSmPort());
        } catch (IOException e) {
            log.error("Failed to send RECONNECT_SERVERS", e);
        }
    }

    /**
     * Callback for when we receive the SERVER_AVAILABLE message from a Server
     *
     * @param serverName the name of the server
     */
    void availableServer(String serverName) {
        try {
            Server server = servers.get(serverName);
            if (server == null) {
                log.errorf("No server called %s available", serverName);
                return;
            }
            checkState(server, ServerState.BOOTING);

            server.setState(ServerState.AVAILABLE);
            log.infof("Sending config to server %s", serverName);
            server.start();
            server.setState(ServerState.STARTING);
        } catch (IOException e) {
            log.errorf(e, "Could not start server %s", serverName);
        }
    }

    /**
     * Callback for when we receive the SHUTDOWN message from PM
     */
    public void stop() {
        directServerCommunicationListener.shutdown();
        // FIXME stop any local DomainController, stop other internal SM services
    }

    /**
     * Callback for when we receive the SHUTDOWN_SERVERS message from PM
     *
     * @param serverName the name of the server
     */
    public void shutdownServers() {
        log.info("Shutting down servers");
        System.out.println();
        if (shutdownLatch != null) {
            throw new IllegalStateException("Already shutting down");
        }
        shutdownLatch = new ShutdownLatch();

        Map<String, Server> serversCopy;
        synchronized (servers) {
            serversCopy = new HashMap<String, Server>(servers);
        }

        for (Map.Entry<String, Server> entry : serversCopy.entrySet()) {
            try {
                Server server = entry.getValue();
                if (server.getState() == ServerState.STOPPED)
                    continue;
                shutdownLatch.add(entry.getKey());
                log.infof("Stopping %s", server.getServerProcessName());
                server.stop();
                server.setState(ServerState.STOPPING);
            }
            catch (Exception e) {
                // FIXME handle exception stopping server
            }
        }
        shutdownLatch.addedAll();
        try {
            shutdownLatch.waitForAll();
        } catch (InterruptedException e) {
            log.errorf("Wait for server shutdown was interrupted");
        }

        try {
            processManagerSlave.serversShutdown();
        } catch (IOException e) {
            log.error("Error confiriming servers shutdown", e);
        }
    }



    /**
     * Callback for when we receive the SERVER_STOPPED message from a Server
     *
     * @param serverName the name of the server
     */
    void stoppedServer(String serverName) {
        Server server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists", serverName);
            return;
        }
        checkState(server, ServerState.STOPPING);
        shutdownLatch.remove(serverName);

        try {
            processManagerSlave.stopProcess(serverName);
            processManagerSlave.removeProcess(serverName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback for when we receive the SERVER_STARTED message from a Server
     *
     * @param serverName the name of the server
     */
    void startedServer(String serverName) {
        Server server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists", serverName);
            return;
        }
        checkState(server, ServerState.STARTING);
        server.setState(ServerState.STARTED);
    }

    /**
     * Callback for when we receive the SERVER_START_FAILED message from a Server
     *
     * @param serverName the name of the server
     */
    void failedStartServer(String serverName) {
        Server server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists", serverName);
            return;
        }
        checkState(server, ServerState.STARTING);
        server.setState(ServerState.FAILED);
        respawn(server);
    }

    void reconnectedServer(String serverName, ServerState state) {
        Server server = servers.get(serverName);
        if (server == null) {
            DirectServerCommunicationHandler handler = directServerCommunicationListener.getHandlerWaitingForReconnect(serverName);
            if (handler == null) {
                log.errorf("No server and no connection found for reconnected server %s", serverName);
                return;
            }

            ServerElement svr = null;
            for (ServerElement serverEl : hostConfig.getServers()) {
                if (getServerProcessName(serverEl.getName()).equals(serverName)) {
                    svr = serverEl;
                    break;
                }
            }
            if (svr == null) {
                log.errorf("No config found for reconnected server %s");
                return;
            }

            ServerModel serverConf = new ServerModel(domainConfig, hostConfig, svr.getName());
            JvmElement jvmElement = getServerJvmElement(domainConfig, hostConfig, svr.getName());
            CommunicationVariables variables = new CommunicationVariables(environment, directServerCommunicationListener);
            ServerMaker serverMaker = new ServerMaker(environment, processManagerSlave, messageHandler, variables);
            try {
                server = serverMaker.makeServer(serverConf, jvmElement, getRespawnPolicy(serverConf));
            } catch (IOException e) {
                log.errorf(e, "Could not create a Server instance for reconnected server %s", serverName);
                return;
            }
            servers.put(getServerProcessName(serverConf), server);
            server.setCommunicationHandler(handler);
        }
        server.setState(state);

        if (state.isRestartOnReconnect()) {
            try {
                server.start();
            } catch (IOException e) {
                log.errorf(e, "Could not start reconnected server %s", server.getServerProcessName());
            }
            server.setState(ServerState.STARTING);
        }
    }

    private void respawn(Server server){
        try {
            processManagerSlave.stopProcess(server.getServerProcessName());
        } catch (IOException e) {
            log.errorf(e, "Error respawning server % s", server.getServerProcessName());
        }

        RespawnPolicy respawnPolicy = server.getRespawnPolicy();
        long timeout = respawnPolicy.getTimeOutMs(server.incrementAndGetRespawnCount());
        if (timeout < 0 ) {
            server.setState(ServerState.MAX_FAILED);
            try {
                processManagerSlave.removeProcess(server.getServerProcessName());
            } catch (IOException e) {
                log.errorf(e, "Error stopping respawned server % s", server.getServerProcessName());
            }
            return;
        }

        //TODO JBAS-8390 Put in actual sleep
        //Thread.sleep(timeout);

        try {
            server.setState(ServerState.BOOTING);
            processManagerSlave.startProcess(server.getServerProcessName());
        } catch (IOException e) {
            log.errorf(e, "Error respawning server % s", server.getServerProcessName());
        }
    }

    public void downServer(String downServerName) {
        Server server = servers.get(downServerName);
        if (server == null) {
            log.errorf("No server called %s exists", downServerName);
            return;
        }
        server.setState(ServerState.FAILED);
        respawn(server);
    }

    private void launchProcessManagerSlave() {
        this.processManagerSlave = ProcessManagerSlaveFactory.getInstance().getProcessManagerSlave(environment, hostConfig, messageHandler);
        Thread t = new Thread(this.processManagerSlave.getController(), "Server Manager Process");
        t.start();
    }

    private void launchDirectServerCommunicationHandler() {
        try {
            this.directServerCommunicationListener = DirectServerCommunicationListener.create(this, environment.getServerManagerAddress(), environment.getServerManagerPort(), 20);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerWithDomainController() {
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final ServiceActivatorContext activatorContext = new ServiceActivatorContextImpl(batchBuilder);
        final DomainControllerClientService domainControllerClientService = new DomainControllerClientService(this);
        final BatchServiceBuilder<Void> serviceBuilder = batchBuilder.addService(DomainControllerClientService.SERVICE_NAME, domainControllerClientService)
            .addListener(new AbstractServiceListener<Void>() {
                @Override
                public void serviceFailed(ServiceController<? extends Void> serviceController, StartException reason) {
                    log.errorf("Failed to register with domain controller.", reason);
                }
            })
            .setInitialMode(ServiceController.Mode.IMMEDIATE);

        final LocalDomainControllerElement localDomainControllerElement = hostConfig.getLocalDomainControllerElement();
        if(localDomainControllerElement != null) {
            final String interfaceName = localDomainControllerElement.getInterfaceName();
            ServerInterfaceElement interfaceElement = null;
            // Activate admin interface
            final Map<String, ServerInterfaceElement> dcInterfaces = localDomainControllerElement.getInterfaces();
            if(dcInterfaces != null) {
                interfaceElement = dcInterfaces.get(interfaceName);
            }

            final Set<ServerInterfaceElement> hostInterfaces = hostConfig.getInterfaces();
            if(interfaceElement == null && hostInterfaces != null) {
                for(ServerInterfaceElement hostInterfaceElement : hostInterfaces) {
                    if(interfaceName.equals(hostInterfaceElement.getName())) {
                        interfaceElement = hostInterfaceElement;
                        break;
                    }
                }
            }
            if(interfaceElement != null) {
                interfaceElement.activate(activatorContext);
            }
            serviceBuilder.addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName), NetworkInterfaceBinding.class, domainControllerClientService.getDomainControllerInterface());
            serviceBuilder.addInjection(domainControllerClientService.getDomainControllerPortInjector(), localDomainControllerElement.getPort());
        } else {
            final RemoteDomainControllerElement remoteDomainControllerElement = hostConfig.getRemoteDomainController();
            final InetAddress hostAddress;
            try {
                hostAddress = InetAddress.getByName(remoteDomainControllerElement.getHost());
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to get remote domain controller address", e);
            }
            serviceBuilder.addInjection(domainControllerClientService.getDomainControllerAddressInjector(), hostAddress);
            serviceBuilder.addInjection(domainControllerClientService.getDomainControllerPortInjector(), remoteDomainControllerElement.getPort());
        }
        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new RuntimeException(e);
        }
    }

    private void initiateDomainController() {
        final LocalDomainControllerElement localDomainControllerElement = hostConfig.getLocalDomainControllerElement();
        CommunicationVariables variables = new CommunicationVariables(environment, directServerCommunicationListener);
        final ServerMaker serverMaker = new ServerMaker(environment, processManagerSlave, messageHandler, variables);
        final DomainControllerConfig config = new DomainControllerConfig(localDomainControllerElement, hostConfig);
        try {
            log.info("Starting local domain controller");
            localDomainControllerProcess = serverMaker.makeDomainController(getDomainControllerJvmElement(hostConfig, localDomainControllerElement.getJvm()));
            localDomainControllerProcess.start(config);
        } catch (IOException e) {
            log.error("Failed to start domain controller server", e);
        }
        //this.domainConfig = parseDomain();
    }

    void localDomainControllerReady() {
        registerWithDomainController();
    }

    HostModel getHostConfig() {
        return hostConfig;
    }

    private HostModel parseHost() {

        if (!hostXML.exists()) {
            throw new IllegalStateException("File " + hostXML.getAbsolutePath() + " does not exist.");
        }
        else if (! hostXML.canWrite()) {
            throw new IllegalStateException("File " + hostXML.getAbsolutePath() + " is not writeable.");
        }

        try {
            XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardHostReaders(mapper);
            ParseResult<HostModel> parseResult = new ParseResult<HostModel>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(this.hostXML))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of host.xml", e);
        }
    }

    /**
     * Set the domain for the server manager.  If this is the first time the domain has been set on this instance it will
     * also invoke the server launch process.
     *
     * @param domain The domain configuration
     */
    public void setDomain(final DomainModel domain) {
        this.domainConfig = domain;
        if(serversStarted.compareAndSet(false, true)) {
            if (!environment.isRestart()) {
                startServers();
            } else {
                reconnectServers();
            }
        }
    }

    /**
     * Combines information from the domain, server group, host and server levels
     * to come up with an overall JVM configuration for a server.
     *
     * @param domain the domain configuration object
     * @param host the host configuration object
     * @param serverName the name of the server
     * @return the JVM configuration object
     */
    private JvmElement getServerJvmElement(DomainModel domain, HostModel host, String serverName) {

        ServerElement server = host.getServer(serverName);
        if (server == null)
            throw new IllegalStateException("Server " + serverName + " is not listed in Host");

        String serverGroupName = server.getServerGroup();
        ServerGroupElement serverGroup = domain.getServerGroup(serverGroupName);
        if (serverGroup == null)
            throw new IllegalStateException("Server group" + serverGroupName + " is not listed in Domain");

        JvmElement serverVM = server.getJvm();
        String serverVMName = serverVM != null ? serverVM.getName() : null;

        JvmElement groupVM = serverGroup.getJvm();
        String groupVMName = groupVM != null ? groupVM.getName() : null;

        String ourVMName = serverVMName != null ? serverVMName : groupVMName;
        if (ourVMName == null) {
            throw new IllegalStateException("Neither " + Element.SERVER_GROUP.getLocalName() +
                    " nor " + Element.SERVER.getLocalName() + " has declared a JVM configuration; one or the other must");
        }

        if (!ourVMName.equals(groupVMName)) {
            // the server setting replaced the group, so ignore group
            groupVM = null;
        }
        JvmElement hostVM = host.getJvm(ourVMName);

        return new JvmElement(groupVM, hostVM, serverVM);
    }

    /**
     * Combines information from the domain controller element, and host to come up with an overall JVM configuration
     * for the domain controller
     *
     * @param host the host configuration object
     * @param dcJvmElement the domain controller jvm element
     * @return the JVM configuration object
     */
    private JvmElement getDomainControllerJvmElement(final HostModel host, final JvmElement dcJvmElement) {
        final JvmElement hostVM = host.getJvm(dcJvmElement.getName());
        if(hostVM == null) {
            return dcJvmElement;
        }
        return new JvmElement(hostVM, dcJvmElement);
    }

    Server getServer(String name) {
        return servers.get(name);
    }

    static String getServerProcessName(ServerModel serverConfig) {
        return getServerProcessName(serverConfig.getServerName());
    }

    static String getServerProcessName(String name) {
        return ServerMaker.SERVER_PROCESS_NAME_PREFIX + name;
    }

    private void checkState(Server server, ServerState expected) {
        ServerState state = server.getState();
        if (state != expected) {
            log.warnf("Server %s is not in the expected %s state: %s" , server.getServerProcessName(), expected, state);
        }
    }

    public Map<String, Server> getServers() {
        synchronized (servers) {
            return Collections.unmodifiableMap(servers);
        }
    }

    private static class ShutdownLatch {
        private final Set<String> waitingForServers = Collections.synchronizedSet(new HashSet<String>());
        private final CountDownLatch done = new CountDownLatch(1);
        private CountDownLatch waitFor;

        synchronized void add(String serverName) {
            waitingForServers.add(serverName);
        }

        synchronized void remove(String serverName) {
            if (!waitingForServers.remove(serverName)) {
                log.warnf("Could not find server called %s", serverName);
                return;
            }
            if (waitFor != null)
                waitFor.countDown();
        }

        synchronized void addedAll() {
            waitFor = new CountDownLatch(waitingForServers.size());
            done.countDown();
        }

        void waitForAll() throws InterruptedException{
            done.await();
            if (!waitFor.await(10, TimeUnit.SECONDS)) {
                log.warnf("Waited 10 seconds for confirmation from all servers they are closing down before giving up %s", waitingForServers);
            }
        }
    }
}
