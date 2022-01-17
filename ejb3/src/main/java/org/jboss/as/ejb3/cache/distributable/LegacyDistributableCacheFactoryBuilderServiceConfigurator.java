/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.cache.distributable;

import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.LegacyBeanManagementProviderFactory;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service that returns a distributable {@link org.jboss.as.ejb3.cache.CacheFactoryBuilder} using a legacy bean management provider
 * loaded from the classpath.
 *
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class LegacyDistributableCacheFactoryBuilderServiceConfigurator<K, V extends Identifiable<K> & Contextual<Batch>> extends AbstractDistributableCacheFactoryBuilderServiceConfigurator<K, V> {

    public LegacyDistributableCacheFactoryBuilderServiceConfigurator(PathAddress address, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        super(address);
        this.accept(new SimpleSupplierDependency<>(load().createBeanManagementProvider(address.getLastElement().getValue(), config)));
    }

    private static LegacyBeanManagementProviderFactory load() {
        PrivilegedAction<Iterable<LegacyBeanManagementProviderFactory>> action = new PrivilegedAction<Iterable<LegacyBeanManagementProviderFactory>>() {
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
