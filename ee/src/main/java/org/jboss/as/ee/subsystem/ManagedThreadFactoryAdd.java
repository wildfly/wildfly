/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryAdd extends AbstractAddStepHandler {

    static final ManagedThreadFactoryAdd INSTANCE = new ManagedThreadFactoryAdd();

    private ManagedThreadFactoryAdd() {
        super(ManagedThreadFactoryResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        final String jndiName = ManagedExecutorServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        final int priority = ManagedThreadFactoryResourceDefinition.PRIORITY_AD.resolveModelAttribute(context, model).asInt();

        final ManagedThreadFactoryService service = new ManagedThreadFactoryService(name, jndiName, priority);
        final ServiceBuilder<ManagedThreadFactoryImpl> serviceBuilder = context.getServiceTarget().addService(ConcurrentServiceNames.getManagedThreadFactoryServiceName(name), service);
        String contextService = null;
        if(model.hasDefined(ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE)) {
            contextService = ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE_AD.resolveModelAttribute(context, model).asString();
        }
        if (contextService != null) {
            serviceBuilder.addDependency(ConcurrentServiceNames.getContextServiceServiceName(contextService), ContextServiceImpl.class, service.getContextServiceInjector());
        }

        serviceBuilder.install();
    }
}
