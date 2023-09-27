/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProviderServiceNameProvider;
import org.jboss.as.ejb3.component.stateful.cache.simple.SimpleStatefulSessionBeanCacheProviderServiceConfigurator;
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
        ServiceConfigurator configurator = (passivationStore != null) ? new IdentityServiceConfigurator<>(new StatefulSessionBeanCacheProviderServiceNameProvider(name).getServiceName(),
                new StatefulSessionBeanCacheProviderServiceNameProvider(passivationStore).getServiceName()) : new SimpleStatefulSessionBeanCacheProviderServiceConfigurator<>(pathAddress);
        ServiceBuilder<?> builder = configurator.build(target);
        // set up aliases to the CacheFactoryBuilder service
        for (String alias: aliases) {
            new IdentityServiceConfigurator<>(new StatefulSessionBeanCacheProviderServiceNameProvider(alias).getServiceName(), configurator.getServiceName()).build(target).install();
        }
        builder.install();
    }
}
