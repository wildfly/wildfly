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
package org.jboss.test.as.protocol.support.server;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.model.ServerModel;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.SystemExiter;
import org.jboss.as.server.Main;
import org.jboss.as.server.Server;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerStartException;
import org.jboss.as.server.manager.ServerState;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerToServerManagerProtocolCommand;
import org.jboss.test.as.protocol.support.process.NoopExiter;

/**
 * Starts a real server instance in-process. It differs from the proper server
 * in that it does not start the underlying MSC
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestServerProcess extends Server {
    public TestServerProcess(ServerEnvironment environment) {
        super(environment);
    }

    public static List<String> createServer(String serverName, int pmPort, int smPort) throws Exception{
        String[] args = new String[] {
                CommandLineConstants.INTERPROCESS_NAME,
                serverName,
                CommandLineConstants.INTERPROCESS_PM_ADDRESS,
                InetAddress.getLocalHost().getHostAddress(),
                CommandLineConstants.INTERPROCESS_PM_PORT,
                String.valueOf(pmPort),
                CommandLineConstants.INTERPROCESS_SM_ADDRESS,
                InetAddress.getLocalHost().getHostAddress(),
                CommandLineConstants.INTERPROCESS_SM_PORT,
                String.valueOf(smPort)
        };

        return Arrays.asList(args);
    }

    public static TestServerProcess createServer(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        SystemExiter.initialize(new NoopExiter());

        ServerEnvironment config = Main.determineEnvironment(args, System.getProperties(), stdin, stdout, stderr);
        if (config == null) {
            throw new RuntimeException("Could not determine server environment");
        } else {
            TestServerProcess server = new TestServerProcess(config);
            server.start();
            return server;
        }
    }

    /**
     * Override the start method so that we don't actually start the MSC server.
     * This is to get around the fact that the servers shutdown their services
     * asynchronously so stuff might be hanging around breaking the next server
     * boot in the next test. This also allows us to start more than one server
     * in-process.
     *
     * @param config the server config
     */
    @Override
    public void start(ServerModel config) throws ServerStartException {
            setState(ServerState.STARTED);
            sendMessage(ServerToServerManagerProtocolCommand.SERVER_STARTED);
    }

    /**
     * Override the start method so that we don't atually stop the not-started MSC server
     * @see #start(ServerModel)
     */
    @Override
    public void stop() {
        sendMessage(ServerToServerManagerProtocolCommand.SERVER_STOPPED);
        setState(ServerState.STOPPED);
        shutdownCommunicationHandlers();
    }


}
