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

package org.jboss.as.server.manager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.ServerFactory;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.process.RespawnPolicy;
import org.jboss.as.server.ServerManagerCommunicationsActivator;
import org.jboss.as.server.ServerManagerProtocolUtils;
import org.jboss.as.server.ServerStartTask;
import org.jboss.as.server.ServerState;
import org.jboss.as.server.ServerManagerProtocol.ServerManagerToServerProtocolCommand;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceActivator;

/**
 * Represents a managed server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry
 */
public final class Server {
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;
    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", Module.getModuleFromDefaultLoader(ModuleIdentifier.fromString("org.jboss.marshalling.river")).getClassLoader());
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setVersion(2);
        config.setClassTable(ModularClassTable.getInstance());
        CONFIG = config;
    }

    /**
     * Prefix applied to a server's name to create it's process name.
     */
    static final String SERVER_PROCESS_NAME_PREFIX = "Server:";

    private final String serverName;
    private final String serverProcessName;
    private final Map<String, String> systemProperties;
    private final JvmElement jvmElement;
    private final ServerManagerEnvironment environment;
    private final int portOffset;
    private final ProcessManagerSlave processManagerSlave;
    private final InetSocketAddress managementSocket;
    private volatile DirectServerManagerCommunicationHandler communicationHandler;

    private final RespawnPolicy respawnPolicy = RespawnPolicy.DefaultRespawnPolicy.INSTANCE;
    private final AtomicInteger respawnCount = new AtomicInteger();
    private final List<AbstractServerModelUpdate<?>> updateList = new ArrayList<AbstractServerModelUpdate<?>>();
    private volatile ServerState state;

    public Server(final String serverName, final DomainModel domainModel, final HostModel hostModel,
            final ServerManagerEnvironment environment, final ProcessManagerSlave processManagerSlave,
            final InetSocketAddress managementSocket) {
        assert domainModel != null : "domainModel is null";
        assert hostModel   != null : "hostModel is null";
        assert serverName  != null : "serverName is null";
        assert environment != null : "environment is null";
        assert processManagerSlave != null : "processManagerSlave is null";
        assert managementSocket != null : "managementSocket is null";

        this.serverName = serverName;
        this.serverProcessName = SERVER_PROCESS_NAME_PREFIX + serverName;
        this.environment = environment;
        this.processManagerSlave = processManagerSlave;
        this.managementSocket = managementSocket;

        ServerFactory.combine(domainModel, hostModel, serverName, updateList);

        ServerElement server = hostModel.getServer(serverName);
        if (server == null)
            throw new IllegalStateException("Server " + serverName + " is not listed in Host");

        String serverGroupName = server.getServerGroup();
        ServerGroupElement serverGroup = domainModel.getServerGroup(serverGroupName);
        if (serverGroup == null)
            throw new IllegalStateException("Server group" + serverGroupName + " is not listed in Domain");

        this.portOffset = server.getSocketBindingGroupName() == null ? serverGroup.getSocketBindingPortOffset() : server.getSocketBindingPortOffset();

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
        JvmElement hostVM = hostModel.getJvm(ourVMName);

        jvmElement = new JvmElement(groupVM, hostVM, serverVM);

        Map<String, String> properties = new HashMap<String, String>();
        properties.putAll(domainModel.getSystemProperties().getProperties());
        properties.putAll(serverGroup.getSystemProperties().getProperties());
        properties.putAll(hostModel.getSystemProperties().getProperties());
        properties.putAll(server.getSystemProperties().getProperties());
        // TODO remove system properties from JvmElement
        properties.putAll(jvmElement.getSystemProperties().getProperties());
        addStandardProperties(properties);

        this.systemProperties = properties;

        this.state = ServerState.BOOTING;
    }

    public ServerState getState() {
        return state;
    }

    void setState(ServerState state) {
        this.state = state;
    }

    int incrementAndGetRespawnCount() {
        return respawnCount.incrementAndGet();
    }

    void resetRespawnCount() {
        respawnCount.set(0);
    }

    String getServerProcessName() {
        return serverProcessName;
    }

    String getServerName() {
        return serverName;
    }

    RespawnPolicy getRespawnPolicy() {
        return respawnPolicy;
    }

    List<AbstractServerModelUpdate<?>> getUpdates() {
        return new ArrayList<AbstractServerModelUpdate<?>>(updateList);
    }

    List<String> getServerLaunchCommand() {
        List<String> command = new ArrayList<String>();

//      if (false) {
//          // Example: run at high priority on *NIX
//          args.add("/usr/bin/nice");
//          args.add("-n");
//          args.add("-10");
//      }
//      if (false) {
//          // Example: run only on processors 1-4 on Linux
//          args.add("/usr/bin/taskset");
//          args.add("0x0000000F");
//      }

        command.add(getJavaCommand());

        JvmOptionsBuilderFactory.getInstance().addOptions(jvmElement, command);

        for (Map.Entry<String, String> prop : systemProperties.entrySet()) {
            StringBuilder sb = new StringBuilder("-D");
            sb.append(prop.getKey());
            sb.append('=');
            sb.append(prop.getValue() == null ? "true" : prop.getValue());
            command.add(sb.toString());
        }

        command.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        command.add("-Dorg.jboss.boot.log.file=domain/servers/" + serverName + "/logs/boot.log");
        command.add("-jar");
        command.add("jboss-modules.jar");
        command.add("-mp");
        command.add("modules");
        command.add("-logmodule");
        command.add("org.jboss.logmanager");
        command.add("org.jboss.as.server");

        return command;
    }

    Map<String, String> getServerLaunchEnvironment() {
        Map<String, String> env = null;
        PropertiesElement pe = jvmElement.getEnvironmentVariables();
        if (pe != null) {
            env = pe.getProperties();
        }
        else {
            env = Collections.emptyMap();
        }
        return env;
    }

    void setCommunicationHandler(DirectServerManagerCommunicationHandler communicationHandler) {
        this.communicationHandler = communicationHandler;
    }

    public void addServerProcess() throws IOException {
        List<String> command = getServerLaunchCommand();

        Map<String, String> env = getServerLaunchEnvironment();

        //Add to process manager
        processManagerSlave.addProcess(serverProcessName, command, env, environment.getHomeDir().getAbsolutePath());
    }

    public void startServerProcess() throws IOException {

        setState(ServerState.BOOTING);

        processManagerSlave.startProcess(serverProcessName);
        ServerManagerCommunicationsActivator commActivator = new ServerManagerCommunicationsActivator(serverProcessName, null, managementSocket);

        Runnable fakeLogConfigurator = new FakeLogConfigurator(); // FIXME use a real one
        ServerStartTask startTask = new ServerStartTask(serverName, portOffset, fakeLogConfigurator, Collections.<ServiceActivator>singletonList(commActivator), updateList);
        // TODO - not efficient but it will work for now
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
        marshaller.start(Marshalling.createByteOutput(baos));
        marshaller.writeObject(startTask);
        marshaller.finish();
        marshaller.close();
        processManagerSlave.sendStdin(serverProcessName, baos.toByteArray());

        setState(ServerState.STARTING);
    }

    public void stopServerProcess() throws IOException {
        processManagerSlave.stopProcess(serverProcessName);
    }

    public void removeServerProcess() throws IOException {
        processManagerSlave.removeProcess(serverProcessName);
    }

    private void sendCommand(ServerManagerToServerProtocolCommand command) throws IOException {
        sendCommand(command, null);
    }

    private void sendCommand(ServerManagerToServerProtocolCommand command, Object o) throws IOException {
        byte[] cmd = ServerManagerProtocolUtils.createCommandBytes(command, o);
        communicationHandler.sendMessage(cmd);
    }

    private String getJavaCommand() {
        String javaHome = jvmElement.getJavaHome();
        if (javaHome == null) { // TODO should this be possible?
            if(environment.getDefaultJVM() != null) {
                return environment.getDefaultJVM().getAbsolutePath();
            }
            return "java"; // hope for the best
        }

        File f = new File(javaHome);
        f = new File(f, "bin");
        f = new File (f, "java");
        return f.getAbsolutePath();
    }


    /**
     * Equivalent to default JAVA_OPTS in < AS 7 run.conf file
     *
     * TODO externalize this somewhere if doing this at all is the right thing
     *
     * @param sysProps
     */
    private void addStandardProperties(Map<String, String> sysProps) {
        //
        if (!sysProps.containsKey("sun.rmi.dgc.client.gcInterval")) {
            sysProps.put("sun.rmi.dgc.client.gcInterval","3600000");
        }
        if (!sysProps.containsKey("sun.rmi.dgc.server.gcInterval")) {
            sysProps.put("sun.rmi.dgc.server.gcInterval","3600000");
        }

        sysProps.put(ServerManagerEnvironment.HOME_DIR, environment.getHomeDir().getAbsolutePath());
        String key = "jboss.server.base.dir";
        if (sysProps.get(key) == null) {
            File serverBaseDir = new File(environment.getDomainServersDir(), serverName);
            sysProps.put(key, serverBaseDir.getAbsolutePath());
        }
    }

    private static class FakeLogConfigurator implements Runnable, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public void run() {
            Logger.getLogger("FakeLogConfigurator").info("Sure would be cool to be a real log configurator");

        }

    }
}
