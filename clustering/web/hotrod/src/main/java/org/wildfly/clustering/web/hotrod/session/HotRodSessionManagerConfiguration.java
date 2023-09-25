/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.util.function.Consumer;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;

/**
 * Configuration for an {@link HotRodSessionManager}.
 * @param <C> the ServletContext specification type
 * @author Paul Ferraro
 */
public interface HotRodSessionManagerConfiguration<C> extends SessionManagerConfiguration<C>, HotRodConfiguration {
    Registrar<Consumer<ImmutableSession>> getExpirationListenerRegistrar();
    Duration getStopTimeout();
}
