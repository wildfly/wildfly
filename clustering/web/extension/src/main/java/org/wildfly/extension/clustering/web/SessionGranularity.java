/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;

/**
 * Enumerates the session granularity values.
 * @author Paul Ferraro
 */
public enum SessionGranularity {

    SESSION(SessionAttributePersistenceStrategy.COARSE),
    ATTRIBUTE(SessionAttributePersistenceStrategy.FINE),
    ;
    private final SessionAttributePersistenceStrategy strategy;

    SessionGranularity(SessionAttributePersistenceStrategy strategy) {
        this.strategy = strategy;
    }

    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.strategy;
    }
}
