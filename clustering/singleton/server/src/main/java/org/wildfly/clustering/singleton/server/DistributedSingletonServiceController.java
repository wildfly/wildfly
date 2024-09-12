/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.jboss.msc.service.DelegatingServiceController;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonState;
import org.wildfly.clustering.singleton.service.SingletonServiceController;

/**
 * {@link ServiceController} for singleton services.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceController<T> extends DelegatingServiceController<T> implements SingletonServiceController<T> {
    private final Supplier<Singleton> singleton;

    public DistributedSingletonServiceController(ServiceController<T> controller, Supplier<Singleton> singleton) {
        super(controller);
        this.singleton = singleton;
    }

    @Override
    public SingletonState getSingletonState() {
        return this.singleton.get().getSingletonState();
    }
}
