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
package org.jboss.test.as.protocol.support.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.jboss.as.process.ProcessManagerMaster;
import org.jboss.as.process.ManagedProcess.ProcessHandler;
import org.jboss.as.process.ProcessOutputStreamHandler.Managed;
import org.jboss.test.as.protocol.support.server.MockServerProcess;
import org.jboss.test.as.protocol.support.server.TestServerProcess;
import org.jboss.test.as.protocol.support.server.manager.MockServerManagerProcess;
import org.jboss.test.as.protocol.support.server.manager.TestServerManagerProcess;

/**
 * A process handler that differs from the default one by starting
 * server and server manager processes in-process.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestProcessHandler implements ProcessHandler {

    private final TestProcessHandlerFactory factory;
    private final boolean useRealServerManager;
    private final boolean useRealServers;
    private final ProcessInputOutputPair processOutputStream;
    private final ProcessInputOutputPair processInputStream;
    private final ProcessInputOutputPair processErrorStream;

    private volatile TestServerManagerProcess testServerManager;
    private volatile MockServerProcess mockServerProcess;
    private volatile MockServerManagerProcess mockServerManager;
    private volatile TestServerProcess testServerProcess;


    public TestProcessHandler(TestProcessHandlerFactory factory, boolean useRealServerManager, boolean useRealServers) {
        this.factory = factory;
        this.useRealServerManager = useRealServerManager;
        this.useRealServers = useRealServers;
        try {
            this.processOutputStream = new ProcessInputOutputPair();
            this.processInputStream = new ProcessInputOutputPair();
            this.processErrorStream = new ProcessInputOutputPair();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ProcessHandler createProcess(Managed managed, List<String> command, Map<String, String> environment, String workingDirectory) throws IOException {
        String processName = managed.getProcessName();
        if (processName.equals(ProcessManagerMaster.SERVER_MANAGER_PROCESS_NAME)) {
            try {
                if (useRealServerManager) {
                        testServerManager = TestServerManagerProcess.createServerManager(managed, command, processInputStream.getInput(), processOutputStream.getOutput(), processOutputStream.getOutput());
                } else {
                    mockServerManager = MockServerManagerProcess.create(managed, command);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (processName.startsWith("Server:")) {
            if (useRealServers) {
                testServerProcess = TestServerProcess.createServer(command.toArray(new String[command.size()]), processInputStream.getInput(), processOutputStream.getOutput(), processOutputStream.getOutput());

            } else {
                mockServerProcess = MockServerProcess.create(managed, processName, command, processInputStream.getInput());
            }
            System.err.println("Creating server");
        }

        factory.addProcessHandler(processName, this);
        return this;
    }

    public PrintStream getInputStream() {
        return processInputStream.getOutput();
    }

    public InputStream getErrorStream() {
        return processErrorStream.getInput();
    }

    public MockServerProcess getMockServerProcess() {
        return mockServerProcess;
    }

    public TestServerManagerProcess getServerManager() {
        return testServerManager;
    }

    public MockServerManagerProcess getMockServerManager() {
        return mockServerManager;
    }

    public TestServerProcess getTestServerProcess() {
        return testServerProcess;
    }

    private static class ProcessInputOutputPair {
        private static final int MAX_BUFFER = 100000;
        private final PrintStream output;
        private final PipedInputStream input;


        public ProcessInputOutputPair() throws IOException {
            this.input = new PipedInputStream(MAX_BUFFER);
            this.output = new PrintStream(new PipedOutputStream(input));
        }

        public PrintStream getOutput() {
            return output;
        }

        public InputStream getInput() {
            return input;
        }
    }
}
