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

package org.jboss.as.process;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.process.ManagedProcess.StopProcessListener;
import org.jboss.logging.Logger;

/**
 * Process manager main entry point.  The thin process manager process is implemented here.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerMaster {

    private static final String SERVER_MANAGER_PROCESS_NAME = "ServerManager";

    private final ServerSocketListener serverSocketListener;
    
    private final Logger log = Logger.getLogger(ProcessManagerMaster.class);
    
    private final Map<String, ManagedProcess> processes = new HashMap<String, ManagedProcess>();
    
    private final AtomicBoolean shutdown = new AtomicBoolean();

    ProcessManagerMaster(InetAddress addr, int port) throws UnknownHostException, IOException {
        serverSocketListener = createServerSocketListener(addr, port);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                shutdown();
            }
        }, "Shutdown Hook"));
    }
    
    public static void main(String[] args) throws Exception{
        ParsedArgs parsedArgs = ParsedArgs.parse(args);
        if (parsedArgs == null) {
            return;
        }
        final ProcessManagerMaster master = new ProcessManagerMaster(parsedArgs.address, parsedArgs.port);
        master.start();
        
        List<String> command = new ArrayList<String>(parsedArgs.command);
        command.add(CommandLineConstants.INTERPROCESS_ADDRESS);
        command.add(master.getInetAddress().getHostAddress());
        command.add(CommandLineConstants.INTERPROCESS_PORT);
        command.add(String.valueOf(master.getPort()));
        command.add(CommandLineConstants.INTERPROCESS_NAME);
        command.add(SERVER_MANAGER_PROCESS_NAME);
        
        master.addProcess(SERVER_MANAGER_PROCESS_NAME, command, System.getenv(), parsedArgs.workingDir);
        master.startProcess(SERVER_MANAGER_PROCESS_NAME);
    }

    synchronized void start() {
        Thread t = new Thread(serverSocketListener);
        t.start();
    }
    
    void shutdown() {
        boolean isShutdown = shutdown.getAndSet(true);
        if (isShutdown)
            return;
            
        serverSocketListener.shutdown();
        synchronized (processes) {
            for (ManagedProcess proc : processes.values()) {
                if (proc.isStart()) {
                    try {
                        proc.stop();
                    } catch (IOException e) {
                    }
                }
            }
            processes.clear();
        }
        
    }
    
    InetAddress getInetAddress() {
        return serverSocketListener.getAddress();
    }
    
    Integer getPort() {
        return serverSocketListener.getPort();
    }

    void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) {
        addProcess(processName, command, env, workingDirectory, RespawnPolicy.DefaultRespawnPolicy.INSTANCE);
    }

    void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory, RespawnPolicy respawnPolicy) {
        
        if (shutdown.get())
            return;
        
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            if (processes.containsKey(processName)) {
                log.debugf("already have process %s", processName);
                // ignore
                return;
            }
            
            final ManagedProcess process = new ManagedProcess(this, processName, command, env, workingDirectory, respawnPolicy);
            processes.put(processName, process);
        }
    }

    void startProcess(final String processName) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                // ignore
                return;
            }
            try {
                process.start();
            } catch (IOException e) {
                // todo log it
            }
        }
    }

    void stopProcess(final String processName) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                // ignore
                return;
            }
            try {
                process.stop();
            } catch (IOException e) {
                // todo log it
            }
        }
    }

    void removeProcess(final String processName) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                // ignore
                return;
            }
            synchronized (process) {
                if (process.isStart()) {
                    log.debugf("Ignoring remove request for running process %s", processName);
                    return;
                }
                processes.remove(processName);
            }
        }
    }

    void sendMessage(final String sender, final String recipient, final List<String> msg) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(recipient);
            if (process == null) {
                // ignore
                return;
            }
            synchronized (process) {
                if (! process.isStart()) {
                    // ignore
                    return;
                }
                try {
                    process.send(sender, msg);
                } catch (IOException e) {
                    // todo log it
                }
            }
        }
    }
    
    void sendMessage(final String sender, final String recipient, final byte[] msg, long chksum) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(recipient);
            if (process == null) {
                // ignore
                return;
            }
            synchronized (process) {
                if (! process.isStart()) {
                    // ignore
                    return;
                }
                try {
                    process.send(sender, msg, chksum);
                } catch (IOException e) {
                    // todo log it
                }
            }
        }
        
    }

    void broadcastMessage(final String sender, final List<String> msg) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            for (ManagedProcess process : processes.values()) {
                synchronized (process) {
                    if (! process.isStart()) {
                        // ignore and go on with the other processes
                        continue;
                    }
                    try {
                        process.send(sender, msg);
                    } catch (IOException e) {
                        // todo log it
                    }
                }
            }
        }
    }
    
    void broadcastMessage(final String sender, final byte[] msg, final long chksum) {
        if (shutdown.get())
            return;

        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            for (ManagedProcess process : processes.values()) {
                synchronized (process) {
                    if (! process.isStart()) {
                        // ignore and go on with the other processes
                        continue;
                    }
                    try {
                        process.send(sender, msg, chksum);
                    } catch (IOException e) {
                        // todo log it
                    }
                }
            }
        }
    }
    
    void registerStopProcessListener(final String name, final StopProcessListener listener) {
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            ManagedProcess process = processes.get(name);
            if (process == null)
                return;
            process.registerStopProcessListener(listener);
        }
    }
    
    List<String> getProcessNames(final boolean onlyStarted) {
        final Map<String, ManagedProcess> processes = this.processes;
        
        synchronized (processes) {
            if (onlyStarted) {
                List<String> started = new ArrayList<String>();
                for (Map.Entry<String, ManagedProcess> entry : processes.entrySet()) {
                    if (entry.getValue().isStart()) {
                        started.add(entry.getKey());
                    }
                }
                return started;
            }
            else
                return new ArrayList<String>(processes.keySet());
        }
    }

    ServerSocketListener createServerSocketListener(InetAddress addr, Integer port) throws UnknownHostException, IOException{
        if (addr == null)
            addr = InetAddress.getLocalHost();
        if (port == null)
            port = 0;

        
        ServerSocket serverSocket = new ServerSocket();
        SocketAddress saddr = new InetSocketAddress(addr, port);
        serverSocket.setReuseAddress(true);
        serverSocket.bind(saddr, 20);
        return new ServerSocketListener(serverSocket);
    }
    
    private static class ParsedArgs {
        final Integer port;
        final InetAddress address;
        final String workingDir;
        final List<String> command;
        
        ParsedArgs(Integer port, InetAddress address, String workingDir, List<String> command){
            this.port = port;
            this.address = address;
            this.workingDir = workingDir;
            this.command = command;
        }

        static ParsedArgs parse(String[] args) {
            Integer port = null;
            InetAddress address = null;
            String workingDir = args[0];
            int i = 1;
            for (; i < args.length ; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.equals(CommandLineConstants.INTERPROCESS_PORT)) {
                        try {
                            port = Integer.valueOf(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.printf("Value for %s is not an Integer -- %s\n", CommandLineConstants.INTERPROCESS_PORT, args[i]);
                            return null;
                        }
    
                    }
                    else if (arg.equals(CommandLineConstants.INTERPROCESS_ADDRESS)) {
                        try {
                            address = InetAddress.getByName(args[++i]);
                        } catch (UnknownHostException e) {
                            System.err.printf("Value for %s-interprocess-address is not a known host -- %s\n", CommandLineConstants.INTERPROCESS_ADDRESS, args[i]);
                            return null;
                        }
                    }
                }
                else {
                    break;
                }
            }
            List<String> command = new ArrayList<String>(Arrays.asList(args).subList(i, args.length));

            
            port = port != null ? port : Integer.valueOf(0);
            if (address == null) {
                try {
                    address = InetAddress.getLocalHost(); 
                } catch (UnknownHostException e) {
                    System.err.printf("Could not determine local host");
                    return null;
                }
            }
       
            return new ParsedArgs(port, address, workingDir, command);
        }
    }

    /**
     * Contains the server socket that is listening for requests from client processes. 
     * When a request is accepted, the new socket is handed off to ProcessAcceptorTask
     * which is executed in a separate thread.
     * 
     */
    class ServerSocketListener implements Runnable {
        
        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        
        private ServerSocketListener(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }
        
        InetAddress getAddress() {
            return serverSocket.getInetAddress();
        }
        
        @Override
        public void run() {
            boolean done = false;
            log.infof("PM listening on " + serverSocket.getLocalPort());
            while (!done) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.execute(new ProcessAcceptorTask(socket));
                } catch (SocketException e) {
                    log.info("PM closed server socket");
                    done = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void shutdown() {
            executor.shutdown();
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Takes a newly created socket and listens for the first request,
     * which should contain the name of the connecting process. If the
     * request is valid, the socket is handed off to the ManagedProcess
     * that has that name.
     * <p>
     * If the request is not valid, or there is no ManagedProcess with 
     * that name the socket is closed. 
     */
    class ProcessAcceptorTask implements Runnable {
        private final Socket socket;
        
        public ProcessAcceptorTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            boolean ok = false; 
            try {
                InputStream in = socket.getInputStream();
                StringBuilder sb = new StringBuilder();
                
                //TODO Timeout on the read?
                Status status = StreamUtils.readWord(in, sb);
                if (status != Status.MORE) {
                    log.errorf("Process acceptor: received '%s' but no more", sb.toString());
                    return;
                }
                if (!sb.toString().equals("STARTED")) {
                    log.errorf("Process acceptor: received unknown start command '%s'", sb.toString());
                    return;
                }
                sb = new StringBuilder();
                while (status == Status.MORE) {
                    status = StreamUtils.readWord(in, sb);
                }
                String processName = sb.toString();
                
                final Map<String, ManagedProcess> processes = ProcessManagerMaster.this.processes;
                ManagedProcess process = null;
                synchronized (processes) {
                    process = processes.get(processName);
                    if (process == null) {
                        log.errorf("Process acceptor: received start command for unknown process '%s'", processName + "(" +  processes.keySet() + ")");
                        return;
                    }
                    if (!process.isStart()) {
                        log.errorf("Process acceptor: received start command for not started process '" + process + "'");
                        return;
                    }
                }
                process.setSocket(socket);
                
                ok = true;
                
            } catch (IOException e) {
                log.errorf("Process acceptor: error reading from socket: %s", e.getMessage());
            }
            finally {
                if (!ok){
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }
}
