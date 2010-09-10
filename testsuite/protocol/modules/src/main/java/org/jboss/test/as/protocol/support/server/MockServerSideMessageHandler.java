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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.server.ServerCommunicationHandler.Handler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MockServerSideMessageHandler implements Handler{

    final AtomicInteger puts = new AtomicInteger();
    final AtomicInteger gets = new AtomicInteger();
    final BlockingQueue<byte[]> data = new LinkedBlockingQueue<byte[]>();

    @Override
    public void handleMessage(byte[] message) {
        data.add(message);
    }

    @Override
    public void handleMessage(List<String> message) {
    }

    @Override
    public void shutdown() {
        System.out.println("Server shutting down");
    }

    public byte[] awaitAndReadMessage() {
        try {
            byte[] bytes = data.poll(10, TimeUnit.SECONDS);
            if (bytes == null)
                throw new RuntimeException("Read timed out");
            return bytes;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reconnectServer(String addr, String port) {
    }
}
