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

package org.jboss.as.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.jboss.as.controller.ModelController;
import org.jboss.as.process.protocol.StreamUtils;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.mgmt.domain.HostControllerServerClient;
import org.jboss.as.server.mgmt.domain.RemoteFileRepository;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.log4j.BridgeRepositorySelector;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;
import org.jboss.threads.AsyncFuture;

/**
 * The main entry point for domain-managed server instances.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainServerMain {

    private DomainServerMain() {
    }

    /**
     * Main entry point.  Reads and executes the command object from standard input.
     *
     * @param args ignored
     */
    public static void main(String[] args) {

        SecurityActions.setSystemProperty("log4j.defaultInitOverride", "true");
        new BridgeRepositorySelector().start();

        final InputStream initialInput = System.in;
        final PrintStream initialError = System.err;

        // Make sure our original stdio is properly captured.
        try {
            Class.forName(ConsoleHandler.class.getName(), true, ConsoleHandler.class.getClassLoader());
        } catch (Throwable ignored) {
        }

        // Install JBoss Stdio to avoid any nasty crosstalk.
        StdioContext.install();
        final StdioContext context = StdioContext.create(
            new NullInputStream(),
            new LoggingOutputStream(org.jboss.logmanager.Logger.getLogger("stdout"), Level.INFO),
            new LoggingOutputStream(org.jboss.logmanager.Logger.getLogger("stderr"), Level.ERROR)
        );
        StdioContext.setStdioContextSelector(new SimpleStdioContextSelector(context));

        final byte[] authKey = new byte[16];
        try {
            org.jboss.as.process.protocol.StreamUtils.readFully(initialInput, authKey);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            throw new IllegalStateException(); // not reached
        }

        final MarshallerFactory factory = Marshalling.getMarshallerFactory("river", DomainServerMain.class.getClassLoader());
        final Unmarshaller unmarshaller;
        final ByteInput byteInput;
        final AsyncFuture<ServiceContainer> containerFuture;
        try {
            Module.registerURLStreamHandlerFactoryModule(Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.vfs")));
            final MarshallingConfiguration configuration = new MarshallingConfiguration();
            configuration.setVersion(2);
            configuration.setClassResolver(new SimpleClassResolver(DomainServerMain.class.getClassLoader()));
            unmarshaller = factory.createUnmarshaller(configuration);
            byteInput = Marshalling.createByteInput(initialInput);
            unmarshaller.start(byteInput);
            final ServerTask task = unmarshaller.readObject(ServerTask.class);
            unmarshaller.finish();
            containerFuture = task.run(Arrays.<ServiceActivator>asList(new ServiceActivator() {
                @Override
                public void activate(final ServiceActivatorContext serviceActivatorContext) {
                    // TODO activate host controller client service
                }
            }));
        } catch (Exception e) {
            e.printStackTrace(initialError);
            System.exit(1);
            throw new IllegalStateException(); // not reached
        } finally {
        }
        for (;;) try {
            String hostName = StreamUtils.readUTFZBytes(initialInput);
            int port = StreamUtils.readInt(initialInput);
            // TODO remove managementSubsystemEndpoint !?
            // This property does not make sense on reconnect, since there can't be any configuration changes
            // while the channel is down. Other changes are either applied to the runtime directly or require a restart.
            boolean managementSubsystemEndpoint = StreamUtils.readBoolean(initialInput);
            byte[] asAuthKey = new byte[16];
            StreamUtils.readFully(initialInput, asAuthKey);

            // Get the host-controller server client
            final ServiceContainer container = containerFuture.get();
            final ServiceController<?> controller = container.getRequiredService(HostControllerServerClient.SERVICE_NAME);
            final HostControllerServerClient client = (HostControllerServerClient) controller.getValue();
            // Reconnect to the host-controller
            client.reconnect(hostName, port, asAuthKey);

        } catch (InterruptedIOException e) {
            Thread.interrupted();
            // ignore
        } catch (EOFException e) {
            // this means it's time to exit
            break;
        } catch (Exception e) {
            e.printStackTrace();
            break;
        }

        // Once the input stream is cut off, shut down
        System.exit(0);
        throw new IllegalStateException(); // not reached
    }

    private static void addCommunicationServices(final ServiceTarget serviceTarget, final String serverName, final String serverProcessName, final byte[] authKey,
            final InetSocketAddress managementSocket, final boolean managementSubsystemEndpoint, final boolean isReconnect) {


        final ServiceName endpointName;
        if (!managementSubsystemEndpoint) {
            endpointName = ManagementRemotingServices.MANAGEMENT_ENDPOINT;
            if (!isReconnect) {
                ManagementRemotingServices.installRemotingEndpoint(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                        SecurityActions.getSystemProperty(ServerEnvironment.NODE_NAME), EndpointService.EndpointType.MANAGEMENT, null, null);
            }
        } else {
            endpointName = RemotingServices.SUBSYSTEM_ENDPOINT;
        }

        try {
            final int port = managementSocket.getPort();
            final String host = InetAddress.getByName(managementSocket.getHostName()).getHostName();
            final HostControllerServerClient client = new HostControllerServerClient(serverName, serverProcessName, host, port, authKey);
                    serviceTarget.addService(HostControllerServerClient.SERVICE_NAME, client)
                        .addDependency(endpointName, Endpoint.class, client.getEndpointInjector())
                        .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, client.getServerControllerInjector())
                        .addDependency(RemoteFileRepository.SERVICE_NAME, RemoteFileRepository.class, client.getRemoteFileRepositoryInjector())
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }



    }

    public static final class HostControllerCommunicationActivator implements ServiceActivator, Serializable {
        private static final long serialVersionUID = -633960958861565102L;
        private final InetSocketAddress managementSocket;
        private final String serverName;
        private final String serverProcessName;
        private final byte[] authKey;
        private final boolean managementSubsystemEndpoint;

        public HostControllerCommunicationActivator(final InetSocketAddress managementSocket, final String serverName, final String serverProcessName, final byte[] authKey, final boolean managementSubsystemEndpoint) {
            this.managementSocket = managementSocket;
            this.serverName = serverName;
            this.serverProcessName = serverProcessName;
            this.authKey = authKey;
            this.managementSubsystemEndpoint = managementSubsystemEndpoint;
        }

        @Override
        public void activate(final ServiceActivatorContext serviceActivatorContext) {
            final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();
            // TODO - Correct the authKey propagation.
            addCommunicationServices(serviceTarget, serverName, serverProcessName, authKey, managementSocket, managementSubsystemEndpoint, false);
        }
    }

}
