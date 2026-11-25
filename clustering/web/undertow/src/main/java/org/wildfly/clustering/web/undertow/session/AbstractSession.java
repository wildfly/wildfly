/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.function.Supplier;

/**
 * An abstract Undertow session implementation.
 * @author Paul Ferraro
 */
public abstract class AbstractSession implements UndertowSession {

    private final UndertowSessionManager manager;
    private final Supplier<String> id;

    protected AbstractSession(UndertowSessionManager manager, Supplier<String> id) {
        this.manager = manager;
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id.get();
    }

    @Override
    public UndertowSessionManager getSessionManager() {
        return this.manager;
    }
}
