/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow;

import io.undertow.server.session.SessionIdGenerator;

import java.util.function.Supplier;

/**
 * Adapts a {@link SessionIdGenerator} to a {@link IdentifierFactory}.
 * @author Paul Ferraro
 */
public class IdentifierFactoryAdapter implements Supplier<String> {

    private final SessionIdGenerator generator;

    public IdentifierFactoryAdapter(SessionIdGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String get() {
        return this.generator.createSessionId();
    }
}
