/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpOpenListener;
import io.undertow.server.OpenListener;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

/**
 * @author Stuart Douglas
 * @author Tomaz Cerar
 */
public class HttpListenerService extends AbstractListenerService<HttpListenerService> {
    private volatile AcceptingChannel<StreamConnection> server;

    public HttpListenerService(String name) {
        super(name);
    }

    @Override
    protected OpenListener createOpenListener() {
        return new HttpOpenListener(getBufferPool().getValue(), OptionMap.create(UndertowOptions.BUFFER_PIPELINED_DATA, true), getBufferSize());
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener)
            throws IOException {
        server = worker.createStreamConnectionServer(socketAddress, acceptListener, SERVER_OPTIONS);
        server.resumeAccepts();
        UndertowLogger.ROOT_LOGGER.listenerStarted("HTTP", getName(), socketAddress);
    }

    @Override
    protected void stopListening() {
        server.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("HTTP", getName());
        IoUtils.safeClose(server);
        server = null;
        UndertowLogger.ROOT_LOGGER.listenerStopped("HTTP", getName(), getBinding().getValue().getSocketAddress());
    }

    @Override
    public HttpListenerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

}
