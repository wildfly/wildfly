/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.subsystem;

import java.util.List;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactoryRegistry;
import org.jboss.as.cmp.keygenerator.uuid.UUIDKeyGeneratorFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public abstract class AbstractKeyGeneratorAdd extends AbstractAddStepHandler  implements DescriptionProvider {

    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        final Service<KeyGeneratorFactory> keyGeneratorFactory = getKeyGeneratorFactory(operation);
        final ServiceBuilder<KeyGeneratorFactory> factoryServiceBuilder = context.getServiceTarget().addService(getServiceName(name), keyGeneratorFactory)
                .addDependency(KeyGeneratorFactoryRegistry.SERVICE_NAME, KeyGeneratorFactoryRegistry.class, KeyGeneratorFactoryRegistry.getRegistryInjector(name, keyGeneratorFactory))
                .addListener(verificationHandler);
        addDependencies(operation, keyGeneratorFactory, factoryServiceBuilder);
        newControllers.add(factoryServiceBuilder.install());
    }

    protected abstract Service<KeyGeneratorFactory> getKeyGeneratorFactory(final ModelNode operation);

    protected abstract ServiceName getServiceName(final String name);

    protected void addDependencies(final ModelNode operation, final Service<KeyGeneratorFactory> keyGeneratorFactory, final ServiceBuilder<KeyGeneratorFactory> factoryServiceBuilder) {
    }
}
