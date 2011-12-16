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

import static java.lang.Thread.holdsLock;
import static org.jboss.as.process.ProcessMessages.MESSAGES;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.process.protocol.StreamUtils;
import org.jboss.logging.Logger;

/**
 * A managed process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
final class ManagedProcess {

    private final String processName;
    private final List<String> command;
    private final Map<String, String> env;
    private final String workingDirectory;
    private final ProcessLogger log;
    private final Object lock;

    private final ProcessController processController;
    private final byte[] authKey;
    private final boolean isPrivileged;
    private final RespawnPolicy respawnPolicy;

    private OutputStream stdin;
    private volatile State state = State.DOWN;
    private Process process;
    private boolean shutdown;
    private boolean stopRequested = false;
    private final AtomicInteger respawnCount = new AtomicInteger(0);

    public byte[] getAuthKey() {
        return authKey;
    }

    public boolean isPrivileged() {
        return isPrivileged;
    }

    public boolean isRunning() {
        return state == State.STARTED;
    }

    enum State {
        DOWN,
        STARTED,
        STOPPING,
        ;
    }

    ManagedProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory, final Object lock, final ProcessController controller, final byte[] authKey, final boolean privileged, final boolean respawn) {
        if (processName == null) {
            throw MESSAGES.nullVar("processName");
        }
        if (command == null) {
            throw MESSAGES.nullVar("command");
        }
        if (env == null) {
            throw MESSAGES.nullVar("env");
        }
        if (workingDirectory == null) {
            throw MESSAGES.nullVar("workingDirectory");
        }
        if (lock == null) {
            throw MESSAGES.nullVar("lock");
        }
        if (controller == null) {
            throw MESSAGES.nullVar("controller");
        }
        if (authKey == null) {
            throw MESSAGES.nullVar("authKey");
        }
        if (authKey.length != 16) {
            throw MESSAGES.invalidLength("authKey");
        }
        this.processName = processName;
        this.command = command;
        this.env = env;
        this.workingDirectory = workingDirectory;
        this.lock = lock;
        processController = controller;
        this.authKey = authKey;
        isPrivileged = privileged;
        respawnPolicy = respawn ? RespawnPolicy.RESPAWN : RespawnPolicy.NONE;
        log = Logger.getMessageLogger(ProcessLogger.class, "org.jboss.as.process." + processName + ".status");
    }

    int incrementAndGetRespawnCount() {
        return respawnCount.incrementAndGet();
    }

    int resetRespawnCount() {
        return respawnCount.getAndSet(0);
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
            resetRespawnCount();
            doStart(false);
        }
    }

    public void sendStdin(final InputStream msg) {
        assert holdsLock(lock); // Call under lock
        try {
            StreamUtils.copyStream(msg, stdin);
            stdin.flush();
        } catch (IOException e) {
            log.failedToSendDataBytes(e, processName);
        }
    }

    public void reconnect(String hostName, int port, boolean managementSubsystemEndpoint, byte[] asAuthKey) {
        assert holdsLock(lock); // Call under lock
        try {
            StreamUtils.writeUTFZBytes(stdin, hostName);
            StreamUtils.writeInt(stdin, port);
            StreamUtils.writeBoolean(stdin, managementSubsystemEndpoint);
            stdin.write(asAuthKey);
            stdin.flush();
        } catch (IOException e) {
            log.failedToSendReconnect(e, processName);
        }
    }

    void doStart(boolean restart) {
        // Call under lock
        assert holdsLock(lock);
        stopRequested = false;
        final List<String> command = new ArrayList<String>(this.command);
        if(restart) {
            //Add the restart flag to the HC process if we are respawning it
            command.add(CommandLineConstants.PROCESS_RESTARTED);
        }
        log.startingProcess(processName);
        log.debugf("Process name='%s' command='%s' workingDirectory='%s'", processName, command, workingDirectory);
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(env);
        builder.directory(new File(workingDirectory));
        final Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            log.failedToStartProcess(processName);
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
            log.failedToSendAuthKey(processName, e);
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
            log.stoppingProcess(processName);
            stopRequested = true;
            StreamUtils.safeClose(stdin);
            state = State.STOPPING;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            if(shutdown) {
                return;
            }
            shutdown = true;
            if (state == State.STARTED) {
                log.stoppingProcess(processName);
                stopRequested = true;
                StreamUtils.safeClose(stdin);
                state = State.STOPPING;
            } else {
                processController.removeProcess(processName);
            }
        }
    }

    void respawn() {
        synchronized (lock) {
            if (state != State.DOWN) {
                log.debugf("Attempted to respawn already-running process '%s'", processName);
                return;
            }
            doStart(true);
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
                log.processFinished(processName, Integer.valueOf(exitCode));
                break;
            } catch (InterruptedException e) {
                // ignore
            }
            boolean respawn = false;
            int respawnCount = 0;
            synchronized (lock) {

                final long endTime = System.currentTimeMillis();
                processController.processStopped(processName, endTime - startTime);
                state = State.DOWN;

                if (shutdown) {
                    processController.removeProcess(processName);
                } else if (isPrivileged() && exitCode == ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE) {
                    // Host Controller abort
                    processController.removeProcess(processName);
                    new Thread(new Runnable() {
                        public void run() {
                            processController.shutdown();
                            System.exit(0);
                        }
                    }).start();
                } else if (isPrivileged() && exitCode == ExitCodes.RESTART_PROCESS_FROM_STARTUP_SCRIPT) {
                    // Host Controller restart via exit code picked up by script
                    processController.removeProcess(processName);
                    new Thread(new Runnable() {
                        public void run() {
                            processController.shutdown();
                            System.exit(ExitCodes.RESTART_PROCESS_FROM_STARTUP_SCRIPT);
                        }
                    }).start();

                } else {
                    if(! stopRequested) {
                        respawn = true;
                        respawnCount = ManagedProcess.this.incrementAndGetRespawnCount();
                    }
                }
                stopRequested = false;
            }
            if(respawn) {
                respawnPolicy.respawn(respawnCount, ManagedProcess.this);
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
                log.streamProcessingFailed(processName, e);
            } finally {
                StreamUtils.safeClose(source);
            }
        }
    }
}
