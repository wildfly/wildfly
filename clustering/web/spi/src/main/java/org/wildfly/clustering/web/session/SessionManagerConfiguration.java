/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import java.util.function.Supplier;

import org.wildfly.clustering.ee.expiration.ExpirationConfiguration;

/**
 * Encapsulates the configuration of a session manager.
 * @author Paul Ferraro
 * @param <SC> the servlet context type
 */
public interface SessionManagerConfiguration<SC> extends ExpirationConfiguration<ImmutableSession> {
    SC getServletContext();
    Supplier<String> getIdentifierFactory();
}