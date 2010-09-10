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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.communication.InitialSocketRequestException;
import org.jboss.as.communication.SocketConnection;
import org.jboss.as.communication.SocketListener;
import org.jboss.as.communication.SocketListener.SocketHandler;
import org.jboss.as.process.Status;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.SystemExiter;
import org.jboss.as.process.ProcessOutputStreamHandler.Master;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MockProcessManager implements Master{

    static {
        SystemExiter.initialize(new NoopExiter());
    }

    private volatile SocketListener listener;
    Logger log = Logger.getLogger(MockProcessManager.class);

    volatile MockManagedProcess serverManager = new MockManagedProcess(this, "ServerManager", null);
    final Map<String, MockManagedProcess> serverProcesses = new ConcurrentHashMap<String, MockManagedProcess>();

    final int expectedProcessCount;

    final AtomicInteger addCount = new AtomicInteger();
    final AtomicInteger startCount = new AtomicInteger();
    final AtomicInteger stopCount = new AtomicInteger();
    final AtomicInteger removeCount = new AtomicInteger();
    volatile CountDownLatch serveManagerLatch = new CountDownLatch(1);
    volatile CountDownLatch addLatch;
    volatile CountDownLatch startLatch;
    volatile CountDownLatch stopLatch;
    volatile CountDownLatch removeLatch;
    volatile BlockingQueue<String> reconnectServers = new LinkedBlockingQueue<String>();

    volatile NewConnectionListener newConnectionListener;

    private final CountDownLatch shutdownServersLatch = new CountDownLatch(1);


    private MockProcessManager(int expectedProcessCount) {
        this.expectedProcessCount = expectedProcessCount;
        addLatch = new CountDownLatch(expectedProcessCount);
        startLatch = new CountDownLatch(expectedProcessCount);
        stopLatch = new CountDownLatch(expectedProcessCount);
        removeLatch = new CountDownLatch(expectedProcessCount);
    }

    public static MockProcessManager create(int expectedProcessCount) throws IOException {
        MockProcessManager pm = new MockProcessManager(expectedProcessCount);
        pm.start();
        return pm;
    }

    private void start() throws IOException {
        listener = SocketListener.createSocketListener("PM", new ProcessManagerSocketHandler(), InetAddress.getLocalHost(), 0, 10);
        listener.start();
    }

    public MockManagedProcess getServerManager() {
        return serverManager;
    }

    public MockManagedProcess getProcess(String processName) {
        MockManagedProcess proc = serverProcesses.get(processName);
        if (proc == null && processName.equals("ServerManager"))
            proc = getServerManager();
        return proc;
    }

    public void shutdown() {
        listener.shutdown();
    }

    public InetAddress getAddress() {
        return listener.getAddress();
    }

    public Integer getPort() {
        return listener.getPort();
    }

    private void waitForLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new RuntimeException("Latch timed out");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public MockManagedProcess waitForServerManager() {
        waitForLatch(serveManagerLatch);
        return serverManager;
    }


    public void waitForAddedProcesses() {
        waitForLatch(addLatch);
    }

    public void waitForStartedProcesses() {
        waitForLatch(startLatch);
    }

    public void waitForRemovedProcesses() {
        waitForLatch(removeLatch);
    }

    public void waitForStoppedProcesses() {
        waitForLatch(stopLatch);
    }

    public String waitForReconnectServers() throws InterruptedException {
        String info = reconnectServers.poll(10, TimeUnit.SECONDS);
        if (info == null)
            throw new RuntimeException("Read timed out");
        return info;
    }

    public void resetAddLatch(int count) {
        addLatch = new CountDownLatch(count);
    }

    public void resetStartLatch(int count) {
        startLatch = new CountDownLatch(count);
    }

    public void resetStopLatch(int count) {
        stopLatch = new CountDownLatch(count);
    }

    public void resetRemoveLatch(int count) {
        removeLatch = new CountDownLatch(count);
    }

    public void resetReconnectServers() {
        reconnectServers.clear();
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

    public void setNewConnectionListener(NewConnectionListener newConnectionListener) {
        this.newConnectionListener = newConnectionListener;
    }

    @Override
    public synchronized void addProcess(String processName, List<String> command, Map<String, String> env, String workingDirectory) {
        log.info("Adding process " + processName);
        serverProcesses.put(processName, new MockManagedProcess(this, processName, command));
        addCount.incrementAndGet();
        addLatch.countDown();
    }

    @Override
    public void startProcess(String processName) {
        log.info("Starting process " + processName);
        serverProcesses.get(processName).start();
        startCount.incrementAndGet();
        startLatch.countDown();
    }

    @Override
    public void stopProcess(String processName) {
        try {
            log.info("Stopping process " + processName);
            MockManagedProcess process = processName.equals("ServerManager") ? serverManager : serverProcesses.get(processName);
            if (process == null) {
                log.error("No process called " + processName);
                return;
            }
            stopCount.incrementAndGet();
            process.stop();

            stopLatch.countDown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeProcess(String processName) {
        log.info("Removing process " + processName);
        serverProcesses.remove(processName);
        removeCount.incrementAndGet();
        removeLatch.countDown();
    }

    @Override
    public void broadcastMessage(String sender, List<String> msg) {
    }

    @Override
    public void broadcastMessage(String sender, byte[] msg) {
    }


    @Override
    public void sendMessage(String sender, String recipient, List<String> msg) {
    }

    @Override
    public void sendMessage(String sender, String recipient, byte[] msg) {
    }

    @Override
    public void serversShutdown() {
        shutdownServersLatch.countDown();
    }

    public void waitForServerShutdown() {
        waitForLatch(shutdownServersLatch);
    }

    @Override
    public void downServer(String serverName) {
    }

    @Override
    public void reconnectServersToServerManager(String smAddress, String smPort) {
        reconnectServers.add(smAddress + ":" + smPort);
    }

    class ProcessManagerSocketHandler implements SocketHandler {

        @Override
        public void initializeConnection(Socket socket) throws IOException, InitialSocketRequestException {
            InputStream in = socket.getInputStream();
            StringBuilder sb = new StringBuilder();

            //TODO Timeout on the read?
            Status status = StreamUtils.readWord(in, sb);
            if (status != Status.MORE) {
                throw new InitialSocketRequestException("Process acceptor: received '" + sb.toString() + "' but no more");
            }
            if (!sb.toString().equals("CONNECTED")) {
                throw new InitialSocketRequestException("Process acceptor: received unknown start command '" + sb.toString() + "'");
            }
            sb = new StringBuilder();
            while (status == Status.MORE) {
                status = StreamUtils.readWord(in, sb);
            }
            String processName = sb.toString();
            SocketConnection conn = SocketConnection.accepted(socket);
            if (processName.equals("ServerManager")) {
                serverManager.setPmConnection(conn);
                serveManagerLatch.countDown();
            }
            else {
                MockManagedProcess process = serverProcesses.get(processName);
                if (process != null) {
                    process.setPmConnection(conn);
                }
            }
            if (newConnectionListener != null)
                newConnectionListener.acceptedConnection(processName, conn);
            log.info("PM got connection from " + processName);

        }
    }

    public interface NewConnectionListener{
        void acceptedConnection(String processName, SocketConnection conn);
    }

}
