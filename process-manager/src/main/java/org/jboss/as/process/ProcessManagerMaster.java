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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.process.ManagedProcess.StopProcessListener;

/**
 * Process manager main entry point.  The thin process manager process is implemented here.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerMaster {

    private static final String SERVER_MANAGER_PROCESS_NAME = "ServerManager";
    
    ProcessManagerMaster() {
    }

    private final Map<String, ManagedProcess> processes = new HashMap<String, ManagedProcess>();

    public static void main(String[] args) {
        final ProcessManagerMaster master = new ProcessManagerMaster();
        final String initialWorkingDirectory = args[0];
        final List<String> fullList = Arrays.asList(args);
        final List<String> command = fullList.subList(1, fullList.size());
        // TODO JBAS-8259 -- possible socket-based communication
        master.addProcess(SERVER_MANAGER_PROCESS_NAME, command, System.getenv(), initialWorkingDirectory);
        master.startProcess(SERVER_MANAGER_PROCESS_NAME);
    }

    void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory) {
        addProcess(processName, command, env, workingDirectory, RespawnPolicy.DefaultRespawnPolicy.INSTANCE);
    }

    void addProcess(final String processName, final List<String> command, final Map<String, String> env, final String workingDirectory, RespawnPolicy respawnPolicy) {
        
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            if (processes.containsKey(processName)) {
                System.err.println("already have process " + processName);
                // ignore
                return;
            }
            final ManagedProcess process = new ManagedProcess(this, processName, command, env, workingDirectory, respawnPolicy);
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
                    System.err.println("Ignoring remove request for running process " + processName);
                    return;
                }
                processes.remove(processName);
            }
        }
    }

    void sendMessage(final String sender, final String recipient, final List<String> msg) {
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
    
    void registerStopProcessListener(String name, StopProcessListener listener) {
        final Map<String, ManagedProcess> processes = this.processes;
        synchronized (processes) {
            ManagedProcess process = processes.get(name);
            if (process == null)
                return;
            process.registerStopProcessListener(listener);
        }
    }
    
    List<String> getProcessNames(boolean onlyStarted) {
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
}
