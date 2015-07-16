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

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.*;

import java.util.ServiceLoader;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceNameFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupAliasBuilderProvider;

/**
 * @author Paul Ferraro
 */
public class CacheContainerServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        // Handle case where ejb subsystem has already installed services for this cache-container
        // This can happen if the ejb cache-container is added to a running server
        if (context.getProcessType().isServer() && !context.isBooting() && name.equals("ejb")) {
            Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            PathElement ejbPath = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
            if (rootResource.hasChild(ejbPath) && rootResource.getChild(ejbPath).hasChild(PathElement.pathElement("service", "remote"))) {
                // Following restart, these services will be installed by this handler, rather than the ejb remote handler
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        context.reloadRequired();
                        context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                    }
                }, OperationContext.Stage.RUNTIME);
                return;
            }
        }

        ServiceTarget target = context.getServiceTarget();

        new GlobalConfigurationBuilder(name).configure(context, model).build(target).install();

        String defaultCache = ModelNodes.asString(DEFAULT_CACHE.getDefinition().resolveModelAttribute(context, model));
        new CacheContainerBuilder(name).setDefaultCache(defaultCache).configure(context, model).build(target).install();

        new KeyAffinityServiceFactoryBuilder(name).build(target).install();

        String jndiName = ModelNodes.asString(CacheContainerResourceDefinition.Attribute.JNDI_NAME.getDefinition().resolveModelAttribute(context, model));
        BinderServiceBuilder<?> bindingBuilder = new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheContainerBinding(name), CacheContainerServiceName.CACHE_CONTAINER.getServiceName(name), CacheContainer.class);
        if (jndiName != null) {
            bindingBuilder.alias(ContextNames.bindInfoFor(JndiNameFactory.parse(jndiName).getAbsoluteName()));
        }
        bindingBuilder.build(target).install();

        if ((defaultCache != null) && !defaultCache.equals(CacheServiceNameFactory.DEFAULT_CACHE)) {
            for (CacheServiceNameFactory nameFactory : CacheServiceName.values()) {
                new AliasServiceBuilder<>(nameFactory.getServiceName(name), nameFactory.getServiceName(name, defaultCache), Object.class).build(target).install();
            }

            new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(name, CacheServiceNameFactory.DEFAULT_CACHE), CacheServiceName.CACHE.getServiceName(name), Cache.class).build(target).install();

            for (CacheGroupAliasBuilderProvider provider : ServiceLoader.load(CacheGroupAliasBuilderProvider.class, CacheGroupAliasBuilderProvider.class.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(name, CacheServiceNameFactory.DEFAULT_CACHE, defaultCache)) {
                    builder.build(target).install();
                }
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE.getDefinition().resolveModelAttribute(context, model));
        if ((defaultCache != null) && !defaultCache.equals(CacheServiceNameFactory.DEFAULT_CACHE)) {
            for (CacheGroupAliasBuilderProvider provider : ServiceLoader.load(CacheGroupAliasBuilderProvider.class, CacheGroupAliasBuilderProvider.class.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(name, CacheServiceNameFactory.DEFAULT_CACHE, defaultCache)) {
                    context.removeService(builder.getServiceName());
                }
            }

            context.removeService(new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(name, CacheServiceNameFactory.DEFAULT_CACHE), CacheServiceName.CACHE.getServiceName(name), Cache.class).getServiceName());

            for (CacheServiceNameFactory nameFactory : CacheServiceName.values()) {
                context.removeService(nameFactory.getServiceName(name));
            }
        }

        context.removeService(InfinispanBindingFactory.createCacheContainerBinding(name).getBinderServiceName());

        for (CacheContainerServiceNameFactory factory : CacheContainerServiceName.values()) {
            context.removeService(factory.getServiceName(name));
        }
    }
}
