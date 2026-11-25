/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.function.BiConsumer;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.ToIntFunction;
import org.wildfly.clustering.function.ToLongFunction;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionReference;

/**
 * @author Paul Ferraro
 */
public interface UndertowSession extends Session {
    /** Name of internal attribute Undertow to store current web socket connections */
    String WEB_SOCKET_CHANNELS_ATTRIBUTE = "io.undertow.websocket.current-connections";
    Function<UndertowSession, String> IDENTIFIER = UndertowSession::getId;
    Predicate<UndertowSession> VALID = UndertowSession::isValid;
    ToLongFunction<UndertowSession> CREATION_TIME = UndertowSession::getCreationTime;
    ToLongFunction<UndertowSession> LAST_ACCESSED_TIME = UndertowSession::getLastAccessedTime;
    ToIntFunction<UndertowSession> MAX_INACTIVE_INTERVAL = UndertowSession::getMaxInactiveInterval;
    BiConsumer<UndertowSession, Integer> SET_MAX_INACTIVE_INTERVAL = UndertowSession::setMaxInactiveInterval;
    Function<UndertowSession, Set<String>> ATTRIBUTE_NAMES = UndertowSession::getAttributeNames;
    BiFunction<UndertowSession, String, Object> GET_ATTRIBUTE = UndertowSession::getAttribute;
    BiFunction<UndertowSession, Map.Entry<String, Object>, Object> SET_ATTRIBUTE = (session, entry) -> session.setAttribute(entry.getKey(), entry.getValue());
    BiFunction<UndertowSession, String, Object> REMOVE_ATTRIBUTE = UndertowSession::removeAttribute;
    BiConsumer<UndertowSession, HttpServerExchange> INVALIDATE = UndertowSession::invalidate;

    boolean isValid();

    @Override
    default boolean isInvalid() {
        return !this.isValid();
    }

    @Override
    UndertowSessionManager getSessionManager();

    @Override
    default SessionReference getReference() {
        return new DistributableSessionReference(this.getSessionManager(), this.getId());
    }
}
