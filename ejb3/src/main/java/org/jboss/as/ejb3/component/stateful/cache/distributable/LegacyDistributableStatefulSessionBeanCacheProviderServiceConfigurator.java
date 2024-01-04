/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementConfiguration;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementProviderFactory;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A legacy distributable stateful session bean cache provider that does not use the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
@Deprecated
public class LegacyDistributableStatefulSessionBeanCacheProviderServiceConfigurator<K, V extends StatefulSessionBeanInstance<K>> extends AbstractDistributableStatefulSessionBeanCacheProviderServiceConfigurator<K, V> {

    public LegacyDistributableStatefulSessionBeanCacheProviderServiceConfigurator(PathAddress address, LegacyBeanManagementConfiguration config) {
        super(address);
        this.accept(new SimpleSupplierDependency<>(load().createBeanManagementProvider(address.getLastElement().getValue(), config)));
    }

    private static LegacyBeanManagementProviderFactory load() {
        PrivilegedAction<Iterable<LegacyBeanManagementProviderFactory>> action = new PrivilegedAction<>() {
            @Override
            public Iterable<LegacyBeanManagementProviderFactory> run() {
                return ServiceLoader.load(LegacyBeanManagementProviderFactory.class, LegacyBeanManagementProviderFactory.class.getClassLoader());
            }
        };
        Iterator<LegacyBeanManagementProviderFactory> providers = WildFlySecurityManager.doUnchecked(action).iterator();
        if (!providers.hasNext()) {
            throw new ServiceConfigurationError(LegacyBeanManagementProviderFactory.class.getName());
        }
        return providers.next();
    }
}
