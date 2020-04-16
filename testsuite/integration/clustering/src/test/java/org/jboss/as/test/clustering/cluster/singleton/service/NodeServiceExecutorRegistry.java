/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.function.Supplier;

import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.ServiceValueCaptor;
import org.jboss.as.clustering.controller.ServiceValueExecutorRegistry;
import org.jboss.as.clustering.controller.ServiceValueRegistry;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
public enum NodeServiceExecutorRegistry implements FunctionExecutorRegistry<Supplier<Node>>, ServiceValueRegistry<Supplier<Node>> {
    INSTANCE;

    private final ServiceValueExecutorRegistry<Supplier<Node>> registry = new ServiceValueExecutorRegistry<>();

    @Override
    public ServiceValueCaptor<Supplier<Node>> add(ServiceName name) {
        return this.registry.add(name);
    }

    @Override
    public ServiceValueCaptor<Supplier<Node>> remove(ServiceName name) {
        return this.registry.remove(name);
    }

    @Override
    public FunctionExecutor<Supplier<Node>> get(ServiceName name) {
        return this.registry.get(name);
    }
}
