/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.subsystem.DistributableCacheFactoryResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.EjbProviderRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;

/**
 * Service that returns a distributable {@link org.jboss.as.ejb3.cache.CacheFactoryBuilder} using a beam management provider
 * from the distributable-ejb subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryBuilderServiceConfigurator<K, V extends Identifiable<K> & Contextual<Batch>> extends AbstractDistributableCacheFactoryBuilderServiceConfigurator<K, V> {

    public DistributableCacheFactoryBuilderServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        // if the attribute is undefined, pass null when generating the service name to pick up the default bean management provider
        String provider = DistributableCacheFactoryResourceDefinition.Attribute.BEAN_MANAGEMENT.resolveModelAttribute(context, model).asStringOrNull();
        this.accept(new ServiceSupplierDependency<>(EjbProviderRequirement.BEAN_MANAGEMENT_PROVIDER.getServiceName(context, provider)));
        return this;
    }
}
