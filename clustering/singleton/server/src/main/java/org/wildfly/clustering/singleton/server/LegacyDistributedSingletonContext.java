/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.Optional;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyDistributedSingletonContext<T> extends AbstractSingletonContext<LegacySingletonContext<T>, ServiceValue<T>> implements LegacySingletonContext<T> {

    private final ServiceValue<T> primaryLifecycle;

    public LegacyDistributedSingletonContext(SingletonServiceContext context, ServiceValue<T> primaryLifecycle) {
        super(context, primaryLifecycle);
        this.primaryLifecycle = primaryLifecycle;
    }

    @Override
    public LegacySingletonContext<T> get() {
        return this;
    }

    @Override
    public Optional<T> getLocalValue() {
        try {
            return this.isPrimary() ? Optional.ofNullable(this.primaryLifecycle.getValue()) : null;
        } catch (IllegalStateException e) {
            // This might happen if primary service has not yet started, or if node is no longer the primary node
            return null;
        }
    }
}
