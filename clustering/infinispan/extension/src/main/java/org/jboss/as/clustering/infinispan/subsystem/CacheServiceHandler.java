/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ServiceLoader;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SubGroupServiceNameFactory;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;

/**
 * @author Paul Ferraro
 */
public class CacheServiceHandler implements ResourceServiceHandler {

    private final ResourceServiceBuilderFactory<Configuration> builderFactory;
    private final Class<? extends CacheGroupBuilderProvider> providerClass;

    CacheServiceHandler(ResourceServiceBuilderFactory<Configuration> builderFactory, Class<? extends CacheGroupBuilderProvider> providerClass) {
        this.builderFactory = builderFactory;
        this.providerClass = providerClass;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress();
        PathAddress containerAddress = cacheAddress.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        this.builderFactory.createBuilder(cacheAddress).configure(context, model).build(target).install();

        new CacheBuilder<>(containerName, cacheName).build(target).install();
        new XAResourceRecoveryBuilder(containerName, cacheName).build(target).install();

        BinderServiceBuilder<?> bindingBuilder = new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(containerName, cacheName), CacheServiceName.CACHE.getServiceName(containerName, cacheName), Cache.class);
        String jndiName = ModelNodes.asString(CacheResourceDefinition.Attribute.JNDI_NAME.getDefinition().resolveModelAttribute(context, model));
        if (jndiName != null) {
            bindingBuilder.alias(ContextNames.bindInfoFor(JndiNameFactory.parse(jndiName).getAbsoluteName()));
        }
        bindingBuilder.build(target).install();

        for (CacheGroupBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(containerName, cacheName)) {
                builder.build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress cacheAddress = context.getCurrentAddress();
        PathAddress containerAddress = cacheAddress.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        for (CacheGroupBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(containerName, cacheName)) {
                context.removeService(builder.getServiceName());
            }
        }

        context.removeService(InfinispanBindingFactory.createCacheBinding(containerName, cacheName).getBinderServiceName());

        for (SubGroupServiceNameFactory factory : CacheServiceName.values()) {
            context.removeService(factory.getServiceName(containerName, cacheName));
        }

        context.removeService(this.builderFactory.createBuilder(cacheAddress).getServiceName());
    }
}
