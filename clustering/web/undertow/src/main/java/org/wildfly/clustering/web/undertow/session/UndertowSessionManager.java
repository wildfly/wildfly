/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.util.Map;

import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.session.SessionManager;

import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;

/**
 * Exposes additional session manager aspects to a session.
 * @author Paul Ferraro
 */
public interface UndertowSessionManager extends io.undertow.server.session.SessionManager, Service {
    /**
     * Returns the configured session listeners for this web application
     * @return the session listeners
     */
    SessionListeners getSessionListeners();

    @Override
    default void registerSessionListener(SessionListener listener) {
        this.getSessionListeners().addSessionListener(listener);
    }

    @Override
    default void removeSessionListener(SessionListener listener) {
        this.getSessionListeners().removeSessionListener(listener);
    }

    /**
     * Returns underlying distributable session manager implementation.
     * @return a session manager
     */
    SessionManager<Map<String, Object>> getSessionManager();

    @Override
    default boolean isStarted() {
        return this.getSessionManager().isStarted();
    }

    @Override
    default void start() {
        this.getSessionManager().start();
    }

    @Override
    default void stop() {
        this.getSessionManager().stop();
    }
}
