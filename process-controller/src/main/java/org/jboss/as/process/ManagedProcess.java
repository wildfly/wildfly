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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.jboss.as.protocol.old.StreamUtils;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;

/**
 * A managed process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
final class ManagedProcess {

    private final String processName;
    private final List<String> command;
    private final Map<String, String> env;
    private final String workingDirectory;
    private final Logger log;
    private final Object lock;

    private final ProcessController processController;
    private final byte[] authKey;
    private final boolean isInitial;

    private OutputStream stdin;
    private State state = State.DOWN;
    private Process process;
    private boolean shutdown;

    public byte[] getAuthKey() {
        return authKey;
    }

    public boolean isInitial() {
        return isInitial;
    }

    public boolean isRunning() {
        synchronized (lock) {
            return state == State.STARTED;
        }
    }

    enum State {
        DOWN,
        STARTED,
        STOPPING,
    }

    ManagedProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory, final Object lock, final ProcessController controller, final byte[] authKey, final boolean initial) {
        if (processName == null) {
            throw new IllegalArgumentException("processName is null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        if (env == null) {
            throw new IllegalArgumentException("env is null");
        }
        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory is null");
        }
        if (lock == null) {
            throw new IllegalArgumentException("lock is null");
        }
        if (controller == null) {
            throw new IllegalArgumentException("controller is null");
        }
        if (authKey == null) {
            throw new IllegalArgumentException("authKey is null");
        }
        if (authKey.length != 16) {
            throw new IllegalArgumentException("authKey length is invalid");
        }
        this.processName = processName;
        this.command = command;
        this.env = env;
        this.workingDirectory = workingDirectory;
        this.lock = lock;
        processController = controller;
        this.authKey = authKey;
        isInitial = initial;
        log = Logger.getLogger("org.jboss.as.process." + processName + ".status");
    }

    public String getProcessName() {
        return processName;
    }

    public void start() {
        synchronized (lock) {
            if (state != State.DOWN) {
                log.debugf("Attempted to start already-running process '%s'", processName);
                return;
            }
            doStart(false);
        }
    }

    public void sendStdin(final InputStream msg) {
        try {
            StreamUtils.copyStream(msg, stdin);
            stdin.flush();
        } catch (IOException e) {
            log.errorf(e, "Failed to send data bytes to process '%s' input stream", processName);
        }
    }


    public void reconnect(String hostName, int port) {
        try {
            StreamUtils.writeUTFZBytes(stdin, hostName);
            StreamUtils.writeInt(stdin, port);
            stdin.flush();
        } catch (IOException e) {
            log.errorf(e, "Failed to send reconnect message to process '%s' input stream", processName);
        }
    }

    private void doStart(boolean restart) {
        // Call under lock
        if (restart && isInitial() && !command.contains(CommandLineConstants.RESTART_HOST_CONTROLLER)){
            //Add the restart flag to the HC process if we are respawning it
            command.add(CommandLineConstants.RESTART_HOST_CONTROLLER);
        }
        log.infof("Starting process '%s'", processName);
        log.debugf("Process name='%s' command='%s' workingDirectory='%s'", processName, command, workingDirectory);
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(env);
        builder.directory(new File(workingDirectory));
        final Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            log.errorf(e, "Failed to start process '%s'", processName);
            return;
        }
        final long startTime = System.currentTimeMillis();
        final OutputStream stdin = process.getOutputStream();
        final InputStream stderr = process.getErrorStream();
        final InputStream stdout = process.getInputStream();
        final Thread stderrThread = new Thread(new ReadTask(stderr, processController.getStderr()));
        stderrThread.setName(String.format("stderr for %s", processName));
        stderrThread.start();
        final Thread stdoutThread = new Thread(new ReadTask(stdout, processController.getStdout()));
        stdoutThread.setName(String.format("stdout for %s", processName));
        stdoutThread.start();
        final Thread joinThread = new Thread(new JoinTask(startTime));
        joinThread.setName(String.format("reaper for %s", processName));
        joinThread.start();
        try {
            stdin.write(authKey);
            stdin.flush();
        } catch (Exception e) {
            log.warnf("Failed to send authentication key to process '%s': %s", processName, e);
        }
        state = State.STARTED;
        this.process = process;
        this.stdin = stdin;
        processController.processStarted(processName);
        return;
    }

    public void stop() {
        synchronized (lock) {
            if (state != State.STARTED) {
                log.debugf("Attempted to stop already-stopping or down process '%s'", processName);
                return;
            }
            log.infof("Stopping process '%s'", processName);
            StreamUtils.safeClose(stdin);
            state = State.STOPPING;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            if (state == State.STARTED) {
                log.infof("Stopping process '%s'", processName);
                StreamUtils.safeClose(stdin);
            }
            state = State.STOPPING;
        }
    }

    private final class JoinTask implements Runnable {
        private final long startTime;

        public JoinTask(final long startTime) {
            this.startTime = startTime;
        }

        public void run() {
            final Process process;
            synchronized (lock) {
                process = ManagedProcess.this.process;
            }
            int exitCode;
            for (;;) try {
                exitCode = process.waitFor();
                log.infof("Process '%s' finished with an exit status of %d", processName, Integer.valueOf(exitCode));
                break;
            } catch (InterruptedException e) {
                // ignore
            }
            synchronized (lock) {
                final long endTime = System.currentTimeMillis();
                state = State.DOWN;
                if (shutdown) {
                    processController.removeProcess(processName);
                } else if (isInitial() && exitCode == 99) {
                    // Host Controller abort
                    processController.removeProcess(processName);
                    new Thread(new Runnable() {
                        public void run() {
                            processController.shutdown();
                            System.exit(0);
                        }
                    }).start();
                } else {
                    processController.processStopped(processName, endTime - startTime);
                    if (isInitial()) {
                        // we must respawn the initial process
                        doStart(true);
                        // TODO: throttle policy
                    }
                }
            }
        }
    }

    private final class ReadTask implements Runnable {
        private final InputStream source;
        private final PrintStream target;

        private ReadTask(final InputStream source, final PrintStream target) {
            this.source = source;
            this.target = target;
        }

        public void run() {
            final InputStream source = this.source;
            final String processName = ManagedProcess.this.processName;
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(source)));
                final OutputStreamWriter writer = new OutputStreamWriter(target);
                String s;
                while ((s = reader.readLine()) != null) {
                    synchronized (target) {
                        writer.write('[');
                        writer.write(processName);
                        writer.write("] ");
                        writer.write(s);
                        writer.write('\n');
                        writer.flush();
                    }
                }
                source.close();
            } catch (IOException e) {
                log.error("Stream processing failed for process '%s': %s", processName, e);
            } finally {
                StreamUtils.safeClose(source);
            }
        }
    }
}
