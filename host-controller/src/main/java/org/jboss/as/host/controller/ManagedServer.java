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

package org.jboss.as.host.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.Element;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.JvmElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.ServerFactory;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import static org.jboss.as.protocol.StreamUtils.safeFinish;

import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.domain.HostControllerClient;
import org.jboss.as.server.mgmt.domain.HostControllerConnectionService;
import org.jboss.as.server.ServerStartTask;
import org.jboss.as.server.ServerState;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Represents a managed server.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry
 */
public final class ManagedServer {
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

    public static String getServerProcessName(String serverName) {
        return SERVER_PROCESS_NAME_PREFIX + serverName;
    }

    private final String serverName;
    private final String serverProcessName;
    private final Map<String, String> systemProperties;
    private final JvmElement jvmElement;
    private final HostControllerEnvironment environment;
    private final int portOffset;
    private final ProcessControllerClient processControllerClient;
    private final AtomicInteger respawnCount = new AtomicInteger();
    private final List<AbstractServerModelUpdate<?>> updateList = new ArrayList<AbstractServerModelUpdate<?>>();
    private final InetSocketAddress managementSocket;
    private volatile ServerState state;
    private Connection serverManagementConnection;

    private final byte[] authKey;

    public ManagedServer(final String serverName, final DomainModel domainModel, final HostModel hostModel,
            final HostControllerEnvironment environment, final ProcessControllerClient processControllerClient,
            final InetSocketAddress managementSocket) {
        assert domainModel != null : "domainModel is null";
        assert hostModel   != null : "hostModel is null";
        assert serverName  != null : "serverName is null";
        assert environment != null : "environment is null";
        assert processControllerClient != null : "processControllerSlave is null";
        assert managementSocket != null : "managementSocket is null";

        final byte[] authKey = new byte[16];
        // TODO: use a RNG with a secure seed
        new Random().nextBytes(authKey);
        this.authKey = authKey;

        this.serverName = serverName;
        this.serverProcessName = getServerProcessName(serverName);
        this.environment = environment;
        this.processControllerClient = processControllerClient;
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

        command.add("-Dorg.jboss.boot.log.file=domain/servers/" + serverName + "/log/boot.log");
        // TODO: make this better
        command.add("-Dlogging.configuration=file:" + new File("").getAbsolutePath() + "/domain/configuration/logging.properties");
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

    public void addServerProcess() throws IOException {
        List<String> command = getServerLaunchCommand();

        Map<String, String> env = getServerLaunchEnvironment();

        //Add to process controller
        processControllerClient.addProcess(serverProcessName, authKey, command.toArray(new String[command.size()]), environment.getHomeDir().getAbsolutePath(), env);
    }

    public void startServerProcess() throws IOException {

        setState(ServerState.BOOTING);


        processControllerClient.startProcess(serverProcessName);
        ServiceActivator hostControllerCommActivator = new HostControllerCommServiceActivator(managementSocket);
        ServerStartTask startTask = new ServerStartTask(serverName, portOffset, Collections.<ServiceActivator>singletonList(hostControllerCommActivator), updateList);
        final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
        final OutputStream os = processControllerClient.sendStdin(serverProcessName);
        marshaller.start(Marshalling.createByteOutput(os));
        marshaller.writeObject(startTask);
        marshaller.finish();
        marshaller.close();
        os.close();

        setState(ServerState.STARTING);
    }

    public void stopServerProcess() throws IOException {
        processControllerClient.stopProcess(serverProcessName);
    }

    public void removeServerProcess() throws IOException {
        processControllerClient.removeProcess(serverProcessName);
    }

    public List<UpdateResultHandlerResponse<?>> applyUpdates(final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {

        if(serverManagementConnection == null) {
            Exception e = new IllegalStateException("Updates can not be applied to a managed server without a management connection");
            UpdateResultHandlerResponse<?> urhr = UpdateResultHandlerResponse.createFailureResponse(e);
            List<UpdateResultHandlerResponse<?>> result = new ArrayList<UpdateResultHandlerResponse<?>>(updates.size());
            for (int i = 0; i < updates.size(); i++) {
                result.add(urhr);
            }
            return result;
        }

        try {
            return new ApplyUpdatesRequest(updates, allowOverallRollback).executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(serverManagementConnection));
        } catch (Exception e) {
            UpdateResultHandlerResponse<?> urhr = UpdateResultHandlerResponse.createFailureResponse(e);
            List<UpdateResultHandlerResponse<?>> result = new ArrayList<UpdateResultHandlerResponse<?>>(updates.size());
            for (int i = 0; i < updates.size(); i++) {
                result.add(urhr);
            }
            return result;
        }
    }

    public void gracefulShutdown(long timeout) throws IOException {
        // FIXME implement gracefulShutdown RPC
        throw new UnsupportedOperationException("HostController to Server RPC is not implemented");
    }

    public ServerModel getServerModel() {
        try {
            return new GetServerModelRequest().executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(serverManagementConnection));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ServerModel from server [" + serverName + "]", e);
        }
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

    public void setServerManagementConnection(Connection serverManagementConnection) {
        this.serverManagementConnection = serverManagementConnection;
        // FIXME this isn't really correct; just means the server started
        // enough to connect
        setState(ServerState.STARTED);
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

        sysProps.put(HostControllerEnvironment.HOME_DIR, environment.getHomeDir().getAbsolutePath());
        String key = ServerEnvironment.SERVER_BASE_DIR;
        if (sysProps.get(key) == null) {
            File serverBaseDir = new File(environment.getDomainServersDir(), serverName);
            sysProps.put(key, serverBaseDir.getAbsolutePath());
        }
        // Servers should use the host controller's deployment content repo
        key = ServerEnvironment.SERVER_DEPLOY_DIR;
        if (sysProps.get(key) == null) {
            File serverDeploymentDir = environment.getDomainDeploymentDir();
            sysProps.put(key, serverDeploymentDir.getAbsolutePath());
        }

        key = ServerEnvironment.SERVER_SYSTEM_DEPLOY_DIR;
        if (sysProps.get(key) == null) {
            File serverDeploymentDir = environment.getDomainSystemDeploymentDir();
            sysProps.put(key, serverDeploymentDir.getAbsolutePath());
        }
    }

    private static class HostControllerCommServiceActivator implements ServiceActivator, Serializable {
        private final InetSocketAddress managementSocket;

        private HostControllerCommServiceActivator(InetSocketAddress managementSocket) {
            this.managementSocket = managementSocket;
        }

        private static final long serialVersionUID = 2522252040771977214L;

        public void activate(final ServiceActivatorContext serviceActivatorContext) {
            final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();

            final HostControllerConnectionService smConnection = new HostControllerConnectionService();
            serviceTarget.addService(HostControllerConnectionService.SERVICE_NAME, smConnection)
                .addInjection(smConnection.getSmAddressInjector(), managementSocket)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

            final HostControllerClient client = new HostControllerClient();
            serviceTarget.addService(HostControllerClient.SERVICE_NAME, client)
                .addDependency(HostControllerConnectionService.SERVICE_NAME, Connection.class, client.getSmConnectionInjector())
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class, client.getServerControllerInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        }
    }

    private static class ApplyUpdatesRequest extends ManagementRequest<List<UpdateResultHandlerResponse<?>>> {
        private final List<AbstractServerModelUpdate<?>> updates;
        private final boolean allowOverallRollback;

        private ApplyUpdatesRequest(final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {
            this.updates = updates;
            this.allowOverallRollback = allowOverallRollback;
        }

        @Override
        protected byte getHandlerId() {
            return DomainServerProtocol.SERVER_TO_HOST_CONTROLLER_OPERATION;
        }

        @Override
        protected byte getRequestCode() {
            return DomainServerProtocol.SERVER_MODEL_UPDATES_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.SERVER_MODEL_UPDATES_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(DomainServerProtocol.PARAM_ALLOW_ROLLBACK);
                marshaller.writeBoolean(allowOverallRollback);
                marshaller.writeByte(DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
                marshaller.writeInt(updates.size());
                for (AbstractServerModelUpdate<?> update : updates) {
                    marshaller.writeByte(DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE);
                    marshaller.writeObject(update);
                }
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }

        @Override
        protected final List<UpdateResultHandlerResponse<?>> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_RESPONSE_COUNT);
                final int updateCount = unmarshaller.readInt();
                final List<UpdateResultHandlerResponse<?>> results = new ArrayList<UpdateResultHandlerResponse<?>>(updateCount);
                for (int i = 0; i < updateCount; i++) {
                    expectHeader(unmarshaller, DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_RESPONSE);
                    UpdateResultHandlerResponse<?> updateResult = unmarshal(unmarshaller, UpdateResultHandlerResponse.class);
                    results.add(updateResult);
                }
                unmarshaller.finish();
                return results;
            } finally {
                safeFinish(unmarshaller);
            }
        }
    }

    private class GetServerModelRequest extends ManagementRequest<ServerModel> {

        @Override
        protected byte getHandlerId() {
            return DomainServerProtocol.SERVER_TO_HOST_CONTROLLER_OPERATION;
        }

        @Override
        protected byte getRequestCode() {
            return DomainServerProtocol.GET_SERVER_MODEL_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected final ServerModel receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, DomainServerProtocol.RETURN_SERVER_MODEL);
                ServerModel serverModel = unmarshal(unmarshaller, ServerModel.class);
                unmarshaller.finish();
                return serverModel;
            } finally {
                safeFinish(unmarshaller);
            }
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }
}
