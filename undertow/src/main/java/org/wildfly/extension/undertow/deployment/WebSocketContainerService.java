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

package org.wildfly.extension.undertow.deployment;

import io.undertow.websockets.jsr.ServerWebSocketContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.Pool;
import org.xnio.XnioWorker;

/**
 * @author Stuart Douglas
 */
public class WebSocketContainerService implements Service<ServerWebSocketContainer> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("WebSocketContainerService");

    private final ServerWebSocketContainer container;
    private final InjectedValue<Pool> injectedBuffer = new InjectedValue<>();
    private final InjectedValue<XnioWorker> xnioWorker = new InjectedValue<>();

    public WebSocketContainerService(final ServerWebSocketContainer container) {
        this.container = container;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        //HttpClient client = HttpClient.create(xnioWorker.getValue(), OptionMap.EMPTY);
        container.start(xnioWorker.getValue(), injectedBuffer.getValue());
    }

    @Override
    public void stop(final StopContext stopContext) {
        container.stop();
    }

    @Override
    public ServerWebSocketContainer getValue() throws IllegalStateException, IllegalArgumentException {
        return container;
    }

    public InjectedValue<Pool> getInjectedBuffer() {
        return injectedBuffer;
    }

    public InjectedValue<XnioWorker> getXnioWorker() {
        return xnioWorker;
    }
}
