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
package org.jboss.test.as.protocol.support.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.as.model.ServerModel;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommandHandler;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerManagerToServerCommandHandler;

/**
 * A server side Handler implementation that allows us to see the commands
 * received by a server instance
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestServerSideMessageHandler extends ServerManagerToServerCommandHandler implements OutgoingPmCommandHandler{

    final BlockingQueue<byte[]> data = new LinkedBlockingQueue<byte[]>();
    final BlockingQueue<String> reconnectServer = new LinkedBlockingQueue<String>();
    final CountDownLatch shutDownLatch = new CountDownLatch(1);


    @Override
    public void handleCommand(byte[] message) {
        data.add(message);
    }

    @Override
    public void handleShutdown() {
        shutDownLatch.countDown();
    }

    public void waitForShutdownCommand() throws InterruptedException {
        if (!shutDownLatch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Wait for shutdown timed out");
        }
    }

    public byte[] awaitAndReadMessage() {
        return awaitAndReadMessage(10000);
    }

    public byte[] awaitAndReadMessage(int timeoutMs) {
        try {
            byte[] bytes = data.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (bytes == null)
                throw new RuntimeException("Read timed out");
            return bytes;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleReconnectServerManager(String addr, String port) {
        reconnectServer.add(addr + ":" + port);
    }

    public String waitForReconnectServer() throws InterruptedException {
        return waitForReconnectServer(10000);
    }

    public String waitForReconnectServer(long timeoutMs) throws InterruptedException {
        String info = reconnectServer.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (info == null)
            throw new RuntimeException("Read timed out");
        return info;
    }

    @Override
    public void handleStartServer(ServerModel serverModel) {
    }

    @Override
    public void handleStopServer() {
    }

    @Override
    public void handleDown(String serverName) {
    }
}
