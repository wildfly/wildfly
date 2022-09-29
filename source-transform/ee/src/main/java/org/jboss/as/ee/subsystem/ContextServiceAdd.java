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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.concurrent.DefaultContextSetupProviderImpl;
import org.jboss.as.ee.concurrent.service.ContextServiceService;
import org.jboss.dmr.ModelNode;

/**
 * @author Eduardo Martins
 */
public class ContextServiceAdd extends AbstractAddStepHandler {

    static final ContextServiceAdd INSTANCE = new ContextServiceAdd();

    private ContextServiceAdd() {
        super(ContextServiceResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        final ModelNode model = resource.getModel();
        final String name = context.getCurrentAddressValue();
        final String jndiName = ContextServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        // TODO *FOLLOW UP* deprecate USE_TRANSACTION_SETUP_PROVIDER_AD since it's of no use anymore (replaced by spec's context service config of context type Transaction)
        // install the service which manages the default context service
        final ContextServiceService contextServiceService = new ContextServiceService(name, jndiName, new DefaultContextSetupProviderImpl(), ContextServiceTypesConfiguration.DEFAULT);
        context.getCapabilityServiceTarget().addCapability(ContextServiceResourceDefinition.CAPABILITY).setInstance(contextServiceService).install();
    }
}
