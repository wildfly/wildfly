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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configure, build and install CacheFactoryBuilders to support SFSB usage.
 *
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCacheFactoryAdd extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        final String name = pathAddress.getLastElement().getValue();

        ModelNode passivationStoreModel = LegacyCacheFactoryResourceDefinition.PASSIVATION_STORE.resolveModelAttribute(context,model);
        String passivationStore = passivationStoreModel.isDefined() ? passivationStoreModel.asString() : null;

        final Collection<String> unwrappedAliasValues = LegacyCacheFactoryResourceDefinition.ALIASES.unwrap(context,model);
        final Set<String> aliases = unwrappedAliasValues != null ? new HashSet<>(unwrappedAliasValues) : Collections.<String>emptySet();
        ServiceDependency<StatefulSessionBeanCacheProvider> provider = (passivationStore != null) ? ServiceDependency.on(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, passivationStore) : new SimpleStatefulSessionBeanCacheProviderResourceDefinition().resolve(context, model);
        ServiceInstaller.UnaryBuilder<StatefulSessionBeanCacheProvider, StatefulSessionBeanCacheProvider> builder = ServiceInstaller.builder(provider).provides(ServiceNameFactory.resolveServiceName(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, name));
        for (String alias : aliases) {
            builder.provides(ServiceNameFactory.resolveServiceName(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, alias));
        }
        builder.build().install(context);
    }
}
