/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.OptionalInt;
import java.util.ServiceLoader;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementConfiguration;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementProviderFactory;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class PassivationStoreAdd extends AbstractAddStepHandler {

    private static final LegacyBeanManagementProviderFactory LEGACY_PROVIDER_FACTORY = ServiceLoader.load(LegacyBeanManagementProviderFactory.class, LegacyBeanManagementProviderFactory.class.getClassLoader()).findFirst().orElseThrow(IllegalStateException::new);

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        int initialMaxSize = PassivationStoreResourceDefinition.MAX_SIZE.resolveModelAttribute(context, model).asInt();
        String containerName = PassivationStoreResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        String cacheName = PassivationStoreResourceDefinition.BEAN_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.install(context, operation, initialMaxSize, containerName, cacheName);
    }

    protected void install(OperationContext context, ModelNode operation, final int maxSize, final String containerName, final String cacheName) {
        LegacyBeanManagementConfiguration config = new LegacyBeanManagementConfiguration() {
            @Override
            public String getContainerName() {
                return containerName;
            }

            @Override
            public String getCacheName() {
                return cacheName;
            }

            @Override
            public OptionalInt getMaxActiveBeans() {
                return OptionalInt.of(maxSize);
            }
        };
        ServiceInstaller.builder(LEGACY_PROVIDER_FACTORY.createBeanManagementProvider(context.getCurrentAddressValue(), config))
                .provides(ServiceNameFactory.resolveServiceName(BeanManagementProvider.SERVICE_DESCRIPTOR, context.getCurrentAddressValue()))
                .build().install(context);
    }
}
