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
package org.jboss.test.as.protocol.support.process;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ProcessManagerMaster;
import org.jboss.as.process.RespawnPolicy;

/**
 * Overrides {@link ProcessManagerMaster} and provides hooks for seeing when
 * commands go through the PM
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestProcessManager extends ProcessManagerMaster {

    public static final String SERVER_NAME_AND_CONNECTION_SEPARATOR = "%";

    private final AtomicInteger addCount = new AtomicInteger();
    private final AtomicInteger startCount = new AtomicInteger();
    private final AtomicInteger stopCount = new AtomicInteger();
    private final AtomicInteger removeCount = new AtomicInteger();
    private final BlockingQueue<String> addedQueue = new LinkedBlockingQueue<String>();
    private final BlockingQueue<String> startedQueue = new LinkedBlockingQueue<String>();
    private final BlockingQueue<String> stoppedQueue = new LinkedBlockingQueue<String>();
    private final BlockingQueue<String> removedQueue = new LinkedBlockingQueue<String>();
    private final CountDownLatch shutdownServersLatch = new CountDownLatch(1);
    private final BlockingQueue<String> reconnectServers = new LinkedBlockingQueue<String>();
    private volatile NewConnectionListener newConnectionListener;


    private TestProcessManager(TestProcessHandlerFactory processHandlerFactory, InetAddress addr, int port) throws IOException {
        super(processHandlerFactory, addr, port);
    }

    public static TestProcessManager create(TestProcessHandlerFactory processHandlerFactory, InetAddress addr, int port) throws Exception {
        return create(processHandlerFactory, addr, port, null);
    }

    public static TestProcessManager create(TestProcessHandlerFactory processHandlerFactory, InetAddress addr, int port, boolean startServerManager) throws Exception {
        return create(processHandlerFactory, addr, port, null, startServerManager);
    }

    public static TestProcessManager create(TestProcessHandlerFactory processHandlerFactory, InetAddress addr, int port, NewConnectionListener newConnectionListener) throws Exception {
        return create(processHandlerFactory, addr, port, newConnectionListener, true);
    }
    public static TestProcessManager create(TestProcessHandlerFactory processHandlerFactory, InetAddress addr, int port, NewConnectionListener newConnectionListener, boolean startServerManager) throws Exception {
        TestProcessManager pm = new TestProcessManager(processHandlerFactory, addr, port);
        if (newConnectionListener != null)
            pm.setNewConnectionListener(newConnectionListener);
        pm.start();
        pm.addAndStartServerManager(startServerManager);
        return pm;
    }

    private void addAndStartServerManager(boolean startServerManager) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(CommandLineConstants.INTERPROCESS_PM_ADDRESS);
        command.add(getInetAddress().getHostAddress());
        command.add(CommandLineConstants.INTERPROCESS_PM_PORT);
        command.add(getPort().toString());
        command.add(CommandLineConstants.INTERPROCESS_NAME);
        command.add("ServerManager");
        command.add(CommandLineConstants.INTERPROCESS_SM_ADDRESS);
        command.add(InetAddress.getLocalHost().getHostAddress());
        command.add(CommandLineConstants.INTERPROCESS_SM_PORT);
        command.add("0");

        addProcess(ProcessManagerMaster.SERVER_MANAGER_PROCESS_NAME, command, System.getenv(), ".", RespawnPolicy.DefaultRespawnPolicy.INSTANCE);
        if (startServerManager)
            startProcess(ProcessManagerMaster.SERVER_MANAGER_PROCESS_NAME);
    }

    public void setNewConnectionListener(NewConnectionListener newConnectionListener) {
        this.newConnectionListener = newConnectionListener;
    }

    @Override
    public void addProcess(String processName, List<String> command, Map<String, String> env, String workingDirectory,
            RespawnPolicy respawnPolicy) {
        super.addProcess(processName, command, env, workingDirectory, respawnPolicy);
        addCount.incrementAndGet();
        addedQueue.add(processName);
    }

    @Override
    public void startProcess(String processName) {
        super.startProcess(processName);
        startCount.incrementAndGet();
        startedQueue.add(processName);
    }

    @Override
    public void removeProcess(String processName) {
        System.err.println("---------> removing " + processName + " " + this);
        removeCount.incrementAndGet();
        removedQueue.add(processName);
        super.removeProcess(processName);
    }

    @Override
    public void stopProcess(String processName) {
        System.err.println("---------> stopping " + processName + " " + this);
        stopCount.incrementAndGet();
        stoppedQueue.add(processName);
        super.stopProcess(processName);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public void serversShutdown() {
        super.serversShutdown();
        shutdownServersLatch.countDown();
    }

    @Override
    public void reconnectServersToServerManager(String smAddress, String smPort) {
        super.reconnectServersToServerManager(smAddress, smPort);
        reconnectServers.add(smAddress + ":" + smPort);
    }

    @Override
    public void reconnectProcessToServerManager(String server, String smAddress, String smPort) {
        super.reconnectProcessToServerManager(server, smAddress, smPort);
        reconnectServers.add(server + SERVER_NAME_AND_CONNECTION_SEPARATOR + smAddress + ":" + smPort);
    }

    @Override
    protected void acceptedConnection(String processName, SocketConnection connection) {
        if (newConnectionListener != null)
            newConnectionListener.acceptedConnection(processName, connection);
    }

    public void waitForServersShutdown() throws InterruptedException {
        if (!shutdownServersLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Wait timed out");
        }
    }

    public List<String> pollAddedProcess(int expectedNumber) throws InterruptedException {
        return pollQueue(addedQueue, expectedNumber);
    }

    public List<String> pollStartedProcess(int expectedNumber) throws InterruptedException {
        return pollQueue(startedQueue, expectedNumber);
    }

    public List<String> pollStoppedProcess(int expectedNumber) throws InterruptedException {
        return pollQueue(stoppedQueue, expectedNumber);
    }

    public List<String> pollRemovedProcess(int expectedNumber) throws InterruptedException {
        return pollQueue(removedQueue, expectedNumber);
    }

    public String waitForReconnectServers() throws InterruptedException {
        String info = reconnectServers.poll(10, TimeUnit.SECONDS);
        if (info == null)
            throw new RuntimeException("Read timed out");
        return info;
    }


    public int getAddCount() {
        return addCount.get();
    }

    public int getStartCount() {
        return startCount.get();
    }

    public int getStopCount() {
        return stopCount.get();
    }

    public int getRemoveCount() {
        return removeCount.get();
    }

    private <T> List<T> pollQueue(BlockingQueue<T> queue, int expectedNumber) throws InterruptedException{
        List<T> result = new ArrayList<T>();
        for (int i = 0 ; i < expectedNumber ; i++) {
            T t = queue.poll(10, TimeUnit.SECONDS);
            Assert.assertNotNull("Wait timed out reading from queue: " + result, t);
            result.add(t);
        }
        return result;
    }

    private <T> T pollQueue(BlockingQueue<T> queue) throws InterruptedException {
        T t = queue.poll(10, TimeUnit.SECONDS);
        Assert.assertNotNull("Wait timed out reading from queue", t);
        return t;
    }

    public interface NewConnectionListener{
        void acceptedConnection(String processName, SocketConnection conn);
    }

}
