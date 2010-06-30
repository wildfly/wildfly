/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Process manager main entry point.  The thin process manager process is implemented here.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerMaster {

    private ProcessManagerMaster() {
    }

    private final Map<String, ManagedProcess> processes = new HashMap<String, ManagedProcess>();

    public static void main(String[] args) {
        final ProcessManagerMaster master = new ProcessManagerMaster();
        final String initialProcessName = args[0];
        final String initialWorkingDirectory = args[1];
        final List<String> fullList = Arrays.asList(args);
        final List<String> command = fullList.subList(2, fullList.size());
        master.addProcess(initialProcessName, command, System.getenv(), initialWorkingDirectory);
        master.startProcess(initialProcessName);
    }

    void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) {
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            if (processes.containsKey(processName)) {
                // ignore
                return;
            }
            final ManagedProcess process = new ManagedProcess(this, processName, command, env, workingDirectory);
            processes.put(processName, process);
        }
    }

    void startProcess(final String processName) {
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
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(processName);
            if (process == null) {
                // ignore
                return;
            }
            synchronized (process) {
                if (process.isStart()) {
                    // ignore
                    return;
                }
                processes.remove(processName);
            }
        }
    }

    void sendMessage(final String name, final List<String> msg) {
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            final ManagedProcess process = processes.get(name);
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
                    process.send(msg);
                } catch (IOException e) {
                    // todo log it
                }
            }
        }
    }

    void broadcastMessage(final List<String> msg) {
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            for (ManagedProcess process : processes.values()) {
                synchronized (process) {
                    if (! process.isStart()) {
                        // ignore
                        return;
                    }
                    try {
                        process.send(msg);
                    } catch (IOException e) {
                        // todo log it
                    }
                }
            }
        }
    }
}
