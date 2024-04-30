/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * A {@link ServiceBuilder} facade for installing a set of {@link ServiceBuilder} instances.
 * @author Paul Ferraro
 */
public class CompositeServiceBuilder<T> extends DelegatingServiceBuilder<T> {

    private final Iterable<ServiceBuilder<?>> builders;

    public CompositeServiceBuilder(Iterable<ServiceBuilder<?>> builders) {
        super(null);
        this.builders = builders;
    }

    @Override
    public ServiceBuilder<T> setInitialMode(ServiceController.Mode mode) {
        for (ServiceBuilder<?> builder : this.builders) {
            builder.setInitialMode(mode);
        }
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(LifecycleListener listener) {
        for (ServiceBuilder<?> builder : this.builders) {
            builder.addListener(listener);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceController<T> install() {
        List<ServiceController<?>> controllers = new LinkedList<>();
        for (ServiceBuilder<?> builder : this.builders) {
            controllers.add(builder.install());
        }
        return !controllers.isEmpty() ? (ServiceController<T>) controllers.get(0) : null;
    }

    @Override
    public <V> Supplier<V> requires(ServiceName name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> Consumer<V> provides(ServiceName... names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> setInstance(Service service) {
        throw new UnsupportedOperationException();
    }
}
