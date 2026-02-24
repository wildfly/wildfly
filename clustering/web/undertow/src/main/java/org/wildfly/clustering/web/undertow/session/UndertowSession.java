/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import io.undertow.server.session.Session;

/**
 * @author Paul Ferraro
 */
public interface UndertowSession extends Session {
    /** Name of internal attribute Undertow to store current web socket connections */
    String WEB_SOCKET_CHANNELS_ATTRIBUTE = "io.undertow.websocket.current-connections";

    boolean isValid();

    default boolean isInvalid() {
        return !this.isValid();
    }

    @Override
    UndertowSessionManager getSessionManager();
}
