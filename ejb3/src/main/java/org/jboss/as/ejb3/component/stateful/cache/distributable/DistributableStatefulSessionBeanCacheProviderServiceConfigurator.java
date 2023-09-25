/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
