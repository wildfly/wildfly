/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;

/**
 * A {@link SessionManagerConfiguration} implementation that delegates to another {@link SessionManagerConfiguration}.
 * @author Paul Ferraro
 * @param <SC> the servlet context type
 */
public class DelegatingSessionManagerConfiguration<SC> implements SessionManagerConfiguration<SC> {

    private final SessionManagerConfiguration<SC> configuration;

    public DelegatingSessionManagerConfiguration(SessionManagerConfiguration<SC> configuration) {
        this.configuration = configuration;
    }

    @Override
    public Consumer<ImmutableSession> getExpirationListener() {
        return this.configuration.getExpirationListener();
    }

    @Override
    public Duration getTimeout() {
        return this.configuration.getTimeout();
    }

    @Override
    public SC getServletContext() {
        return this.configuration.getServletContext();
    }

    @Override
    public Supplier<String> getIdentifierFactory() {
        return this.configuration.getIdentifierFactory();
    }
}
