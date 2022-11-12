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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.subsystem.DistributableStatefulSessionBeanCacheProviderResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanProviderRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;

/**
 * Configures a service providing a distributable stateful session bean cache provider that uses capabilities provided by the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DistributableStatefulSessionBeanCacheProviderServiceConfigurator<K, V extends StatefulSessionBeanInstance<K>> extends AbstractDistributableStatefulSessionBeanCacheProviderServiceConfigurator<K, V> {

    public DistributableStatefulSessionBeanCacheProviderServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        // if the attribute is undefined, pass null when generating the service name to pick up the default bean management provider
        String provider = DistributableStatefulSessionBeanCacheProviderResourceDefinition.Attribute.BEAN_MANAGEMENT.resolveModelAttribute(context, model).asStringOrNull();
        this.accept(new ServiceSupplierDependency<>(BeanProviderRequirement.BEAN_MANAGEMENT_PROVIDER.getServiceName(context, provider)));
        return this;
    }
}
