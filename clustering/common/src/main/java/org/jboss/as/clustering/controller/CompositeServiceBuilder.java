/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.controller;

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

    @Override
    public ServiceController<T> install() {
        for (ServiceBuilder<?> builder : this.builders) {
            builder.install();
        }
        return null;
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
