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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.DelegateCacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.simple.SimpleCacheFactoryBuilderService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

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
        for (AttributeDefinition attr: this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        ModelNode passivationStoreModel = CacheFactoryResourceDefinition.PASSIVATION_STORE.resolveModelAttribute(context,model);
        String passivationStore = passivationStoreModel.isDefined() ? passivationStoreModel.asString() : null;

        final Collection<String> unwrappedAliasValues = CacheFactoryResourceDefinition.ALIASES.unwrap(context,model);
        final Set<String> aliases = unwrappedAliasValues != null ? new HashSet<>(unwrappedAliasValues) : Collections.<String>emptySet();
        ServiceTarget target = context.getServiceTarget();
        ServiceBuilder<?> builder = buildCacheFactoryBuilder(target, name, passivationStore);
        for (String alias: aliases) {
            builder.addAliases(CacheFactoryBuilderService.getServiceName(alias));
        }
        builder.install();
    }

    private static ServiceBuilder<?> buildCacheFactoryBuilder(ServiceTarget target, String name, String passivationStore) {
        if (passivationStore == null) {
            return new SimpleCacheFactoryBuilderService<>(name).build(target);
        }
        return new DelegateCacheFactoryBuilderService<>(name, DistributableCacheFactoryBuilderService.getServiceName(passivationStore)).build(target);
    }
}
