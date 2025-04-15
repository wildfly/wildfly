/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WebServerService implements CommonWebServer, Service<WebServerService> {
    private final Consumer<WebServerService> serviceConsumer;
    private final Supplier<Server> server;

    WebServerService(final Consumer<WebServerService> serviceConsumer, final Supplier<Server> server) {
        this.serviceConsumer = serviceConsumer;
        this.server = server;
    }

    //todo we need to handle cases when deployments reference listeners/server/host directly
    @Override
    public int getPort(final String protocol, final boolean secure) {
        Map<String, UndertowListener> listeners = getListenerMap();
        UndertowListener listener = null;
        for (Map.Entry<String, UndertowListener> entry : listeners.entrySet()) {
            if (protocol.toLowerCase(Locale.ENGLISH).contains(entry.getKey())) {
                listener = entry.getValue();
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
        for (UndertowListener listener : server.get().getListeners()) {
            listeners.put(listener.getProtocol(), listener);
        }
        return listeners;
    }

    @Override
    public void start(final StartContext context) {
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        serviceConsumer.accept(null);
    }

    @Override
    public WebServerService getValue() {
        return this;
    }
}
