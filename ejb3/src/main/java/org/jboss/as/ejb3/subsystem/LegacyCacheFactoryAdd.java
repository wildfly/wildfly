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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Configure, build and install CacheFactoryBuilders to support SFSB usage.
 *
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCacheFactoryAdd extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String passivationStore = LegacyCacheFactoryResourceDefinition.PASSIVATION_STORE.resolveModelAttribute(context, model).asStringOrNull();

        final Collection<String> unwrappedAliasValues = LegacyCacheFactoryResourceDefinition.ALIASES.unwrap(context,model);
        final Set<String> aliases = unwrappedAliasValues != null ? new HashSet<>(unwrappedAliasValues) : Collections.<String>emptySet();

        ServiceDependency<StatefulSessionBeanCacheProvider> provider = (passivationStore != null) ? new DistributableStatefulSessionBeanCacheProviderResourceDefinition().apply(passivationStore) : new SimpleStatefulSessionBeanCacheProviderResourceDefinition().resolve(context, model);
        CapabilityServiceInstaller.Builder<StatefulSessionBeanCacheProvider, StatefulSessionBeanCacheProvider> builder = CapabilityServiceInstaller.builder(StatefulSessionBeanCacheProviderResourceDefinition.CAPABILITY, provider);
        for (String alias : aliases) {
            builder.provides(ServiceNameFactory.resolveServiceName(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, alias));
        }
        builder.build().install(context);
    }
}
