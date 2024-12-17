/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.user.elytron;

import java.util.function.Supplier;

import io.undertow.server.session.SessionIdGenerator;

/**
 * Adapts a {@link String} {@link Supplier} to a {@link SessionIdGenerator}.
 * @author Paul Ferraro
 */
public class SessionIdGeneratorAdapter implements SessionIdGenerator {

    private final Supplier<String> generator;

    public SessionIdGeneratorAdapter(Supplier<String> generator) {
        this.generator = generator;
    }

    @Override
    public String createSessionId() {
        return this.generator.get();
    }
}
