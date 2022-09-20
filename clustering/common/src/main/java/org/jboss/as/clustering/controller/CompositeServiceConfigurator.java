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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * A {@link ServiceConfigurator} facade for collecting and building a set of {@link ServiceConfigurator} instances.
 * @author Paul Ferraro
 */
public class CompositeServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, Consumer<ServiceConfigurator> {

    private final List<ServiceConfigurator> configurators = new LinkedList<>();

    public CompositeServiceConfigurator() {
        super(null);
    }

    public CompositeServiceConfigurator(ServiceName name) {
        super(name);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        List<ServiceBuilder<?>> builders = new ArrayList<>(this.configurators.size());
        for (ServiceConfigurator configurator : this.configurators) {
            builders.add(configurator.build(target));
        }
        return new CompositeServiceBuilder<>(builders);
    }

    @Override
    public void accept(ServiceConfigurator configurator) {
        this.configurators.add(configurator);
    }
}
