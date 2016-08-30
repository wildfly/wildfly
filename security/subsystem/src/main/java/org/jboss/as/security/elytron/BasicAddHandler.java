/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security.elytron;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.Arrays;
import java.util.HashSet;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.security.elytron.BasicService.ValueSupplier;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * This {@link AbstractAddStepHandler} implementation contains code that is common to all elytron integration handlers.
 * It registers the resource capabilities (an array where the first capability is the main capability and remaining values
 * are aliases) and installs a {@link BasicService} that uses a {@link ValueSupplier} to create and return an instance of
 * an object of type T.
 *
 * @param <T> the type of the object returned by the {@link BasicService} that is installed by this handler.
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
abstract class BasicAddHandler<T> extends AbstractAddStepHandler {

    private final RuntimeCapability<?>[] runtimeCapabilities;

    BasicAddHandler(AttributeDefinition[] attributes, RuntimeCapability<?>... runtimeCapabilities) {
        super(new HashSet<>(Arrays.asList(checkNotNullParam("runtimeCapabilities", runtimeCapabilities))), attributes);
        this.runtimeCapabilities = runtimeCapabilities;
    }

    @Override
    protected final void performRuntime(OperationContext context, ModelNode operation, Resource resource)
            throws OperationFailedException {
        String address = context.getCurrentAddressValue();
        ServiceName mainName = runtimeCapabilities[0].fromBaseCapability(address).getCapabilityServiceName();

        ServiceTarget serviceTarget = context.getServiceTarget();
        BasicService<T> basicService = new BasicService<T>();

        ServiceBuilder<T> serviceBuilder = serviceTarget.addService(mainName, basicService);
        for (int i = 1; i < runtimeCapabilities.length; i++) {
            serviceBuilder.addAliases(runtimeCapabilities[i].fromBaseCapability(address).getCapabilityServiceName());
        }

        basicService.setValueSupplier(getValueSupplier(serviceBuilder, context, resource.getModel()));
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    /**
     * Subclasses must implement this method to return a {@link ValueSupplier} instance that will be used to create
     * the object returned by the installed service.
     */
    protected abstract ValueSupplier<T> getValueSupplier(ServiceBuilder<T> serviceBuilder, OperationContext context, ModelNode model) throws OperationFailedException;

}
