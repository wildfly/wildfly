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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.logging.NDC;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedProcess {
    private final ProcessManagerMaster master;
    private final String processName;
    private final List<String> command;
    private final Map<String, String> env;
    private final String workingDirectory;
    private final RespawnPolicy respawnPolicy;
    private final long[] startTimeHistory = new long[5];
    private final Logger log;

    private boolean stopped;
    private volatile boolean start;
    private final DelegatingSocketOutputStream commandStream = new DelegatingSocketOutputStream();
    private Process process;
    private List<StopProcessListener> stopProcessListeners;
    private int respawnCount;

    ManagedProcess(final ProcessManagerMaster master, final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory, final RespawnPolicy respawnPolicy) {
        this.master = master;
        this.processName = processName;
        this.command = command;
        this.env = env;
        this.workingDirectory = workingDirectory;
        this.respawnPolicy = respawnPolicy;
        this.log = Logger.getLogger("org.jboss.process." + processName);
    }

    public String getProcessName() {
        return processName;
    }

    synchronized void setSocket(Socket socket) throws IOException{
        log.info("Initializing socket for " + processName);
        commandStream.setSocketOutputStream(socket.getOutputStream());
        
        final OutputStreamHandler outputStreamHandler = new OutputStreamHandler(new BufferedInputStream(socket.getInputStream()));
        final Thread outputThread = new Thread(outputStreamHandler);
        outputThread.setName("Process " + processName + " socket reader thread");
        // todo - error handling in the event that a thread can't start?
        outputThread.start();
    }
    
    void start() throws IOException {
        log.info("Starting " + processName);
        start(false);
    }
    
    private void start(boolean isRespawn) throws IOException{
        synchronized (this) {
            if (start) {
                return;
            }
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            final Map<String, String> env = processBuilder.environment();
            env.clear();
            env.putAll(this.env);
            processBuilder.directory(new File(workingDirectory));
            final Process process;
            synchronized (ManagedProcess.class) {
                // this is the only point in the process manager which opens FDs OR fork/execs after initial boot.  By
                // restricting it to a single thread we reduce the risk of bogus FDs, resource leaks, and other
                // issues surrounding fork/exec vs. Java.
                process = processBuilder.start();
            }
            final ErrorStreamHandler errorStreamHandler = new ErrorStreamHandler(processName, process.getErrorStream());
            final Thread errorThread = new Thread(errorStreamHandler);
            errorThread.setName("Process " + processName + " stderr thread");
            // todo - error handling in the event that a thread can't start?
            errorThread.start();
            this.process = process;
            start = true;
            stopped = false;
            if (!isRespawn)
                respawnCount = 0;
        }
    }

    void stop() throws IOException {
        synchronized (this) {
            if (!start) {
                return;
            }
            stopped = true;
            final OutputStream stream = commandStream;
            StreamUtils.writeString(stream, "SHUTDOWN\n");
            stream.flush();
        }
    }

    boolean isStart() {
        return start;
    }

    void send(final String sender, final List<String> msg) throws IOException {
        final StringBuilder b = new StringBuilder();
        b.append("MSG");
        b.append('\0');
        b.append(sender);
        for (String s : msg) {
            b.append('\0').append(s);
        }
        b.append('\n');
        StreamUtils.writeString(commandStream, b);
        commandStream.flush();
    }
    
    void send(final String sender, final byte[] msg) throws IOException {
        final StringBuilder b = new StringBuilder();
        b.append("MSG_BYTES");
        b.append('\0');
        b.append(sender);
        b.append('\0');
        StreamUtils.writeString(commandStream, b.toString());
        StreamUtils.writeInt(commandStream, msg.length);
        commandStream.write(msg, 0, msg.length);
        StreamUtils.writeChar(commandStream, '\n');
        commandStream.flush();
    }

    private final class OutputStreamHandler implements Runnable {

        private final InputStream inputStream;

        OutputStreamHandler(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            
            // FIXME reliable transmission support (JBAS-8262)            
            final InputStream inputStream = this.inputStream;
            final StringBuilder b = new StringBuilder();
            try {
                for (;;) {
                    Status status = StreamUtils.readWord(inputStream, b);
                    if (status == Status.END_OF_STREAM) {
                        log.info("Received end of stream, shutting down " + processName);
                        commandStream.closeSocketOutputStream();
                        // no more input
                        return;
                    }
                    try {
                        final Command command = Command.valueOf(b.toString());
                        OUT: switch (command) {
                            case ADD: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                if (status != Status.MORE) {
                                    break;
                                }
                                final String name = b.toString();
                                status = StreamUtils.readWord(inputStream, b);
                                if (status != Status.MORE) {
                                    break;
                                }
                                final String workingDirectory = b.toString();
                                status = StreamUtils.readWord(inputStream, b);
                                if (status != Status.MORE) {
                                    break;
                                }
                                final String sizeString = b.toString();
                                final int size;
                                try {
                                    size = Integer.parseInt(sizeString, 10);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace(System.err); // FIXME remove
                                    break;
                                }
                                final List<String> execCmd = new ArrayList<String>();
                                for (int i = 0; i < size; i++) {
                                    status = StreamUtils.readWord(inputStream, b);
                                    if (status != Status.MORE) {
                                        break OUT;
                                    }
                                    execCmd.add(b.toString());
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                if (status != Status.MORE) {
                                    break;
                                }
                                final String mapSizeString = b.toString();
                                final int mapSize, lastEntry;
                                try {
                                    mapSize = Integer.parseInt(mapSizeString, 10);
                                    lastEntry = mapSize - 1;
                                } catch (NumberFormatException e) {
                                    e.printStackTrace(System.err); // FIXME remove
                                    break;
                                }
                                final Map<String, String> env = new HashMap<String, String>();
                                for (int i = 0; i < mapSize; i ++) {
                                    status = StreamUtils.readWord(inputStream, b);
                                    if (status != Status.MORE) {
                                        break OUT;
                                    }
                                    final String key = b.toString();
                                    status = StreamUtils.readWord(inputStream, b);
                                    if (status == Status.MORE || (i == lastEntry && status == Status.END_OF_LINE)) {
                                        env.put(key, b.toString());
                                    }
                                    else {
                                        break OUT;
                                    }                                    
                                }
                                master.addProcess(name, execCmd, env, workingDirectory);
                                break;
                            }
                            case START: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                final String name = b.toString();
                                master.startProcess(name);
                                break;
                            }
                            case STOP: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                final String name = b.toString();
                                master.stopProcess(name);
                                break;
                            }
                            case REMOVE: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                final String name = b.toString();
                                master.removeProcess(name);
                                break;
                            }
                            case SEND: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                final String recipient = b.toString();
                                final List<String> msg = new ArrayList<String>(0);
                                while (status == Status.MORE) {
                                    status = StreamUtils.readWord(inputStream, b);
                                    msg.add(b.toString());
                                }
                                master.sendMessage(processName, recipient, msg);
                                break;
                            }
                            case SEND_BYTES: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                status = StreamUtils.readWord(inputStream, b);
                                if (status == Status.MORE) {
                                    final String recipient = b.toString();
                                    master.sendMessage(processName, recipient, StreamUtils.readBytesWithLength(inputStream));
                                    status = StreamUtils.readStatus(inputStream);
                                }
                                break;
                            }
                            case BROADCAST: {
                                final List<String> msg = new ArrayList<String>(0);
                                while (status == Status.MORE) {
                                    status = StreamUtils.readWord(inputStream, b);
                                    msg.add(b.toString());
                                }
                                master.broadcastMessage(processName, msg);
                                break;
                            }
                            case BROADCAST_BYTES: {
                                if (status == Status.MORE) {
                                    master.broadcastMessage(processName, StreamUtils.readBytesWithLength(inputStream));
                                    status = StreamUtils.readStatus(inputStream);
                                }
                                break;
                            }
                            case LOG: {
                                if (status != Status.MORE) {
                                    break;
                                }
                                final byte[] bytes = StreamUtils.readBytesWithLength(inputStream);
                                status = StreamUtils.readStatus(inputStream);
                                master.logMessage(processName, bytes);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // unknown command...
                        log.error("Received unknown command: " + b.toString());
                    }
                    if (status == Status.MORE) StreamUtils.readToEol(inputStream);

                }
            } catch (Exception e) {
                // exception caught, shut down channel and exit
                log.error("Output stream handler for process " + processName + " caught an exception; shutting down", e);
                commandStream.closeSocketOutputStream();

            } finally {
                boolean respawn = false;
                safeClose(inputStream);
                int exitCode = 0;
                for (;;) try {
                    exitCode = process.waitFor();
                    synchronized (ManagedProcess.this) {
                        start = false;
                        if (exitCode != 0)
                            respawn = !stopped;
                    }
                    invokeStopProcessListeners(exitCode);
                    if (respawn)
                        respawn();
                    break;
                } catch (InterruptedException e) {
                }
            }
        }
        
        private void respawn() {
            long wait = 0;
            synchronized (this) {
                if (stopped || start)
                    return;
                wait = respawnPolicy.getTimeOutMs(++respawnCount);
                if (wait < 0){
                    log.info(processName + " has crashed " + respawnCount + " times, stopping it");
                    try {
                        stop();
                    } catch (IOException e) {
                        log.warn("Error stopping crashed " + processName, e);
                    }
                    return;
                }
            }
            
            try {
                TimeUnit.MILLISECONDS.sleep(wait);
            } catch (InterruptedException e) {
                log.info("Error waiting  " + wait + "ms to respawn crashed " + processName, e);
            }
            
            synchronized (this) {
                if (stopped || start)
                    return;
            }
            try {
                start(true);
            }catch(IOException e) {
                log.warn("Error respawning " + processName, e);
            }
        }
    }
    
    private static final class ErrorStreamHandler implements Runnable {
        private static final Logger log = Logger.getLogger("org.jboss.as.process.stderr");

        private final String processName;
        private final InputStream errorStream;

        private ErrorStreamHandler(final String processName, final InputStream errorStream) {
            this.processName = processName;
            this.errorStream = errorStream;
        }

        public void run() {
            NDC.push(processName);
            final int idx = NDC.getDepth();
            final Reader reader = new InputStreamReader(errorStream);
            final StringBuilder b = new StringBuilder();
            try {
                for (;;) {
                    try {
                        int c = reader.read();
                        if (c == -1) {
                            return;
                        }
                        if (c == '\n') {
                            if (b.length() > 0) {
                                log.error(b.toString());
                            }
                            b.setLength(0);
                        } else {
                            b.append((char) c);
                            if (b.length() >= 8192) {
                                log.error(b.toString());
                                b.setLength(0);
                            }
                        }
                    } catch (IOException e) {
                        safeClose(reader);
                        return;
                    } finally {
                        NDC.setMaxDepth(idx);
                    }
                }
            } finally {
                NDC.pop();
            }
        }
    }
    
    /**
     * OutputStream to buffer commands to a process until the socket has been initialized 
     * 
     */
    private class DelegatingSocketOutputStream extends OutputStream {
        private List<byte[]> bufferedBytes = new ArrayList<byte[]>(0);
        private volatile OutputStream current;
        private volatile ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        private volatile OutputStream realOut;
        
        @Override
        public void write(int b) throws IOException {
            if (current == null) {
                synchronized (this) {
                    if (current == null) {
                        if (realOut == null) {
                            bytesOut = new ByteArrayOutputStream();
                            current = bytesOut;
                        }else {
                            current = realOut;
                        }
                    }
                    
                }
            }
            current.write(b);
        }
        
        @Override
        public void flush() throws IOException {
            OutputStream curr = null;
            synchronized (this) {
                curr = current;
                if (bytesOut != null) {
                    if (bufferedBytes == null)
                         bufferedBytes = new ArrayList<byte[]>();
                    bufferedBytes.add(bytesOut.toByteArray());
                    bytesOut = null;
                    current = null;
                }
            }
            if (curr != null)
                curr.flush();
        }
        
        void setSocketOutputStream(OutputStream out) throws IOException {
            synchronized (this) {
                if (realOut != null) {
                    throw new IllegalStateException("There is already a socket output stream for " + processName);
                }
                realOut = new BufferedOutputStream(out);
                current = realOut;
                if (bufferedBytes != null) {
                    for (byte[] buf : bufferedBytes) {
                        current.write(buf);
                        current.flush();
                    }
                }
                if (bytesOut != null) {
                    byte[] bytes = bytesOut.toByteArray();
                    realOut.write(bytes);
                    out.flush();
                }

                safeClose(bytesOut);
                bytesOut = null;
                bufferedBytes.clear();
            }
        }
        
        void closeSocketOutputStream() {
            synchronized (this) {
                if (realOut == null) {
                    throw new IllegalStateException("The socket output stream was already closed " + processName);
                }
                safeClose(realOut);
                realOut = null;
                current = null;
            }
        }
    }

    private static void safeClose(final Closeable closeable) {
        try {
            closeable.close();
        } catch (Throwable ignored) {
        }
    }
    
    void registerStopProcessListener(StopProcessListener listener) {
        synchronized (this) {
            if (stopProcessListeners == null)
                stopProcessListeners = new ArrayList<StopProcessListener>();
            stopProcessListeners.add(listener);
        }
    }
    
    private void invokeStopProcessListeners(int exitCode) {
        List<StopProcessListener> listeners = null;
        synchronized (ManagedProcess.this) {
            listeners = stopProcessListeners;
        }
        if (listeners != null) {
            for (StopProcessListener listener : listeners)
                listener.processStopped(exitCode);
        }
    }
    
    interface StopProcessListener{
        void processStopped(int exitCode);
    }    
}
