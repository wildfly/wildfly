/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Simple {@link ResourceServiceHandler} that installs/removes a single service via a {@link ResourceServiceBuilderFactory}.
 * @author Paul Ferraro
 */
public class SimpleResourceServiceHandler implements ResourceServiceHandler {

    private ResourceServiceConfiguratorFactory factory;

    public SimpleResourceServiceHandler(ResourceServiceConfiguratorFactory factory) {
        this.factory = factory;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        this.factory.createServiceConfigurator(context.getCurrentAddress()).configure(context, model).build(context.getServiceTarget()).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        context.removeService(this.factory.createServiceConfigurator(context.getCurrentAddress()).getServiceName());
    }
}
