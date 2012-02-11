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

package org.jboss.as.ejb3.subsystem;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

/**
 * @author Paul Ferraro
 */
public abstract class PassivationStoreAdd extends AbstractAddStepHandler {

    private final AttributeDefinition[] attributes;

    PassivationStoreAdd(AttributeDefinition... attributes) {
        this.attributes = attributes;
    }

    /**
     * Populate the <code>strictMaxPoolModel</code> from the <code>operation</code>
     *
     * @param operation          the operation
     * @param model strict-max-pool ModelNode
     * @throws OperationFailedException
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        model.get(EJB3SubsystemModel.NAME).set(name);

        for (AttributeDefinition attr: this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {
        // add this to the service controllers
        serviceControllers.add(installRuntimeService(context, model, verificationHandler));
    }

    ServiceController<?> installRuntimeService(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        BackingCacheEntryStoreSourceService<?, ?, ?, ?> service = this.createService(model);
        BackingCacheEntryStoreConfig config = service.getValue();
        if (model.hasDefined(EJB3SubsystemModel.IDLE_TIMEOUT)) {
            config.setIdleTimeout(model.get(EJB3SubsystemModel.IDLE_TIMEOUT).asLong());
        }
        if (model.hasDefined(EJB3SubsystemModel.IDLE_TIMEOUT_UNIT)) {
            config.setIdleTimeoutUnit(TimeUnit.valueOf(model.get(EJB3SubsystemModel.IDLE_TIMEOUT_UNIT).asString()));
        }
        if (model.hasDefined(EJB3SubsystemModel.MAX_SIZE)) {
            config.setMaxSize(model.get(EJB3SubsystemModel.MAX_SIZE).asInt());
        }
        ServiceBuilder<?> builder = service.build(context.getServiceTarget());
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        return builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();
    }

    protected abstract BackingCacheEntryStoreSourceService<?, ?, ?, ?> createService(ModelNode model);
}
