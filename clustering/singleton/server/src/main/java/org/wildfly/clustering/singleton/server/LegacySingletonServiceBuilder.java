/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceController;

/**
 * @author Paul Ferraro
 */
public class LegacySingletonServiceBuilder<T> extends AbstractSingletonServiceBuilder<T> {

    private final Supplier<Singleton> singleton;

    public LegacySingletonServiceBuilder(Supplier<Singleton> singleton, SingletonServiceBuilderContext context, ServiceBuilder<T> builder) {
        super(builder, context);
        this.singleton = singleton;
    }

    @Override
    public SingletonServiceBuilder<T> setInstance(Service service) {
        throw new IllegalStateException();
    }

    @Override
    public SingletonServiceController<T> install() {
        return new DistributedSingletonServiceController<>(this.getDelegate().install(), this.singleton);
    }
}
