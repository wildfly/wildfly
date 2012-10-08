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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.as.process.CommandLineConstants;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class TestProcessUtils {

    private static final int DEFAULT_PORT = 12934;
    private static final int PORT;
    static {
        String s = System.getenv().get("PM_TEST_LISTENER_PORT");
        PORT = s == null ? DEFAULT_PORT : Integer.valueOf(s);
    }

    private static final int TIMEOUT_MILLISECONDS = 1000;

    static final Map<String, TestCommand> COMMANDS;
    static {
        Map<String, TestCommand> map = new HashMap<String, TestCommand>();
        StartCommand start = new StartCommand();
        map.put(start.cmd(), start);
        MessageCommand msg = new MessageCommand();
        map.put(msg.cmd(), msg);
        StopCommand stop = new StopCommand();
        map.put(stop.cmd(), stop);
        COMMANDS = Collections.unmodifiableMap(map);
    }

    public static List<String> createCommand(String processName, String classname, int pmPort)  throws UnknownHostException {
        return createCommand(processName, classname, pmPort, 0, false);
    }

    public static List<String> createCommand(String processName, String classname, int pmPort,
            int debugPort, boolean suspend) throws UnknownHostException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(getJava());
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));

        if (debugPort > 0)
            cmd.add("-agentlib:jdwp=transport=dt_socket,address=" + debugPort
                    + ",server=y,suspend=" + (suspend ? "y" : "n"));

        cmd.add(classname);
        cmd.add(processName);

        //Add the socket parameters
        cmd.add(CommandLineConstants.PROCESS_CONTROLLER_BIND_PORT);
        cmd.add(String.valueOf(pmPort));
        cmd.add(CommandLineConstants.PROCESS_CONTROLLER_BIND_ADDR);
        cmd.add(InetAddress.getLocalHost().getHostAddress());

        return cmd;
    }

    public static TestStreamManager createStreamManager(TestProcessController controller) {
        ServerSocketThread serverSocketThread = new ServerSocketThread(
                controller);
        serverSocketThread.start();
        serverSocketThread.waitForStart();
        return serverSocketThread;
    }

    public static TestProcessSenderStream createProcessClient(String processName) {
        return new ClientSocketWriter(processName);
    }

    private static class ServerSocketThread extends Thread implements TestStreamManager {
        TestProcessController controller;
        ServerSocket server;
        Set<ListenerSocketThread> listenerThreads = new HashSet<ListenerSocketThread>();
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, ListenerSocketThread> listenerThreadsByProcessName = new HashMap<String, ListenerSocketThread>();

        Map<String, CountDownLatch> startProcessLatches = new HashMap<String, CountDownLatch>();
        Map<String, CountDownLatch> stopProcessLatches = new HashMap<String, CountDownLatch>();

        ServerSocketThread(TestProcessController controller) {
            super("Test Server Socket Thread");
            this.controller = controller;
            try {
                server = new ServerSocket();
                server.setReuseAddress(true);
                SocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), PORT);
                System.err.println("*Test - " + this.getName() + " attempting to listen on " + PORT);

                //ServerSocket.close() does not seem to always free up the port. I don't really want
                //to modify everything to pass ports through to the tests yet, so retry a few times
                for (int i = 0 ; ; i++) {
                    try {
                        server.bind(addr, 5);
                        break;
                    } catch (BindException e) {
                        if (i == 5)
                            throw e;
                        Thread.sleep(100);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Can't listen on port " + PORT, e);
            }
        }

        @Override
        public void shutdown() {
            System.err.println("*Test - " + this.getName() + " closing server");

            closeServer();
            synchronized (this) {
                for (ListenerSocketThread listener : listenerThreads) {
                    listener.shutdown();
                    if (listener.getProcessName() != null)
                        controller.stopProcess(listener.getProcessName());
                }

                for (ListenerSocketThread listener : listenerThreadsByProcessName.values()) {
                    listener.shutdown();
                    if (listener.getProcessName() != null)
                        controller.stopProcess(listener.getProcessName());
                }
                listenerThreadsByProcessName.clear();
            }
        }

        @Override
        public TestProcessListenerStream getProcessListener(String name, long timeoutMillis) {
            long cutoff = System.currentTimeMillis() + timeoutMillis;

            TestProcessListenerStream stream = null;
            do {
                synchronized (this) {
                    stream = listenerThreadsByProcessName.get(name);
                }
                if (stream == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignore) {
                    }
                }
            }while (stream == null && System.currentTimeMillis() < cutoff);

            return stream;
        }

        @Override
        public TestProcessListenerStream createProcessListener(String name) {
            return createProcessListener(name, null);
        }

        @Override
        public TestProcessListenerStream createProcessListener(String name, Runnable preWait) {
            CountDownLatch processLatch = null;
            synchronized (this) {
                ListenerSocketThread thread = listenerThreadsByProcessName
                        .get(name);
                if (thread != null)
                    return thread;

                processLatch = new CountDownLatch(1);
                startProcessLatches.put(name, processLatch);
            }

            try {
                System.err.println("*Test - Starting " + name + " " + processLatch);
                controller.startProcess(name);
                if (preWait != null)
                    preWait.run();
                processLatch.await(TIMEOUT_MILLISECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.err.println("*Test - Started " + name);
            synchronized (this) {
                ListenerSocketThread thread = listenerThreadsByProcessName
                        .get(name);
                if (thread == null)
                    throw new IllegalStateException("No process called " + name);
                return thread;
            }
        }



        public void detachProcessListener(String name) {
            synchronized (this) {
                listenerThreadsByProcessName.remove(name);
            }
        }

        void processStarted(String name, ListenerSocketThread thread) {
            System.err.println("*Test - Start received for " + name + " " + startProcessLatches);
            synchronized (this) {
                countdownLatch(startProcessLatches, name);
                listenerThreadsByProcessName.put(name, thread);
            }
        }

        @Override
        public void stopProcessListener(String name) {
            if (name == null)
                throw new IllegalArgumentException("Null name");
            CountDownLatch processLatch = null;

            synchronized (this) {
                ListenerSocketThread thread = null;
                thread = listenerThreadsByProcessName
                        .get(name);

                if (thread == null)
                    return;

                processLatch = new CountDownLatch(1);
                stopProcessLatches.put(name, processLatch);
                System.err.println("*Test - Stopping " + name + " "
                        + stopProcessLatches);
                controller.stopProcess(name);
                System.err.println("*Test - Waiting for stop " + name);
            }

            try {
                processLatch.await(TIMEOUT_MILLISECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.err.println("*Test - Stopped " + name);
        }

        void processStopped(String name, ListenerSocketThread thread) {
            System.err.println("*Test - Stop received for " + name + " "  + stopProcessLatches);
            synchronized (this) {
                countdownLatch(stopProcessLatches, name);
                listenerThreadsByProcessName.remove(name);
            }
        }

        void countdownLatch(Map<String, CountDownLatch> latches, String processName) {
            CountDownLatch processLatch = latches.remove(processName);
            if (processLatch != null) {
                processLatch.countDown();
            }
        }

        void waitForStart() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                latch.countDown();
                System.err.println("*Test - Server listening on " + server.getLocalPort());
                while (true) {
                    ListenerSocketThread t = new ListenerSocketThread(this,
                            server.accept());
                    t.start();
                    synchronized (this) {
                        listenerThreads.add(t);
                    }
                }
            } catch (SocketException e) {
                System.err.println("*Test - " + this.getName() + " server socket closed");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeServer();
            }
        }

        private synchronized void closeServer() {
            try {
                if (!server.isClosed())
                    server.close();
                System.err.println("*Test - server socket closed " + server.isClosed() + " " + server.isClosed());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void removeThread(ListenerSocketThread listener) {
            synchronized (this) {
                listenerThreads.remove(listener);
            }
        }
    }

    private static class ListenerSocketThread extends Thread implements TestProcessListenerStream {
        volatile String processName;
        final ServerSocketThread serverSocketThread;
        final Socket socket;
        final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();

        public ListenerSocketThread(ServerSocketThread serverSocketThread,
                Socket socket) {
            super("Test Listener Socket Thread");
            this.serverSocketThread = serverSocketThread;
            this.socket = socket;
        }

        public void exitProcess(int exitCode) throws IOException {
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            String cmd = "Exit" + exitCode + "\n";
            out.write(cmd.getBytes());
            out.flush();
        }

        public void shutdown() {
            closeSocket(socket);
        }

        @Override
        public String getProcessName() {
            return processName;
        }

        @Override
        public void run() {
            try {
                System.err.println("*Test - Listener started");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                boolean done = false;
                while (!done) {
                    String line = in.readLine();

                    System.err.println("*Test - " + processName + " listener got data " + line);

                    if (line != null)
                        TestCommand.receive(this, line.trim());
                    else
                        done = true;
                }
            } catch (SocketException e) {
                System.err.println("*Test - " + this.getName() + " socket closed");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                serverSocketThread.removeThread(this);
                closeSocket(socket);
            }
        }

        void processStarted(String processName) {
            // TODO Check we don't get started twice
            this.processName = processName;
            serverSocketThread.processStarted(processName, this);
        }

        void processStopped(String processName) {
            serverSocketThread.processStopped(processName, this);
        }

        @Override
        public String readMessage() {
            return readMessage(TIMEOUT_MILLISECONDS);
        }

        @Override
        public String readMessage(long timeoutMs) {
            try {
                return messages.poll(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void messageReceived(String msg) {
            messages.add(msg);
        }
    }

    public static class ClientSocketWriter implements TestProcessSenderStream {
        final String processName;
        final Socket socket;
        final InputStream socketInput;
        final PrintWriter socketOutput;

        public ClientSocketWriter(String processName) {
            this.processName = processName;
            try {
                socket = new Socket(InetAddress.getLocalHost(), PORT);
                socketOutput = new PrintWriter(socket.getOutputStream(), true);
                socketOutput.println(new StartCommand(processName));
                socketInput = new BufferedInputStream(socket.getInputStream());
            } catch (Exception e) {
                e.printStackTrace(System.err);
                shutdown();
                throw new RuntimeException(e);
            }

        }

        public synchronized void shutdown() {
            if (socketOutput != null) {
                try {
                    socketOutput.println(new StopCommand(processName).formatCommandForSend());
                    socketOutput.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            closeSocket(socket);
        }

        public void writeData(String msg) {
            try {
                socketOutput.println(new MessageCommand(processName, msg).formatCommandForSend());
            } catch (Exception e) {
                shutdown();
            }
        }

        @Override
        public InputStream getInput() {
            return socketInput;
        }
    }

    static void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getJava() {
        return System.getProperty("java.home") + File.separator + "bin"
                + File.separator + "java";
    }


    private static abstract class TestCommand {
        String processName;

        TestCommand() {
        }

        TestCommand(String processName) {
            this.processName = processName;
        }

        static void receive(ListenerSocketThread listener, String cmdString) throws IOException {
            String[] elements = cmdString.split(":");
            if (elements.length < 2)
                throw new IllegalArgumentException("'" + cmdString
                        + "' could not be parsed into a command");

            TestCommand cmd = COMMANDS.get(elements[0]);
            if (cmd == null)
                throw new IllegalArgumentException("'" + cmdString
                        + "' is not a known command: " + cmdString);

            elements = Arrays.copyOfRange(elements, 1, elements.length);
            cmd.receive(listener, elements);
        }

        abstract String cmd();

        abstract void receive(ListenerSocketThread listener, String[] cmdData) throws IOException;

        public String formatCommandForSend() {
            return cmd() + ":" + processName;
        }

        @Override
        public String toString() {
            return cmd() + ":" + processName;
        }

    }

    private static class StartCommand extends TestCommand {
        StartCommand() {
        }

        StartCommand(String processName) {
            super(processName);
        }

        @Override
        String cmd() {
            return "START";
        }

        @Override
        void receive(ListenerSocketThread listener, String[] cmdData) {
            if (cmdData.length != 1)
                throw new IllegalArgumentException("Invalid START data "
                        + Arrays.toString(cmdData));
            listener.processStarted(cmdData[0]);
        }
    }

    private static class StopCommand extends TestCommand {
        StopCommand() {
        }

        StopCommand(String processName) {
            super(processName);
        }

        @Override
        String cmd() {
            return "STOP";
        }

        @Override
        void receive(ListenerSocketThread listener, String[] cmdData) {
            if (cmdData.length != 1)
                throw new IllegalArgumentException("Invalid START data "
                        + Arrays.toString(cmdData));
            listener.processStopped(cmdData[0]);
        }
    }

    private static class MessageCommand extends TestCommand {
        String command;

        MessageCommand() {
        }

        public MessageCommand(String processName, String command) {
            super(processName);
            this.command = command;
        }

        @Override
        String cmd() {
            return "MSG";
        }

        @Override
        void receive(ListenerSocketThread listener, String[] cmdData) {
            if (cmdData.length != 2)
                throw new IllegalArgumentException("Invalid MSG data "
                        + Arrays.toString(cmdData));
            listener.messageReceived(cmdData[1]);
        }

        @Override
        public String formatCommandForSend() {
            return super.toString() + ":" + command;
        }

        @Override
        public String toString() {
            return cmd() + ":" + processName + ":" + command;
        }
    }

    public interface TestStreamManager {
        void shutdown();

        TestProcessListenerStream createProcessListener(String name, Runnable preWait);

        TestProcessListenerStream createProcessListener(String name);

        TestProcessListenerStream getProcessListener(String name, long timeoutMillis);

        void stopProcessListener(String name);

        void detachProcessListener(String name);
    }

    public interface TestProcessListenerStream {
        String getProcessName();

        String readMessage();

        String readMessage(long timeoutMs);

        void shutdown();

        void exitProcess(int exitCode) throws IOException;
    }

    public interface TestProcessSenderStream {
        void shutdown();

        void writeData(String msg);

        InputStream getInput();
    }

    public interface TestProcessController {
        void startProcess(String processName);

        void stopProcess(String processName);
    }

}
