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
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;

import java.util.logging.Handler;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManager {

    private static final Logger log = Logger.getLogger("org.jboss.as.process-manager");

    /**
     * Main lock - anything which opens a file descriptor or spawns a process must
     * hold this lock for the duration of the operation.
     */
    private final Object lock = new Object();

    private final Map<String, ManagedProcess> processes = new HashMap<String, ManagedProcess>();
    private final Map<Key, ManagedProcess> processesByKey = new HashMap<Key, ManagedProcess>();
    private final Handler consoleHandler;
    private final ProtocolServer server;

    private final Random rng;

    private final Set<Connection> managerConnections = new HashSet<Connection>();

    private boolean shutdown;

    private final PrintStream stdout;
    private final PrintStream stderr;

    public ProcessManager(final Handler consoleHandler, final ProtocolServer.Configuration configuration, final PrintStream stdout, final PrintStream stderr) throws IOException {
        this.consoleHandler = consoleHandler;
        this.stdout = stdout;
        this.stderr = stderr;
        rng = new Random(new SecureRandom().nextLong());
        //noinspection ThisEscapedInObjectConstruction
        configuration.setConnectionHandler(new ProcessManagerServerHandler(this));
        final ProtocolServer server = new ProtocolServer(configuration);
        server.start();
        this.server = server;
    }

    public void addManagerConnection(Connection connection) {
        synchronized (lock) {
            managerConnections.add(connection);
        }
    }

    public void removeManagerConnection(Connection connection) {
        synchronized (lock) {
            managerConnections.remove(connection);
        }
    }

    public void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory, final boolean isInitial) {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            final Map<String, ManagedProcess> processes = this.processes;
            if (processes.containsKey(processName)) {
                log.warnf("Attempted to register duplicate-named process '%s'", processName);
                // ignore
                return;
            }
            final byte[] authKey = new byte[16];
            rng.nextBytes(authKey);
            final ManagedProcess process = new ManagedProcess(processName, command, env, workingDirectory, lock, this, authKey, isInitial);
            processes.put(processName, process);
            processesByKey.put(new Key(authKey), process);
            for (Connection connection : managerConnections) {
                try {
                    final OutputStream os = connection.writeMessage();
                    try {
                        os.write(Protocol.PROCESS_ADDED);
                        StreamUtils.writeUTFZBytes(os, processName);
                        os.close();
                    } finally {
                        StreamUtils.safeClose(os);
                    }
                } catch (IOException e) {
                    log.errorf("Failed to write PROCESS_ADDED message to manager connection: %s", e);
                }
            }
        }
    }

    public void startProcess(final String processName) {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            final Map<String, ManagedProcess> processes = this.processes;
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                log.warnf("Attempted to start non-existent process '%s'", processName);
                // ignore
                return;
            }
            process.start();
        }
    }

    public void stopProcess(final String processName) {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            final Map<String, ManagedProcess> processes = this.processes;
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                log.warnf("Attempted to stop non-existent process '%s'", processName);
                // ignore
                return;
            }
            process.stop();
        }
    }

    public void removeProcess(final String processName) {
        synchronized (lock) {
            final Map<String, ManagedProcess> processes = this.processes;
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                log.warnf("Attempted to remove non-existent process '%s'", processName);
                // ignore
                return;
            }
            processes.remove(processName);
            processesByKey.remove(new Key(process.getAuthKey()));
            lock.notifyAll();
        }
    }

    public void sendStdin(final String recipient, final InputStream source) {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            final Map<String, ManagedProcess> processes = this.processes;
            final ManagedProcess process = processes.get(recipient);
            if (process == null) {
                // ignore
                return;
            }
            process.sendStdin(source);
        }
    }

    public void shutdown() {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            log.info("Shutting down process manager");
            shutdown = true;
            for (ManagedProcess process : processes.values()) {
                process.shutdown();
            }
            while (! processes.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public Handler getConsoleHandler() {
        return consoleHandler;
    }

    public ManagedProcess getServerByAuthCode(final byte[] code) {
        synchronized (lock) {
            return processesByKey.get(new Key(code));
        }
    }

    void processStarted(final String processName) {
        synchronized (lock) {
            for (Connection connection : managerConnections) {
                try {
                    final OutputStream os = connection.writeMessage();
                    try {
                        os.write(Protocol.PROCESS_STARTED);
                        StreamUtils.writeUTFZBytes(os, processName);
                        os.close();
                    } finally {
                        StreamUtils.safeClose(os);
                    }
                } catch (IOException e) {
                    log.errorf("Failed to write PROCESS_STARTED message to manager connection: %s", e);
                }
            }
        }
    }

    void processStopped(final String processName, final long uptime) {
        synchronized (lock) {
            for (Connection connection : managerConnections) {
                try {
                    final OutputStream os = connection.writeMessage();
                    try {
                        os.write(Protocol.PROCESS_STOPPED);
                        StreamUtils.writeUTFZBytes(os, processName);
                        StreamUtils.writeLong(os, uptime);
                        os.close();
                    } finally {
                        StreamUtils.safeClose(os);
                    }
                } catch (IOException e) {
                    log.errorf("Failed to write PROCESS_STOPPED message to manager connection: %s", e);
                }
            }
        }
    }

    void sendInventory() {
        synchronized (lock) {
            for (Connection connection : managerConnections) {
                try {
                    final OutputStream os = connection.writeMessage();
                    try {
                        os.write(Protocol.PROCESS_INVENTORY);
                        final Collection<ManagedProcess> processCollection = processes.values();
                        StreamUtils.writeInt(os, processCollection.size());
                        for (ManagedProcess process : processCollection) {
                            StreamUtils.writeUTFZBytes(os, process.getProcessName());
                            os.write(process.getAuthKey());
                            StreamUtils.writeBoolean(os, process.isRunning());
                        }
                        os.close();
                    } finally {
                        StreamUtils.safeClose(os);
                    }
                } catch (IOException e) {
                    log.errorf("Failed to write PROCESS_INVENTORY message to manager connection: %s", e);
                }
            }
        }
    }

    public ProtocolServer getServer() {
        return server;
    }

    PrintStream getStdout() {
        return stdout;
    }

    PrintStream getStderr() {
        return stderr;
    }

    private static final class Key {
        private final byte[] authKey;
        private final int hashCode;

        public Key(final byte[] authKey) {
            this.authKey = authKey;
            hashCode = Arrays.hashCode(authKey);
        }

        public boolean equals(Object other) {
            return other instanceof Key && equals((Key)other);
        }

        public boolean equals(Key other) {
            return this == other || other != null && Arrays.equals(authKey, other.authKey);
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
