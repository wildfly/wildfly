/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSourceService;
import org.jboss.as.ejb3.cache.impl.factory.GroupAwareCacheFactoryService;
import org.jboss.as.ejb3.cache.impl.factory.NonPassivatingCacheFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class CacheFactoryAdd extends AbstractAddStepHandler {

    private final AttributeDefinition[] attributes;

    CacheFactoryAdd(AttributeDefinition... attributes) {
        this.attributes = attributes;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        String cacheName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        model.get(EJB3SubsystemModel.NAME).set(cacheName);

        for (AttributeDefinition attr: this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {
        // add this to the service controllers
        serviceControllers.addAll(this.installRuntimeServices(context, model, verificationHandler));
    }

    Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        String name = model.require(EJB3SubsystemModel.NAME).asString();
        String passivationStore = model.hasDefined(EJB3SubsystemModel.PASSIVATION_STORE) ? model.get(EJB3SubsystemModel.PASSIVATION_STORE).asString() : null;

        Set<String> aliases = new HashSet<String>();
        if (model.hasDefined(EJB3SubsystemModel.ALIASES)) {
            for (ModelNode alias: model.get(EJB3SubsystemModel.ALIASES).asList()) {
                aliases.add(alias.asString());
            }
        }

        ServiceTarget target = context.getServiceTarget();
        ServiceBuilder<?> builder = (passivationStore != null) ? new GroupAwareCacheFactoryService<Serializable, Cacheable<Serializable>>(name, aliases).build(target, passivationStore) : new NonPassivatingCacheFactoryService<Serializable, Cacheable<Serializable>>(name, aliases).build(target);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        ServiceController<?> controller = builder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        if (passivationStore != null) {
            InjectedValue<String> clusterName = new InjectedValue<String>();
            ServiceController<?> clusterNameController = target.addService(ClusteredBackingCacheEntryStoreSourceService.getCacheFactoryClusterNameServiceName(name), new ValueService<String>(clusterName))
                    .addDependency(ClusteredBackingCacheEntryStoreSourceService.getPassivationStoreClusterNameServiceName(passivationStore), String.class, clusterName)
                    .setInitialMode(ServiceController.Mode.ON_DEMAND)
                    .install();
            return Arrays.<ServiceController<?>>asList(controller, clusterNameController);
        }
        return Collections.<ServiceController<?>>singleton(controller);
    }
}
