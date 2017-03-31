/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Stuart Douglas
 */
class WebServerService implements CommonWebServer, Service<WebServerService> {
    private InjectedValue<Server> serverInjectedValue = new InjectedValue<>();

    InjectedValue<Server> getServerInjectedValue() {
        return serverInjectedValue;
    }

    //todo we need to handle cases when deployments reference listeners/server/host directly
    @Override
    public int getPort(final String protocol, final boolean secure) {
        Map<String, UndertowListener> listeners = getListenerMap();
        UndertowListener listener = null;
        for (String p : listeners.keySet()) {
            if (protocol.toLowerCase().contains(p)) {
                listener = listeners.get(p);
            }
        }
        if (listener != null && listener.getProtocol() == HttpListenerService.PROTOCOL && secure) {
            if (listeners.containsKey(HttpsListenerService.PROTOCOL)) {
                listener = listeners.get(HttpsListenerService.PROTOCOL);
            } else {
                UndertowLogger.ROOT_LOGGER.secureListenerNotAvailableForPort(protocol);
            }
        }
        if (listener != null) {
            SocketBinding binding = listener.getSocketBinding();
            return binding.getAbsolutePort();
        }
        throw UndertowLogger.ROOT_LOGGER.noPortListeningForProtocol(protocol);

    }

    private Map<String, UndertowListener> getListenerMap() {
        HashMap<String, UndertowListener> listeners = new HashMap<>();
        for (UndertowListener listener : serverInjectedValue.getValue().getListeners()) {
            listeners.put(listener.getProtocol(), listener);
        }
        return listeners;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {
    }

    @Override
    public WebServerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
