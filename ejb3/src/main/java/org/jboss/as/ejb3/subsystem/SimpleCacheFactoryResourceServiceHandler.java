/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.cache.simple.SimpleCacheFactoryBuilderServiceConfigurator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Configure, build and install CacheFactoryBuilders to support SFSB usage.
 *
 * @author Richard Achmatowicz
 */
public class SimpleCacheFactoryResourceServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();

        ServiceTarget target = context.getServiceTarget();
        // set up the CacheFactoryBuilder service for a non-distributable, non-passivating cache factory
        ServiceConfigurator configurator = new SimpleCacheFactoryBuilderServiceConfigurator<>(name);
        ServiceBuilder<?> builder = configurator.build(target);
        builder.install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        // remove the CacheFactoryBuilder service for a non-distributable, non-passivating cache factory
        context.removeService(new SimpleCacheFactoryBuilderServiceConfigurator<>(name).getServiceName());
    }
}
