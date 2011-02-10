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
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.mgmt.domain.HostControllerConnectionService;
import org.jboss.as.server.mgmt.domain.HostControllerServerClient;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.log4j.BridgeRepositorySelector;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.SimpleStdioContextSelector;
import org.jboss.stdio.StdioContext;

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
        // TODO: privileged block
        System.setProperty("log4j.defaultInitOverride", "true");
        new BridgeRepositorySelector().start();

        final InputStream initialInput = System.in;
        final PrintStream initialError = System.err;

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
            StreamUtils.readFully(initialInput, authKey);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            throw new IllegalStateException(); // not reached
        }

        final MarshallerFactory factory = Marshalling.getMarshallerFactory("river", DomainServerMain.class.getClassLoader());
        final Unmarshaller unmarshaller;
        final ByteInput byteInput;
        try {
            final MarshallingConfiguration configuration = new MarshallingConfiguration();
            configuration.setVersion(2);
            configuration.setClassResolver(new SimpleClassResolver(DomainServerMain.class.getClassLoader()));
            unmarshaller = factory.createUnmarshaller(configuration);
            byteInput = Marshalling.createByteInput(initialInput);
            unmarshaller.start(byteInput);
            final ServerTask task = unmarshaller.readObject(ServerTask.class);
            unmarshaller.finish();
            task.run(Arrays.<ServiceActivator>asList(new ServiceActivator() {
                @Override
                public void activate(final ServiceActivatorContext serviceActivatorContext) {
                    // TODO activate host controller client service
                }
            }));
        } catch (Exception e) {
            e.printStackTrace(initialError);
            System.exit(1);
            throw new IllegalStateException(); // not reached
        }
        for (;;) try {
            unmarshaller.start(byteInput);
            final InetSocketAddress socketAddress = unmarshaller.readObject(InetSocketAddress.class);
            unmarshaller.finish();
            // todo connect to the HC at socketAddress, disconnect from old
            break;
        } catch (InterruptedIOException e) {
            Thread.interrupted();
            // ignore
        } catch (EOFException e) {
            // this means it's time to exit
            break;
        } catch (Exception e) {
            break;
        }
        // Once the input stream is cut off, shut down
        System.exit(0);
        throw new IllegalStateException(); // not reached
    }

    public static final class HostControllerCommunicationActivator implements ServiceActivator, Serializable {
        private static final long serialVersionUID = 6671220116719309952L;
        private final String serverName;
        private final InetSocketAddress managementSocket;

        public HostControllerCommunicationActivator(final String serverName, final InetSocketAddress managementSocket) {
            this.serverName = serverName;
            this.managementSocket = managementSocket;
        }

        @Override
        public void activate(final ServiceActivatorContext serviceActivatorContext) {
            final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();

            final HostControllerConnectionService smConnection = new HostControllerConnectionService();
            serviceTarget.addService(HostControllerConnectionService.SERVICE_NAME, smConnection)
                .addInjection(smConnection.getSmAddressInjector(), managementSocket)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

            final HostControllerServerClient client = new HostControllerServerClient(serverName);
            serviceTarget.addService(HostControllerServerClient.SERVICE_NAME, client)
                .addDependency(HostControllerConnectionService.SERVICE_NAME, Connection.class, client.getSmConnectionInjector())
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class, client.getServerControllerInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        }
    }
}
