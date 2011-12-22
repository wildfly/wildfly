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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.SaslException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.protocol.mgmt.ManagementMessageHandler;
import org.jboss.as.server.ServerState;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.jboss.sasl.util.UsernamePasswordHashUtil;

/**
 * Inventory of the managed servers.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class ServerInventoryImpl implements ServerInventory {

    private final Map<String, ManagedServer> servers = Collections.synchronizedMap(new HashMap<String, ManagedServer>());

    private final HostControllerEnvironment environment;
    private final ProcessControllerClient processControllerClient;
    private final InetSocketAddress managementAddress;
    private final DomainController domainController;
    private final Object shutdownCondition = new Object();
    private volatile CountDownLatch processInventoryLatch;
    private volatile Map<String, ProcessInfo> processInfos;
    private volatile boolean stopped;

    ServerInventoryImpl(final DomainController domainController, final HostControllerEnvironment environment, final InetSocketAddress managementAddress, final ProcessControllerClient processControllerClient) {
        this.domainController = domainController;
        this.environment = environment;
        this.managementAddress = managementAddress;
        this.processControllerClient = processControllerClient;
    }

    public String getServerProcessName(String serverName) {
        return ManagedServer.getServerProcessName(serverName);
    }

    public String getProcessServerName(String processName) {
        return ManagedServer.getServerName(processName);
    }

    public synchronized Map<String, ProcessInfo> determineRunningProcesses(){
        processInventoryLatch = new CountDownLatch(1);
        try {
            processControllerClient.requestProcessInventory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            if (!processInventoryLatch.await(30, TimeUnit.SECONDS)){
                throw MESSAGES.couldNotGetServerInventory(30L, TimeUnit.SECONDS.toString().toLowerCase(Locale.US));
            }
        } catch (InterruptedException e) {
        }
        return processInfos;
    }

    public Map<String, ProcessInfo> determineRunningProcesses(boolean serversOnly){
        Map<String, ProcessInfo> processInfos = determineRunningProcesses();
        if (!serversOnly) {
            return processInfos;
        }
        Map<String, ProcessInfo> processes = new HashMap<String, ProcessInfo>();
        for (Map.Entry<String, ProcessInfo> procEntry : processInfos.entrySet()) {
            if (ManagedServer.isServerProcess(procEntry.getKey())) {
                processes.put(procEntry.getKey(), procEntry.getValue());
            }
        }
        return processes;
    }

    @Override
    public void processInventory(Map<String, ProcessInfo> processInfos) {
        this.processInfos = processInfos;
        if (processInventoryLatch != null){
            processInventoryLatch.countDown();
        }
    }

    public ServerStatus determineServerStatus(final String serverName) {
        ServerStatus status;
        final String processName = ManagedServer.getServerProcessName(serverName);
        final ManagedServer client = servers.get(processName);
        if (client == null) {
            status = ServerStatus.STOPPED; // TODO move the configuration state outside
        } else {
            switch (client.getState()) {
                case AVAILABLE:
                case BOOTING:
                case STARTING:
                    status = ServerStatus.STARTING;
                    break;
                case FAILED:
                case MAX_FAILED:
                    status = ServerStatus.FAILED;
                    break;
                case STARTED:
                    status = ServerStatus.STARTED;
                    break;
                case STOPPING:
                    status = ServerStatus.STOPPING;
                    break;
                case STOPPED:
                    status = ServerStatus.STOPPED;
                    break;
                default:
                    throw MESSAGES.unexpectedState(client.getState());
            }
        }
        return status;
    }

    public ServerStatus startServer(final String serverName, final ModelNode domainModel) {
        final String processName = ManagedServer.getServerProcessName(serverName);
        final ManagedServer existing = servers.get(processName);
        if(existing != null) { // FIXME
            ROOT_LOGGER.existingServerWithState(processName, existing.getState());
            return determineServerStatus(serverName);
        }
        ROOT_LOGGER.startingServer(serverName);
        final ManagedServer server = createManagedServer(serverName, domainModel);
        synchronized (shutdownCondition) {
            servers.put(processName, server);
            shutdownCondition.notifyAll();
        }

        try {
            server.createServerProcess();
        } catch(IOException e) {
            ROOT_LOGGER.failedToCreateServerProcess(e, serverName);
        }
        try {
            server.startServerProcess();
        } catch(IOException e) {
            ROOT_LOGGER.failedToStartServer(e, serverName);
        }
        return determineServerStatus(serverName);
    }

    public void reconnectServer(final String serverName, final ModelNode domainModel, final boolean running){

        final String processName = ManagedServer.getServerProcessName(serverName);
        final ManagedServer existing = servers.get(processName);
        if(existing != null) { // FIXME
            ROOT_LOGGER.existingServerWithState(processName, existing.getState());
        }
        ROOT_LOGGER.reconnectingServer(serverName);
        final ManagedServer server = createManagedServer(serverName, domainModel);
        synchronized (shutdownCondition) {
            servers.put(processName, server);
            shutdownCondition.notifyAll();
        }

        if (running){
            try {
                server.reconnectServerProcess();
            } catch (IOException e) {
                ROOT_LOGGER.failedToSendReconnect(e, serverName);
            }
        }
    }

    public ServerStatus restartServer(String serverName, final int gracefulTimeout, final ModelNode domainModel) {
        stopServer(serverName, gracefulTimeout);
        ServerStatus status;
        // FIXME total hack; set up some sort of notification scheme
        for (int i = 0; i < 50; i++) {
            status = determineServerStatus(serverName);
            if (status == ServerStatus.STOPPING) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            else {
                break;
            }
        }
        return startServer(serverName, domainModel);
    }

    public ServerStatus stopServer(final String serverName, final int gracefulTimeout) {
        ROOT_LOGGER.stoppingServer(serverName);
        final String processName = ManagedServer.getServerProcessName(serverName);
        try {
            final ManagedServer server = servers.get(processName);
            if (server != null) {
                server.setState(ServerState.STOPPING);
                if (gracefulTimeout > -1) {
                    // FIXME implement gracefulShutdown
                    //server.gracefulShutdown(gracefulTimeout);
                    // FIXME figure out how/when server.removeServerProcess() && servers.remove(processName) happens

                    // Workaround until the above is fixed
                    ROOT_LOGGER.gracefulShutdownNotSupported(serverName);
                    server.stopServerProcess();
                    server.removeServerProcess();
                }
                else {
                    server.stopServerProcess();
                    server.removeServerProcess();
                }
            }
        }
        catch (final Exception e) {
            ROOT_LOGGER.failedToStopServer(e, serverName);
        }
        return determineServerStatus(serverName);
    }

    /** {@inheritDoc} */
    @Override
    public void serverRegistered(final String serverProcessName, final Channel channel, ProxyCreatedCallback callback) {
        try {
            final ManagedServer server = servers.get(serverProcessName);
            if (server == null) {
                ROOT_LOGGER.noServerAvailable(serverProcessName);
                return;
            }

            channel.addCloseHandler(new CloseHandler<Channel>() {
                public void handleClose(final Channel closed, final IOException exception) {
                    domainController.unregisterRunningServer(server.getServerName());
                }
            });

            if (!environment.isRestart()){
                checkState(server, ServerState.STARTING);
            }
            synchronized (shutdownCondition) {
                server.setState(ServerState.STARTED);
                shutdownCondition.notifyAll();
            }

            final PathElement element = PathElement.pathElement(RUNNING_SERVER, server.getServerName());
            final ProxyController serverController = RemoteProxyController.create(Executors.newCachedThreadPool(),
                    PathAddress.pathAddress(PathElement.pathElement(HOST, domainController.getLocalHostInfo().getLocalHostName()), element),
                    ProxyOperationAddressTranslator.SERVER,
                    channel);
            if (callback != null && serverController instanceof ManagementMessageHandler) {
                callback.proxyOperationHandlerCreated((ManagementMessageHandler)serverController);
            }
            domainController.registerRunningServer(serverController);

            server.resetRespawnCount();
        } catch (final Exception e) {
            ROOT_LOGGER.failedToStartServer(e, serverProcessName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void serverStartFailed(String serverProcessName) {
        final ManagedServer server = servers.get(serverProcessName);
        if (server == null) {
            ROOT_LOGGER.noServerAvailable(serverProcessName);
            return;
        }
        checkState(server, ServerState.STARTING);
        synchronized (shutdownCondition) {
            server.setState(ServerState.FAILED);
            shutdownCondition.notifyAll();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void serverStopped(String serverProcessName) {
        final ManagedServer server = servers.get(serverProcessName);
        if (server == null) {
            ROOT_LOGGER.noServerAvailable(serverProcessName);
            return;
        }
        domainController.unregisterRunningServer(server.getServerName());
        if (server.getState() != ServerState.STOPPING && ! stopped){
            //The server crashed, try to restart it
            // TODO: throttle policy
            try {
                //TODO make configurable
                if (server.incrementAndGetRespawnCount() < 10 ){
                    server.startServerProcess();
                    return;
                }
                server.setState(ServerState.MAX_FAILED);
            } catch(IOException e) {
                ROOT_LOGGER.failedToStopServer(e, serverProcessName);
            }
        }
        synchronized (shutdownCondition) {
            servers.remove(serverProcessName);
            shutdownCondition.notifyAll();
        }
    }

    public void stopServers(int gracefulTimeout) {
        stopServers(gracefulTimeout, false);
    }

    void stopServers(int gracefulTimeout, boolean blockUntilStopped) {
        stopped = true;
        Map<String, ProcessInfo> processInfoMap = determineRunningProcesses();
        for (String serverProcessName : processInfoMap.keySet()) {
            if (ManagedServer.isServerProcess(serverProcessName)) {
                String serverName = ManagedServer.getServerName(serverProcessName);
                stopServer(serverName, gracefulTimeout);
            }
        }

        if (blockUntilStopped) {
            synchronized (shutdownCondition) {
                for (;;) {
                    int count = 0;
                    try {
                        processInfoMap = determineRunningProcesses();
                    } catch(RuntimeException e) {
                        // In case the process-controller connection is broken, the PC most likely got killed
                        // and we won't receive any further notifications
                        return;
                    }
                    for (ManagedServer server : servers.values()) {
                        final ProcessInfo info = processInfoMap.get(server.getServerProcessName());
                        switch (server.getState()) {
                            case FAILED:
                            case MAX_FAILED:
                            case STOPPED:
                                break;
                            default:
                                // Only in case there is still a process registered and running
                                if(info != null && info.isRunning()) {
                                    count++;
                                }
                        }
                    }

                    if (count == 0) {
                        break;
                    }
                    try {
                        shutdownCondition.wait(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void checkState(final ManagedServer server, final ServerState expected) {
        final ServerState state = server.getState();
        if (state != expected) {
            ROOT_LOGGER.unexpectedServerState(server.getServerProcessName(), expected, state);
        }
    }

    private ManagedServer createManagedServer(final String serverName, final ModelNode domainModel) {
        final String hostControllerName = domainController.getLocalHostInfo().getLocalHostName();
        final ModelNode hostModel = domainModel.require(HOST).require(hostControllerName);
        final ModelCombiner combiner = new ModelCombiner(serverName, domainModel, hostModel, domainController, environment);
        return new ManagedServer(hostControllerName, serverName, processControllerClient, managementAddress, combiner);
    }

    public CallbackHandler getServerCallbackHandler() {
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                List<Callback> toRespondTo = new LinkedList<Callback>();

                String userName = null;
                String realm = null;
                ManagedServer server = null;

                // A single pass may be sufficient but by using a two pass approach the Callbackhandler will not
                // fail if an unexpected order is encountered.

                // First Pass - is to double check no unsupported callbacks and to retrieve
                // information from the callbacks passing in information.
                for (Callback current : callbacks) {

                    if (current instanceof AuthorizeCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) current;
                        userName = nameCallback.getDefaultName();

                        server = servers.get(ManagedServer.getServerProcessName(userName));
                    } else if (current instanceof PasswordCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof VerifyPasswordCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof DigestHashCallback) {
                        toRespondTo.add(current);
                    } else if (current instanceof RealmCallback) {
                        realm = ((RealmCallback)current).getDefaultText();
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }

                /*
                * At the moment this is a special CallbackHandler where we know the setting of a password will be double checked
                 * before going back to the base realm.
                */
                if (server == null) {
                    return;
                }

                final String password = new String(server.getAuthKey());

                // Second Pass - Now iterate the Callback(s) requiring a response.
                for (Callback current : toRespondTo) {
                    if (current instanceof AuthorizeCallback) {
                        AuthorizeCallback authorizeCallback = (AuthorizeCallback) current;
                        // Don't support impersonating another identity
                        authorizeCallback.setAuthorized(authorizeCallback.getAuthenticationID().equals(authorizeCallback.getAuthorizationID()));
                    } else if (current instanceof PasswordCallback) {
                        ((PasswordCallback) current).setPassword(password.toCharArray());
                    } else if (current instanceof VerifyPasswordCallback) {
                        VerifyPasswordCallback vpc = (VerifyPasswordCallback) current;
                        vpc.setVerified(password.equals(vpc.getPassword()));
                    } else if (current instanceof DigestHashCallback) {
                        DigestHashCallback dhc = (DigestHashCallback) current;
                        try {
                            UsernamePasswordHashUtil uph = new UsernamePasswordHashUtil();
                            if (userName == null || realm == null) {
                                throw MESSAGES.insufficientInformationToGenerateHash();
                            }
                            dhc.setHash(uph.generateHashedURP(userName, realm, password.toCharArray()));
                        } catch (NoSuchAlgorithmException e) {
                            throw MESSAGES.unableToGenerateHash(e);
                        }
                    }
                }

            }
        };
    }

}
