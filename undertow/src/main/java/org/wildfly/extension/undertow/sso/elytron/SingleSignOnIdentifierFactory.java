/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.sso.elytron;

import java.util.function.Supplier;

import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.server.session.SessionIdGenerator;

/**
 * Adapts a {@link SessionIdGenerator} to {@link Supplier}.
 * @author Paul Ferraro
 */
public class SingleSignOnIdentifierFactory implements Supplier<String> {

    private final SessionIdGenerator generator;

    public SingleSignOnIdentifierFactory() {
        this(new SecureRandomSessionIdGenerator());
    }

    public SingleSignOnIdentifierFactory(SessionIdGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String get() {
        return this.generator.createSessionId();
    }
}
