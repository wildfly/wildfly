/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.process.protocol.ProtocolClient;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility class providing async helper methods on top of the {@code ProcessControllerClient}.
 *
 * @author Emanuel Muckenhuber
 */
public final class AsyncProcessControllerClient implements Closeable {

    private final Listeners listeners;
    private final ProcessControllerClient client;

    private AsyncProcessControllerClient(final ProcessControllerClient client, final Listeners listeners) {
        this.listeners = listeners;
        this.client = client;
    }

    public static AsyncProcessControllerClient connect(final ProtocolClient.Configuration configuration, final byte[] authCode) throws IOException {
        final Listeners listeners = new Listeners();
        final ProcessControllerClient client = ProcessControllerClient.connect(configuration, authCode, new AbstractProcessMessageHandler() {

            @Override
            public void handleProcessAdded(final ProcessControllerClient client, final String processName) {
                listeners.handle(new ProcessEvent(ProcessEvent.Type.ADDED, processName));
            }

            @Override
            public void handleProcessStarted(final ProcessControllerClient client, final String processName) {
                listeners.handle(new ProcessEvent(ProcessEvent.Type.STARTED, processName));
            }

            @Override
            public void handleProcessStopped(final ProcessControllerClient client, final String processName, final long uptimeMillis) {
                listeners.handle(new ProcessEvent(ProcessEvent.Type.STOPPED, processName));
            }

            @Override
            public void handleProcessRemoved(final ProcessControllerClient client, final String processName) {
                listeners.handle(new ProcessEvent(ProcessEvent.Type.REMOVED, processName));
            }

            @Override
            public void handleProcessInventory(final ProcessControllerClient client, final Map<String, ProcessInfo> inventory) {
                listeners.handle(new ProcessEvent(ProcessEvent.Type.PROCESS_INVENTORY, inventory));
            }
        });
        return new AsyncProcessControllerClient(client, listeners);
    }

    /**
     * Get the underlying process controller client.
     *
     * @return the process controller client
     */
    public ProcessControllerClient getClient() {
        return client;
    }

    /**
     * Register an event listener.
     *
     * @param listener
     */
    public void registerEventListener(final ProcessEventListener listener) {
        if(listener == null) {
            throw new IllegalArgumentException();
        }
        listeners.add(listener);
    }

    /**
     * Unregister an event listener.
     *
     * @param listener
     */
    public void unregisterEventListener(final ProcessEventListener listener) {
        if(listener == null) {
            throw new IllegalArgumentException();
        }
        listeners.remove(listener);
    }

    /**
     * Add a new process.
     *
     * @param processName the process name
     * @param authKey the auth key
     * @param cmd the command
     * @param workingDir the working dir
     * @param env the environment
     * @return the future
     * @throws IOException
     */
    public Future<Void> addProcess(final String processName, final byte[] authKey, final String[] cmd, final String workingDir, final Map<String, String> env) throws IOException {
        final ProcessAddTask task = new ProcessAddTask(listeners, processName);
        task.authKey = authKey;
        task.command = cmd;
        task.workDir = workingDir;
        task.env = env;
        return listeners.exec(task, client);
    }

    /**
     * Start a managed process.
     *
     * @param processName the process name to start
     * @return the future
     * @throws IOException
     */
    public Future<Void> startProcess(final String processName) throws IOException {
        return listeners.exec(new ProcessStartTask(listeners, processName), client);
    }

    /**
     * Stop a managed process.
     *
     * @param processName the process name to stop
     * @return the future
     * @throws IOException
     */
    public Future<Void> stopProcess(final String processName) throws IOException {
        return listeners.exec(new ProcessStopTask(listeners, processName), client);
    }

    /**
     * Remove a managed process.
     *
     * @param processName the process name
     * @return the future
     * @throws IOException
     */
    public Future<Void> removeProcess(final String processName) throws IOException {
        return listeners.exec(new ProcessRemoveTask(listeners, processName), client);
    }

    /**
     * Request the current process inventory.
     *
     * @return the process inventory
     * @throws IOException
     */
    public Future<Map<String, ProcessInfo>> requestProcessInventory() throws IOException {
        return listeners.exec(new ProcessInventoryTask(listeners), client);
    }

    /**
     * Send to the process Std.in
     *
     * @param processName the process name
     * @return the output stream
     * @throws IOException
     */
    public OutputStream sendStdin(final String processName) throws IOException {
        return client.sendStdin(processName);
    }

    /**
     * Ask a managed process to reconnect to the server.
     *
     * @param processName the process name
     * @param hostName the host name
     * @param port the port
     * @throws IOException
     */
    public void reconnectProcess(final String processName, final String hostName, final int port) throws IOException {
        client.reconnectProcess(processName, hostName, port);
    }


    @Override
    public void close() throws IOException {
        client.close();
    }

    public static interface ProcessEventListener {

        /**
         * on event...
         *
         * @param event the event
         */
        void onEvent(final ProcessEvent event);

    }

    public static class ProcessEvent {

        public enum Type {
            ADDED,
            STARTED,
            STOPPED,
            REMOVED,
            PROCESS_INVENTORY,
            ;
        }

        final Type eventType;
        final Object attachment;

        protected ProcessEvent(Type type, Object attachment) {
            this.eventType = type;
            this.attachment = attachment;
        }

        public Type getEventType() {
            return eventType;
        }

        public boolean isLifecycleEvent() {
            return eventType != Type.PROCESS_INVENTORY;
        }

        public Object getAttachment() {
            return attachment;
        }

        public <T> T getAttachment(Class<T> expected) {
            return expected.cast(attachment);
        }

        public String getAttachmentAsString() {
            return getAttachment(String.class);
        }
    }

    private static class Listeners {

        final List<ProcessEventListener> tasks = new CopyOnWriteArrayList<ProcessEventListener> ();

        void add(ProcessEventListener task) {
            tasks.add(task);
        }

        void remove(ProcessEventListener task) {
            tasks.remove(task);
        }

        void handle(ProcessEvent event) {
            for(final ProcessEventListener task : tasks) {
                try {
                    task.onEvent(event);
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        <T> Future<T> exec(final Task<T> task, final ProcessControllerClient client) throws IOException {
            add(task);
            try {
                task.sendRequest(client);
            } catch(IOException e) {
                remove(task);
                throw e;
            }
            return task;
        }

    }

    abstract static class Task<V> implements Future<V>, ProcessEventListener {

        private V result;
        private Throwable failure;
        private volatile boolean done;
        private final Lock lock = new ReentrantLock();
        private final Condition hasCompleted = lock.newCondition();
        private final Listeners listeners;

        protected Task(Listeners listeners) {
            this.listeners = listeners;
        }

        abstract void handleEvent(final ProcessEvent event);
        abstract void sendRequest(ProcessControllerClient client) throws IOException;

        public void onEvent(final ProcessEvent event) {
            try {
                handleEvent(event);
            } catch(Throwable t) {
                failed(t);
            }
        }

        public V get() throws InterruptedException, ExecutionException {
            lock.lock();
            try {
                while (!done) {
                    hasCompleted.await();
                }
                if(failure != null) {
                    throw new ExecutionException(failure);
                }
                return result;
            } finally {
                lock.unlock();
            }
        }

        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            long deadline = unit.toMillis(timeout) + System.currentTimeMillis();
            lock.lock();
            try {
                while (!done) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        throw new TimeoutException();
                    }
                    hasCompleted.await(remaining, TimeUnit.MILLISECONDS);
                }
                if(failure != null) {
                    throw new ExecutionException(failure);
                }
                return result;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        void completed(V result) {
           lock.lock();
            try {
                this.result = result;
                done = true;
                listeners.remove(this);
                hasCompleted.signalAll();
            } finally {
                lock.unlock();
            }
        }

        void failed(final Throwable t) {
           lock.lock();
            try {
                failure = t;
                done = true;
                listeners.remove(this);
                hasCompleted.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    abstract static class ProcessTask extends Task<Void> {

        final ProcessEvent.Type type;
        final String processName;

        protected ProcessTask(final Listeners listeners, final ProcessEvent.Type type, final String processName) {
            super(listeners);
            this.processName = processName;
            this.type = type;
        }

        @Override
        public void handleEvent(final ProcessEvent event) {
            if(type == event.getEventType() && processName.equals(event.getAttachment())) {
                completed(null);
            }
        }
    }

    static class ProcessInventoryTask extends Task<Map<String, ProcessInfo>> {
        ProcessInventoryTask(Listeners listeners) {
            super(listeners);
        }

        void sendRequest(ProcessControllerClient client) throws IOException {
            client.requestProcessInventory();
        }

        @Override
        public void handleEvent(ProcessEvent event) {
            if(event.getEventType() == ProcessEvent.Type.PROCESS_INVENTORY) {
                completed(event.getAttachment(Map.class));
            }
        }
    }

    static class ProcessStopTask extends ProcessTask {
        ProcessStopTask(final Listeners listeners, String processName) {
            super(listeners, ProcessEvent.Type.STOPPED, processName);
        }

        @Override
        void sendRequest(ProcessControllerClient client) throws IOException {
            client.stopProcess(this.processName);
        }

    }

    static class ProcessRemoveTask extends ProcessTask {
        ProcessRemoveTask(final Listeners listeners, String processName) {
            super(listeners, ProcessEvent.Type.REMOVED, processName);
        }

        @Override
        void sendRequest(ProcessControllerClient client) throws IOException {
            client.removeProcess(processName);
        }
    }

    static class ProcessAddTask extends ProcessTask {

        byte[] authKey;
        String[] command;
        Map<String, String> env;
        String workDir;

        ProcessAddTask(final Listeners listeners, String processName) {
            super(listeners, ProcessEvent.Type.ADDED, processName);
        }

        @Override
        void sendRequest(ProcessControllerClient client) throws IOException {
            client.addProcess(processName, authKey, command, workDir, env);
        }
    }

    static class ProcessStartTask extends ProcessTask {
        ProcessStartTask(final Listeners listeners, String processName) {
            super(listeners, ProcessEvent.Type.STARTED, processName);
        }

        @Override
        void sendRequest(ProcessControllerClient client) throws IOException {
            client.startProcess(processName);
        }
    }

}
