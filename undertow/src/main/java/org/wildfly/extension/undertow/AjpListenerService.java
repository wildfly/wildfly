/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.undertow.ajp.AjpOpenListener;
import io.undertow.server.OpenListener;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class AjpListenerService extends AbstractListenerService<AjpListenerService> {

    private volatile AcceptingChannel<StreamConnection> server;
    private final String scheme;

    public AjpListenerService(String name, final String scheme) {
        super(name);
        this.scheme = scheme;
    }

    @Override
    protected OpenListener createOpenListener() {
        AjpOpenListener ajpOpenListener = new AjpOpenListener(getBufferPool().getValue(), getBufferSize());
        ajpOpenListener.setScheme(scheme);
        return ajpOpenListener;
    }

    @Override
    void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException {
        server = worker.createStreamConnectionServer(socketAddress, acceptListener, SERVER_OPTIONS);
        server.resumeAccepts();
        UndertowLogger.ROOT_LOGGER.listenerStarted("AJP", getName(), binding.getValue().getSocketAddress());
    }

    @Override
    void stopListening() {
        server.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("AJP", getName());
        IoUtils.safeClose(server);
        UndertowLogger.ROOT_LOGGER.listenerStopped("AJP", getName(), getBinding().getValue().getSocketAddress());
    }

    @Override
    public AjpListenerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public boolean isSecure() {
        return false;
    }
}
