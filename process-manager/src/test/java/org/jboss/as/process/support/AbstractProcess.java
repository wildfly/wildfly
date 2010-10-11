/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.process.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommand;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommandHandler;
import org.jboss.as.process.support.TestFileUtils.TestFile;
import org.jboss.as.process.support.TestProcessUtils.TestProcessSenderStream;
import org.jboss.as.protocol.Status;
import org.jboss.as.protocol.StreamUtils;

/**
 * Abstract base class for processes started by the tests. A processes main
 * method must instantiate the test using the first argument as the processName
 * parameter ending up in {@link AbstractProcess#AbstractProcess(String)} and
 * then call {@link #startSlave()}, i.e.
 *
 * <pre>
 * public static void main(String[] args) {
 *     SpecificProcess = new SpecificProcess(args[0]);
 *     startSlave();
 * }
 * </pre>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractProcess {

    /** The output file */
    private final TestFile file;

    /** The name of this process */
    protected final String processName;

    /** True if this process has received the shutdown event */
    private final AtomicBoolean shutdown = new AtomicBoolean();

    /** The stream for sending data back to the test manager */
    private final TestProcessSenderStream clientStream;

    private final SocketConnection conn;

    private final TestHandler handler = new TestHandler();

    /**
     * Constructor
     *
     * @param processName the name of this process
     */
    protected AbstractProcess(String processName, int port){
        this.processName = processName;
        file = TestFileUtils.getOutputFile(processName);
        try {
            conn = SocketConnection.connect(InetAddress.getLocalHost(), port, "CONNECTED", processName);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        clientStream = TestProcessUtils.createProcessClient(processName);
    }

    protected static Integer getPort(String[] args) {
        for (int i = 0 ; i < args.length - 1 ; i++) {
            if (args[i].equals(CommandLineConstants.INTERPROCESS_PM_PORT)) {
                return Integer.valueOf(args[++i]);
            }
        }
        return null;
    }

    /**
     * Writes the string to this processes output stream and to its output file
     *
     * @param s the string to write
     */
    protected void writeData(String s) {
        file.writeToFile(s + "\n");
        clientStream.writeData(s);
    }

    /**
     * Write a message to this processes System.err
     *
     * @param processName the name of the process
     * @param msg the message
     */
    protected static void debug(String processName, String msg) {
        System.err.println("(remote-process-debug): " + processName + "-" + msg);
    }

    /**
     * Write a message to this processes System.err
     *
     * @param msg the message
     */
    protected void debug(String msg) {
        debug(processName, msg);
    }


    /**
     * Check if the process has been shutdown, useful for process specific
     * worker threads
     *
     * @return true if the process has been shut down
     */
    protected synchronized boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * Called once {@link #startSlave()} has been called
     */
    protected abstract void started();

    /**
     * Callback for when the process receives a <code>shutdown()</code> call.
     */
    protected abstract void shutdown();

    /**
     * Callback for when the process receives a <code>down()</code> call.
     */
    protected void down(String downProcessName) {

    }

    /**
     * Callback for when the process receives a <code>shutdownServers()</code> call.
     */
    protected void shutdownServers() {

    }

    protected void handleTestCommand(String cmd) {

    }

    protected void startThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = conn.getInputStream();
                StringBuilder b = new StringBuilder();
                try {
                    while (!shutdown.get()) {
                        Status status = StreamUtils.readWord(in, b);

                        try {

                            final OutgoingPmCommand command = OutgoingPmCommand.valueOf(b.toString());
                            status = command.handleMessage(in, status, handler, b);
                        } catch (IllegalArgumentException e) {
                            // unknown command...
                        }

                        if (status == Status.MORE) StreamUtils.readToEol(in);
                    }
                } catch (IOException e) {
                    handler.handleShutdown();
                }
            }
        }).start();

        new Thread (new Runnable() {

            @Override
            public void run() {
                InputStream in = clientStream.getInput();
                StringBuilder b = new StringBuilder();
                try {
                    while (!shutdown.get()) {
                        Status status = StreamUtils.readWord(in, b);
                        handleTestCommand(b.toString());

                        if (status == Status.MORE) StreamUtils.readToEol(in);
                    }
                } catch (IOException e) {
                    handler.handleShutdown();
                }
            }
        }).start();

        new Thread (new Runnable() {

            @Override
            public void run() {
                debug("Monitor stdin " + processName);
                InputStream in = System.in;
                try {
                    while (in != null && in.read() != -1) {

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                debug("End of stdin " + processName);
                handler.handleShutdown();
                System.exit(0);
            }
        }).start();

        started();
    }

    private class TestHandler implements OutgoingPmCommandHandler {
        @Override
        public void handleShutdown() {
            if (!shutdown.getAndSet(true)) {
                AbstractProcess.this.shutdown();
                clientStream.shutdown();
            }
        }

		@Override
        public void handleDown(String downProcessName) {
			AbstractProcess.this.down(downProcessName);
        }

        @Override
        public void handleReconnectServerManager(String address, String port) {
        }
    }
}
