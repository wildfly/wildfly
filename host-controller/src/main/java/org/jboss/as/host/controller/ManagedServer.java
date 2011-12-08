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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.server.ServerStartTask;
import org.jboss.as.server.ServerState;
import org.jboss.dmr.ModelNode;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.remoting3.Channel;

/**
 * Represents a managed server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry
 * @author Emanuel Muckenhuber
 */
class ManagedServer {

    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;
    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", Module.getModuleFromCallerModuleLoader(ModuleIdentifier.fromString("org.jboss.marshalling.river")).getClassLoader());
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final ClassLoader cl = ManagedServer.class.getClassLoader();
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setVersion(2);
        config.setClassResolver(new SimpleClassResolver(cl));
        CONFIG = config;
    }

    /**
     * Prefix applied to a server's name to create it's process name.
     */
    public static String SERVER_PROCESS_NAME_PREFIX = "Server:";

    public static String getServerProcessName(String serverName) {
        return SERVER_PROCESS_NAME_PREFIX + serverName;
    }

    public static boolean isServerProcess(String serverProcessName) {
        return serverProcessName.startsWith(SERVER_PROCESS_NAME_PREFIX);
    }

    public static String getServerName(String serverProcessName) {
        return serverProcessName.substring(SERVER_PROCESS_NAME_PREFIX.length());
    }

    private final String serverName;
    private final String serverProcessName;
    private final Object lock = new Object();
    private final ProcessControllerClient processControllerClient;
    private final AtomicInteger respawnCount = new AtomicInteger();
    private final InetSocketAddress managementSocket;
    private final ManagedServerBootConfiguration bootConfiguration;
    private final byte[] authKey;
    private volatile ServerState state;
    private volatile Channel serverManagementChannel;

    public ManagedServer(final String serverName, final ProcessControllerClient processControllerClient,
            final InetSocketAddress managementSocket, final ManagedServerBootConfiguration bootConfiguration) {
        assert serverName  != null : "serverName is null";
        assert processControllerClient != null : "processControllerSlave is null";
        assert managementSocket != null : "managementSocket is null";

        this.serverName = serverName;
        this.serverProcessName = getServerProcessName(serverName);
        this.processControllerClient = processControllerClient;
        this.managementSocket = managementSocket;
        this.bootConfiguration = bootConfiguration;

        final byte[] authKey = new byte[16];
        // TODO: use a RNG with a secure seed
        new Random().nextBytes(authKey);
        this.authKey = authKey;

        this.state = ServerState.STOPPED;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerProcessName() {
        return serverProcessName;
    }

    Channel getServerManagementChannel() {
        return serverManagementChannel;
    }

    public ServerState getState() {
        return state;
    }

    public void setState(ServerState state) {
        this.state = state;
    }

    void setServerManagementChannel(Channel serverManagementChannel) {
        this.serverManagementChannel = serverManagementChannel;
    }

    int incrementAndGetRespawnCount() {
        return respawnCount.incrementAndGet();
    }

    void resetRespawnCount() {
        respawnCount.set(0);
    }

    byte[] getAuthKey() {
        return authKey;
    }

    void createServerProcess() throws IOException {
        synchronized(lock) {
            final List<String> command = bootConfiguration.getServerLaunchCommand();
            final Map<String, String> env = bootConfiguration.getServerLaunchEnvironment();
            final HostControllerEnvironment environment = bootConfiguration.getHostControllerEnvironment();
            // Add the process to the process controller
            processControllerClient.addProcess(serverProcessName, authKey, command.toArray(new String[command.size()]), environment.getHomeDir().getAbsolutePath(), env);
            this.state = ServerState.BOOTING;
        }
    }

    void startServerProcess() throws IOException {
        synchronized(lock) {
            setState(ServerState.BOOTING);

            final List<ModelNode> bootUpdates = bootConfiguration.getBootUpdates();

            processControllerClient.startProcess(serverProcessName);
            ServiceActivator hostControllerCommActivator = HostCommunicationServices.createServerCommuncationActivator(managementSocket, serverName, serverProcessName, authKey, bootConfiguration.isManagementSubsystemEndpoint());
            ServerStartTask startTask = new ServerStartTask(serverName, 0, Collections.<ServiceActivator>singletonList(hostControllerCommActivator), bootUpdates);
            final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
            final OutputStream os = processControllerClient.sendStdin(serverProcessName);
            marshaller.start(Marshalling.createByteOutput(os));
            marshaller.writeObject(startTask);
            marshaller.finish();
            marshaller.close();
            os.close();

            setState(ServerState.STARTING);
        }
    }

    void reconnectServerProcess() throws IOException {
        synchronized (lock){
            processControllerClient.reconnectProcess(serverProcessName, managementSocket.getAddress().getHostName(), managementSocket.getPort(), bootConfiguration.isManagementSubsystemEndpoint(), authKey);
        }
    }

    void stopServerProcess() throws IOException {
        synchronized(lock) {
            processControllerClient.stopProcess(serverProcessName);
        }
    }

    void removeServerProcess() throws IOException {
        synchronized(lock) {
            processControllerClient.removeProcess(serverProcessName);
        }
    }

    /**
     * The managed server boot configuration.
     */
    public static interface ManagedServerBootConfiguration {
        /**
         * Get a list of boot updates.
         *
         * @return the boot updates
         */
        List<ModelNode> getBootUpdates();

        /**
         * Get the server launch environment.
         *
         * @return the launch environment
         */
        Map<String, String> getServerLaunchEnvironment();

        /**
         * Get server launch command.
         *
         * @return the launch command
         */
        List<String> getServerLaunchCommand();

        /**
         * Get the host controller environment.
         *
         * @return the host controller environment
         */
        HostControllerEnvironment getHostControllerEnvironment();

        /**
         * Get whether the native management remoting connector should use the endpoint set up by
         */
        boolean isManagementSubsystemEndpoint();
    }

}
