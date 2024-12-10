/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.jboss.as.clustering.infinispan.tx.InfinispanXAResourceRecovery;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 *
 */
public enum XAResourceRecoveryServiceInstallerFactory implements Function<BinaryServiceConfiguration, ResourceServiceInstaller> {
    INSTANCE;

    static final NullaryServiceDescriptor<XAResourceRecoveryRegistry> XA_RESOURCE_RECOVERY_REGISTRY = NullaryServiceDescriptor.of("org.wildfly.transactions.xa-resource-recovery-registry", XAResourceRecoveryRegistry.class);

    @Override
    public ResourceServiceInstaller apply(BinaryServiceConfiguration config) {
        ServiceDependency<XAResourceRecoveryRegistry> registry = ServiceDependency.on(XA_RESOURCE_RECOVERY_REGISTRY);
        ServiceDependency<XAResourceRecovery> recovery = config.getServiceDependency(InfinispanServiceDescriptor.CACHE).map(new Function<>() {
            @Override
            public XAResourceRecovery apply(Cache<?, ?> cache) {
                return cache.getCacheConfiguration().transaction().recovery().enabled() ? new InfinispanXAResourceRecovery(cache) : null;
            }
        });
        Consumer<XAResourceRecovery> start = new Consumer<>() {
            @Override
            public void accept(XAResourceRecovery recovery) {
                if (recovery != null) {
                    registry.get().addXAResourceRecovery(recovery);
                }
            }
        };
        Consumer<XAResourceRecovery> stop = new Consumer<>() {
            @Override
            public void accept(XAResourceRecovery recovery) {
                if (recovery != null) {
                    registry.get().removeXAResourceRecovery(recovery);
                }
            }
        };
        return ServiceInstaller.builder(recovery)
                .requires(registry)
                .onStart(start)
                .onStop(stop)
                .build();
    }
}
