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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ProcessManagerSlave;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.ProcessManagerSlave.Handler;
import org.jboss.as.process.support.TestFileUtils.TestFile;
import org.jboss.as.process.support.TestProcessUtils.TestProcessSenderStream;

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
    
    /** The process manager slave */
    private ProcessManagerSlave slave;
    
    /** The port on which the ProcessManager is listening */
    private final int port;

    /** True if this process has received the shutdown event */
    private final AtomicBoolean shutdown = new AtomicBoolean();

    /** The stream for sending data back to the test manager */
    private TestProcessSenderStream clientStream;
    
    /**
     * Constructor
     * 
     * @param processName the name of this process
     */
    protected AbstractProcess(String processName, int port) {
        this.processName = processName;
        this.port = port;
        file = TestFileUtils.getOutputFile(processName);
    }
    
    protected static Integer getPort(String[] args) {
        for (int i = 0 ; i < args.length - 1 ; i++) {
            if (args[i].equals(CommandLineConstants.INTERPROCESS_PORT)) {
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
     * Must be called after instantiating the process for the process to respond
     * to output. This initializes the 
     */
    protected void startSlave() {
        clientStream = TestProcessUtils.createProcessClient(processName);

        try {
            slave = new ProcessManagerSlave(processName, InetAddress.getLocalHost(), port, new TestHandler());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        Thread t = new Thread(this.slave.getController(), "Slave Process");
        t.start();
        started();

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
     * Callback for when the process receives a <code>handleMessage()</code>
     * call
     * 
     * @param sourceProcessName the name of the process sending the message
     * @param message the message
     */
    protected abstract void handleMessage(String sourceProcessName, byte[] message);

    /**
     * Callback for when the process receives a <code>handleMessage()</code>
     * call
     * @param sourceProcessName the name of the process sending the message
     * @param message the message
     */
    protected abstract void handleMessage(String sourceProcessName, List<String> message);

    /**
     * Callback for when the process receives a <code>shutdown()</code> call.
     */
    protected abstract void shutdown();
    
    /**
     * Send a message to another process via the slave
     * 
     * @param processName the name of the process
     * @param message the messages
     */
    protected void sendMessage(String processName, List<String> message){
        try {
            slave.sendMessage(processName, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Send a message to another process via the slave
     * 
     * @param processName the name of the process
     * @param message the message
     */
    protected void sendMessage(String processName, byte[] message){
        try {
            slave.sendMessage(processName, message, StreamUtils.calculateChecksum(message));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Broadcast a message to other processes via the slave
     * 
     * @param message the messages
     */
    protected void broadcastMessage(List<String> message){
        try {
            slave.broadcastMessage(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Add a process via the slave
     * 
     * @param processName the process name
     * @param classname the class name
     */
    protected void addProcess(String processName, String classname) {
        try {
            slave.addProcess(processName, TestProcessUtils.createCommand(processName, classname, port), System.getenv(), ".");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start a process via the slave
     * 
     * @param processName the process name
     */
    protected void startProcess(String processName) {
        try {
            slave.startProcess(processName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop a process via the slave
     * 
     * @param processName the process name
     */
    protected void stopProcess(String processName) {
        try {
            slave.stopProcess(processName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove a process via the slave
     * 
     * @param processName the process name
     */
    protected void removeProcess(String processName) {
        try {
            slave.removeProcess(processName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Broadcast a message to other processes via the slave
     * 
     * @param message the message
     */
    protected void broadcastMessage(final byte[] message){
        try {
            slave.broadcastMessage(message, StreamUtils.calculateChecksum(message));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class TestHandler implements Handler {

        @Override
        public void handleMessage(String sourceProcessName, byte[] message) {
            AbstractProcess.this.handleMessage(sourceProcessName, message);
        }

        @Override
        public void handleMessage(String sourceProcessName, List<String> message) {
            AbstractProcess.this.handleMessage(sourceProcessName, message);
        }

        @Override
        public void shutdown() {
            shutdown.set(true);
            AbstractProcess.this.shutdown();
            clientStream.shutdown();
        }
    }
}
