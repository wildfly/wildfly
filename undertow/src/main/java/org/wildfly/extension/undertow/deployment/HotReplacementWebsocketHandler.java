/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.deployment;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.zip.InflaterInputStream;

import org.wildfly.extension.classchange.DeploymentClassChangeSupport;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.Buffers;
import org.xnio.IoUtils;
import org.xnio.Pooled;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class HotReplacementWebsocketHandler implements WebSocketConnectionCallback {

    private static final long MAX_WAIT_TIME = Long.getLong("org.wildfly.undertow.remote-hot-deploy-wait-time", 15000);

    private static final int CLASS_CHANGE_RESPONSE = 2;
    private static final int CLASS_CHANGE_REQUEST = 1;
    private final DeploymentClassChangeSupport deploymentClassChangeSupport;

    private final Object lock = new Object();
    /**
     * The current connection, managed under lock
     * <p>
     * There will only ever be one connection at a time
     */
    private ConnectionContext connection;

    public HotReplacementWebsocketHandler(DeploymentClassChangeSupport deploymentClassChangeSupport) {
        this.deploymentClassChangeSupport = deploymentClassChangeSupport;
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        synchronized (lock) {
            if (connection != null) {
                //only one open connection at a time
                IoUtils.safeClose(connection.connection);
                //add an empty message to unblock a waiting request
                connection.messages.add(new Message());
            }
            final ConnectionContext currentConnection = new ConnectionContext(channel);
            this.connection = currentConnection;
            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                    handleResponseMessage(message, currentConnection);
                }

                @Override
                protected void onError(WebSocketChannel channel, Throwable error) {
                    UndertowLogger.ROOT_LOGGER.failedToProcessRemoteHotDeployment(error);
                    currentConnection.messages.add(new Message()); //unblock a waiting thread
                    IoUtils.safeClose(channel);
                }

                @Override
                protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) {
                    synchronized (lock) {
                        if (HotReplacementWebsocketHandler.this.connection == currentConnection) {
                            HotReplacementWebsocketHandler.this.connection = null;
                        }
                    }
                    currentConnection.messages.add(new Message()); //unblock a waiting thread
                }
            });
            channel.resumeReceives();
        }
    }

    public void checkForChanges() {
        final ConnectionContext con;
        synchronized (lock) {
            con = connection;
        }
        if (con == null) {
            //we return if there is no connection
            return;
        }
        WebSockets.sendBinary(ByteBuffer.wrap(new byte[]{CLASS_CHANGE_REQUEST}), con.connection, new WebSocketCallback<Void>() {
            @Override
            public void complete(WebSocketChannel channel, Void context) {

            }

            @Override
            public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
                UndertowLogger.ROOT_LOGGER.failedToProcessRemoteHotDeployment(throwable);
                //add an empty message so the request can continue
                con.messages.add(new Message());
            }
        });
        try {
            Message m = con.messages.poll(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
            if (m == null) {
                UndertowLogger.ROOT_LOGGER.timedOutProcessingRemoteHotDeployment();
            } else {
                if (!m.srcFiles.isEmpty() ||
                        !m.classFiles.isEmpty() ||
                        !m.webResources.isEmpty())
                    deploymentClassChangeSupport.notifyChangedClasses(m.srcFiles, m.classFiles, m.webResources);
            }
        } catch (InterruptedException e) {
            UndertowLogger.ROOT_LOGGER.failedToProcessRemoteHotDeployment(e);
        }
    }

    protected void handleResponseMessage(BufferedBinaryMessage message, ConnectionContext currentConnection) throws IOException {
        try (Pooled<ByteBuffer[]> pooled = message.getData()) {
            ByteBuffer[] buffers = pooled.getResource();
            byte first = buffers[0].get();
            if (first == CLASS_CHANGE_RESPONSE) {
                Message m = new Message();
                //a response message
                int rem = (int) Buffers.remaining(buffers);
                byte[] data = new byte[rem];
                Buffers.copy(ByteBuffer.wrap(data), buffers, 0, buffers.length);
                try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
                    Map<String, byte[]> srcFiles = m.srcFiles;
                    Map<String, byte[]> classFiles = m.classFiles;
                    Map<String, byte[]> webResources = m.webResources;
                    String key;
                    byte[] rd;
                    int count = in.readInt();
                    for (int i = 0; i < count; ++i) {
                        key = in.readUTF();
                        int byteLength = in.readInt();
                        rd = new byte[byteLength];
                        in.readFully(rd);
                        srcFiles.put(key, rd);
                    }
                    count = in.readInt();
                    for (int i = 0; i < count; ++i) {
                        key = in.readUTF();
                        int byteLength = in.readInt();
                        rd = new byte[byteLength];
                        in.readFully(rd);
                        classFiles.put(key, rd);
                    }
                    count = in.readInt();
                    for (int i = 0; i < count; ++i) {
                        key = in.readUTF();
                        int byteLength = in.readInt();
                        rd = new byte[byteLength];
                        in.readFully(rd);
                        webResources.put(key, rd);
                    }
                    currentConnection.messages.add(m);
                }
            }

        }
    }

    static final class Message {

        Map<String, byte[]> srcFiles = new HashMap<>();
        Map<String, byte[]> classFiles = new HashMap<>();
        Map<String, byte[]> webResources = new HashMap<>();
    }

    private static final class ConnectionContext {

        final WebSocketChannel connection;
        final BlockingDeque<Message> messages = new LinkedBlockingDeque<>();

        private ConnectionContext(WebSocketChannel connection) {
            this.connection = connection;
        }
    }
}
