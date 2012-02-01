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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementMessageHandler;
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

    /** The managed servers. */
    private final ConcurrentMap<String, ManagedServer> servers = new ConcurrentHashMap<String, ManagedServer>();

    private final HostControllerEnvironment environment;
    private final ProcessControllerClient processControllerClient;
    private final InetSocketAddress managementAddress;
    private final DomainController domainController;

    private volatile boolean stopped;
    private volatile boolean connectionFinished;

    //
    private volatile CountDownLatch processInventoryLatch;
    private volatile Map<String, ProcessInfo> processInfos;

    private final Object shutdownCondition = new Object();

    ServerInventoryImpl(final DomainController domainController, final HostControllerEnvironment environment, final InetSocketAddress managementAddress, final ProcessControllerClient processControllerClient) {
        this.domainController = domainController;
        this.environment = environment;
        this.managementAddress = managementAddress;
        this.processControllerClient = processControllerClient;
    }

    @Override
    public String getServerProcessName(String serverName) {
        return ManagedServer.getServerProcessName(serverName);
    }

    @Override
    public String getProcessServerName(String processName) {
        return ManagedServer.getServerName(processName);
    }

    @Override
    public synchronized Map<String, ProcessInfo> determineRunningProcesses() {
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
            throw MESSAGES.couldNotGetServerInventory(30L, TimeUnit.SECONDS.toString().toLowerCase(Locale.US));
        }
        return processInfos;
    }

    @Override
    public Map<String, ProcessInfo> determineRunningProcesses(final boolean serversOnly) {
        final Map<String, ProcessInfo> processInfos = determineRunningProcesses();
        if (!serversOnly) {
            return processInfos;
        }
        final Map<String, ProcessInfo> processes = new HashMap<String, ProcessInfo>();
        for (Map.Entry<String, ProcessInfo> procEntry : processInfos.entrySet()) {
            if (ManagedServer.isServerProcess(procEntry.getKey())) {
                processes.put(procEntry.getKey(), procEntry.getValue());
            }
        }
        return processes;
    }

    @Override
    public ServerStatus determineServerStatus(final String serverName) {
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            return ServerStatus.STOPPED;
        }
        return server.getState();
    }

    @Override
    public ServerStatus startServer(final String serverName, final ModelNode domainModel) {
        if(stopped || connectionFinished) {
            throw HostControllerMessages.MESSAGES.hostAlreadyShutdown();
        }
        final ManagedServer existing = servers.get(serverName);
        if(existing != null) {
            ROOT_LOGGER.existingServerWithState(serverName, existing.getState());
            return determineServerStatus(serverName);
        }
        final ManagedServer server = createManagedServer(serverName, domainModel);
        if(servers.putIfAbsent(serverName, server) != null) {
            return determineServerStatus(serverName);
        }
        server.start();
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
        return server.getState();
    }

    @Override
    public ServerStatus restartServer(final String serverName, final int gracefulTimeout, final ModelNode domainModel) {
        stopServer(serverName, gracefulTimeout);
        synchronized (shutdownCondition) {
            for(;;) {
                if(stopped || connectionFinished) {
                    throw HostControllerMessages.MESSAGES.hostAlreadyShutdown();
                }
                if(! servers.containsKey(serverName)) {
                    break;
                }
                try {
                    shutdownCondition.wait();
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        startServer(serverName, domainModel);
        return determineServerStatus(serverName);
    }

    @Override
    public ServerStatus stopServer(final String serverName, final int gracefulTimeout) {
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            return ServerStatus.STOPPED;
        }
        server.stop();
        return server.getState();
    }

    @Override
    public void reconnectServer(final String serverName, final ModelNode domainModel, final boolean running) {
        if(stopped || connectionFinished) {
            throw HostControllerMessages.MESSAGES.hostAlreadyShutdown();
        }
        final ManagedServer existing = servers.get(serverName);
        if(existing != null) {
            ROOT_LOGGER.existingServerWithState(serverName, existing.getState());
            return;
        }
        final ManagedServer server = createManagedServer(serverName, domainModel);
        if(servers.putIfAbsent(serverName, server) != null) {
            ROOT_LOGGER.existingServerWithState(serverName, existing.getState());
            return;
        }
        if(running) {
            server.reconnectServerProcess();
        }
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void stopServers(final int gracefulTimeout) {
        stopServers(gracefulTimeout, false);
    }

    public void stopServers(final int gracefulTimeout, final boolean blockUntilStopped) {
        final boolean stopped = this.stopped;
        this.stopped = true;
        if(! stopped) {
            if(connectionFinished) {
                // In case the connection to the ProcessController is closed we won't be able to shutdown the servers from here
                // nor can expect to receive any further notifications notifications.
                return;
            }
            for(final ManagedServer server : servers.values()) {
                server.stop();
            }
            if(blockUntilStopped) {
                synchronized (shutdownCondition) {
                    for(;;) {
                        if(connectionFinished) {
                            break;
                        }
                        int count = 0;
                        for(final ManagedServer server : servers.values()) {
                            final ServerStatus state = server.getState();
                            switch (state) {
                                case DISABLED:
                                case FAILED:
                                case STOPPED:
                                    break;
                                default:
                                    count++;
                            }
                        }
                        if(count == 0) {
                            break;
                        }
                        try {
                            shutdownCondition.wait();
                        } catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void serverCommunicationRegistered(final String serverProcessName, final Channel channel, final ProxyCreatedCallback callback) {
        if(stopped || connectionFinished) {
            throw HostControllerMessages.MESSAGES.hostAlreadyShutdown();
        }
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }

        final Channel.Key key = channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                server.callbackUnregistered();
                domainController.unregisterRunningServer(server.getServerName());
            }
        });

        server.callbackRegistered(new ManagedServer.TransitionTask() {
            @Override
            public void execute(ManagedServer server) throws Exception {
                final PathElement element = PathElement.pathElement(RUNNING_SERVER, server.getServerName());
                final ProxyController serverController = RemoteProxyController.create(Executors.newCachedThreadPool(),
                        PathAddress.pathAddress(PathElement.pathElement(HOST, domainController.getLocalHostInfo().getLocalHostName()), element),
                        ProxyOperationAddressTranslator.SERVER,
                        channel);
                if (callback != null && serverController instanceof ManagementMessageHandler) {
                    callback.proxyOperationHandlerCreated((ManagementMessageHandler)serverController);
                }
                domainController.registerRunningServer(serverController);
            }
        }, channel, key);
    }

    @Override
    public void serverProcessStopped(final String serverProcessName) {
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }
        server.processFinished();
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void connectionFinished() {
        this.connectionFinished = true;
        ROOT_LOGGER.debug("process controller connection closed.");
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void serverStartFailed(final String serverProcessName) {
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }
        // server.serverStartFailed();
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void serverProcessAdded(final String serverProcessName) {
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }
        server.processAdded();
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void serverProcessStarted(final String serverProcessName) {
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }
        server.processStarted();
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void serverProcessRemoved(final String serverProcessName) {
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.remove(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }
        server.processRemoved();
        synchronized (shutdownCondition) {
            shutdownCondition.notifyAll();
        }
    }

    @Override
    public void operationFailed(final String serverProcessName, final ProcessMessageHandler.OperationType type) {
        final String serverName = ManagedServer.getServerName(serverProcessName);
        final ManagedServer server = servers.get(serverName);
        if(server == null) {
            ROOT_LOGGER.noServerAvailable(serverName);
            return;
        }
        switch (type) {
            case ADD:
                server.transitionFailed(ManagedServer.InternalState.PROCESS_ADDING);
                break;
            case START:
                server.transitionFailed(ManagedServer.InternalState.PROCESS_STARTING);
                break;
            case STOP:
                server.transitionFailed(ManagedServer.InternalState.PROCESS_STOPPING);
                break;
            case SEND_STDIN:
            case RECONNECT:
                server.transitionFailed(ManagedServer.InternalState.SERVER_STARTING);
                break;
            case REMOVE:
                server.transitionFailed(ManagedServer.InternalState.PROCESS_REMOVING);
                break;
        }
    }

    @Override
    public void processInventory(final Map<String, ProcessInfo> processInfos) {
        this.processInfos = processInfos;
        if (processInventoryLatch != null){
            processInventoryLatch.countDown();
        }
    }

    private ManagedServer createManagedServer(final String serverName, final ModelNode domainModel) {
        final String hostControllerName = domainController.getLocalHostInfo().getLocalHostName();
        final ModelNode hostModel = domainModel.require(HOST).require(hostControllerName);
        final ModelCombiner combiner = new ModelCombiner(serverName, domainModel, hostModel, domainController, environment);
        return new ManagedServer(hostControllerName, serverName, processControllerClient, managementAddress, combiner);
    }

    @Override
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

                        server = servers.get(userName);
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
