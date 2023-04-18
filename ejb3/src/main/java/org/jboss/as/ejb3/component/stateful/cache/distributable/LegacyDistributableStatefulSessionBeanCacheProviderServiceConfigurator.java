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
