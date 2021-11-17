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
import org.jboss.as.ejb3.cache.CacheFactoryBuilderServiceNameProvider;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderServiceNameProvider;
import org.jboss.as.ejb3.cache.simple.SimpleCacheFactoryBuilderServiceConfigurator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Configure, build and install CacheFactoryBuilders to support SFSB usage.
 *
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCacheFactoryAdd extends AbstractAddStepHandler {

    LegacyCacheFactoryAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        final String name = pathAddress.getLastElement().getValue();

        ModelNode passivationStoreModel = LegacyCacheFactoryResourceDefinition.PASSIVATION_STORE.resolveModelAttribute(context,model);
        String passivationStore = passivationStoreModel.isDefined() ? passivationStoreModel.asString() : null;

        final Collection<String> unwrappedAliasValues = LegacyCacheFactoryResourceDefinition.ALIASES.unwrap(context,model);
        final Set<String> aliases = unwrappedAliasValues != null ? new HashSet<>(unwrappedAliasValues) : Collections.<String>emptySet();
        ServiceTarget target = context.getServiceTarget();
        // set up the CacheFactoryBuilder service
        ServiceConfigurator configurator = (passivationStore != null) ? new IdentityServiceConfigurator<>(new CacheFactoryBuilderServiceNameProvider(name).getServiceName(),
                new DistributableCacheFactoryBuilderServiceNameProvider(passivationStore).getServiceName()) : new SimpleCacheFactoryBuilderServiceConfigurator<>(pathAddress);
        ServiceBuilder<?> builder = configurator.build(target);
        // set up aliases to the CacheFactoryBuilder service
        for (String alias: aliases) {
            new IdentityServiceConfigurator<>(new CacheFactoryBuilderServiceNameProvider(alias).getServiceName(), configurator.getServiceName()).build(target).install();
        }
        builder.install();
    }
}
