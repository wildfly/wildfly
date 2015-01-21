/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsBindingFactory;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceNameFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ChannelBuilder;
import org.wildfly.clustering.jgroups.spi.service.ChannelConnectorBuilder;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceNameFactory;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;
import org.wildfly.clustering.spi.ClusteredGroupBuilderProvider;
import org.wildfly.clustering.spi.ClusteredCacheGroupBuilderProvider;
import org.wildfly.clustering.spi.GroupBuilderProvider;
import org.wildfly.clustering.spi.LocalCacheGroupBuilderProvider;
import org.wildfly.clustering.spi.LocalGroupBuilderProvider;

/**
 * Add operation handler for /subsystem=infinispan/cache-container=*
 * @author Paul Ferraro
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class CacheContainerAddHandler extends AbstractAddStepHandler {

    CacheContainerAddHandler() {
        super(CacheContainerResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // Because we use child resources in a read-only manner to configure the cache container, replace the local model with the full model
        installRuntimeServices(context, operation, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
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

        // pick up the attribute values from the model
        // make default cache non required (AS7-3488)
        String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, model));
        String jndiName = ModelNodes.asString(CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, model));
        ServiceController.Mode initialMode = StartMode.valueOf(CacheContainerResourceDefinition.START.resolveModelAttribute(context, model).asString()).getMode();
        ModuleIdentifier module = ModelNodes.asModuleIdentifier(CacheContainerResourceDefinition.MODULE.resolveModelAttribute(context, model));

        CacheContainerConfigurationBuilder configBuilder = new CacheContainerConfigurationBuilder(name)
                .setModule(module)
                .setStatisticsEnabled(CacheContainerResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean())
                .setListenerExecutor(ModelNodes.asString(CacheContainerResourceDefinition.LISTENER_EXECUTOR.resolveModelAttribute(context, model)))
                .setEvictionExecutor(ModelNodes.asString(CacheContainerResourceDefinition.EVICTION_EXECUTOR.resolveModelAttribute(context, model)))
                .setReplicationQueueExecutor(ModelNodes.asString(CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.resolveModelAttribute(context, model)));

        if (model.hasDefined(TransportResourceDefinition.PATH.getKey())) {
            ModelNode transport = model.get(TransportResourceDefinition.PATH.getKeyValuePair());
            String channel = ModelNodes.asString(TransportResourceDefinition.CHANNEL.resolveModelAttribute(context, transport), ChannelServiceNameFactory.DEFAULT_CHANNEL);

            configBuilder.setTransport()
                    .setLockTimeout(TransportResourceDefinition.LOCK_TIMEOUT.resolveModelAttribute(context, transport).asLong(), TimeUnit.MILLISECONDS)
                    .setExecutor(ModelNodes.asString(TransportResourceDefinition.EXECUTOR.resolveModelAttribute(context, transport)))
                    .build(target).install();

            if (!name.equals(channel)) {
                new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), ChannelServiceName.CHANNEL.getServiceName(name), Channel.class).build(target).install();

                new ChannelBuilder(name).build(target).install();
                new ChannelConnectorBuilder(name).build(target).install();
                new AliasServiceBuilder<>(ChannelServiceName.FACTORY.getServiceName(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(channel), ChannelFactory.class).build(target).install();

                for (GroupBuilderProvider provider : ServiceLoader.load(ClusteredGroupBuilderProvider.class, ClusteredGroupBuilderProvider.class.getClassLoader())) {
                    Iterator<Builder<?>> builders = provider.getBuilders(channel, module).iterator();
                    for (Builder<?> builder : provider.getBuilders(name, module)) {
                        new AliasServiceBuilder<>(builder.getServiceName(), builders.next().getServiceName(), Object.class).build(target).install();
                    }
                }
            }
        } else {
            for (GroupBuilderProvider provider : ServiceLoader.load(LocalGroupBuilderProvider.class, LocalGroupBuilderProvider.class.getClassLoader())) {
                Iterator<Builder<?>> builders = provider.getBuilders(LocalGroupBuilderProvider.LOCAL, module).iterator();
                for (Builder<?> builder : provider.getBuilders(name, module)) {
                    new AliasServiceBuilder<>(builder.getServiceName(), builders.next().getServiceName(), Object.class).build(target).install();
                }
            }
        }

        // Install cache container configuration service
        configBuilder.build(target).install();

        // Install cache container service
        CacheContainerBuilder containerBuilder = new CacheContainerBuilder(name, defaultCache);

        if (model.hasDefined(CacheContainerResourceDefinition.ALIASES.getName())) {
            for (ModelNode alias : operation.get(CacheContainerResourceDefinition.ALIASES.getName()).asList()) {
                containerBuilder.addAlias(alias.asString());
            }
        }

        containerBuilder.build(target).setInitialMode(initialMode).install();

        // Install cache container jndi binding
        ContextNames.BindInfo binding = InfinispanBindingFactory.createCacheContainerBinding(name);
        BinderServiceBuilder<CacheContainer> bindingBuilder = new BinderServiceBuilder<>(binding, CacheContainerServiceName.CACHE_CONTAINER.getServiceName(name), CacheContainer.class);
        if (jndiName != null) {
            bindingBuilder.alias(ContextNames.bindInfoFor(JndiNameFactory.parse(jndiName).getAbsoluteName()));
        }
        bindingBuilder.build(target).install();

        // Install key affinity service factory
        new KeyAffinityServiceFactoryBuilder(name).build(target).install();

        if ((defaultCache != null) && !defaultCache.equals(CacheServiceNameFactory.DEFAULT_CACHE)) {

            for (CacheServiceNameFactory nameFactory : CacheServiceName.values()) {
                new AliasServiceBuilder<>(nameFactory.getServiceName(name), nameFactory.getServiceName(name, defaultCache), Object.class).build(target).install();
            }

            new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(name, CacheServiceNameFactory.DEFAULT_CACHE), CacheServiceName.CACHE.getServiceName(name), Cache.class).build(target).install();

            Class<? extends CacheGroupBuilderProvider> providerClass = model.hasDefined(TransportResourceDefinition.PATH.getKey()) ? ClusteredCacheGroupBuilderProvider.class : LocalCacheGroupBuilderProvider.class;
            for (CacheGroupBuilderProvider provider : ServiceLoader.load(providerClass, providerClass.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(name, CacheServiceNameFactory.DEFAULT_CACHE)) {
                    builder.build(target).install();
                }
            }
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        // remove the BinderService entry
        context.removeService(InfinispanBindingFactory.createCacheContainerBinding(name).getBinderServiceName());

        for (CacheContainerServiceNameFactory factory : CacheContainerServiceName.values()) {
            context.removeService(factory.getServiceName(name));
        }

        if (model.hasDefined(TransportResourceDefinition.PATH.getKey())) {
            removeServices(context, ClusteredGroupBuilderProvider.class, name);

            context.removeService(new TransportConfigurationBuilder(name).getServiceName());

            context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
            for (ChannelServiceNameFactory factory : ChannelServiceName.values()) {
                context.removeService(factory.getServiceName(name));
            }
        } else {
            removeServices(context, LocalGroupBuilderProvider.class, name);
        }

        String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, model));

        if ((defaultCache != null) && !defaultCache.equals(CacheServiceNameFactory.DEFAULT_CACHE)) {
            Class<? extends CacheGroupBuilderProvider> providerClass = model.hasDefined(TransportResourceDefinition.PATH.getKey()) ? ClusteredCacheGroupBuilderProvider.class : LocalCacheGroupBuilderProvider.class;
            for (CacheGroupBuilderProvider provider : ServiceLoader.load(providerClass, providerClass.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(name, CacheServiceNameFactory.DEFAULT_CACHE)) {
                    context.removeService(builder.getServiceName());
                }
            }
        }
    }

    private static <I extends GroupBuilderProvider> void removeServices(OperationContext context, Class<I> providerClass, String group) {
        for (I provider: ServiceLoader.load(providerClass, providerClass.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(group, null)) {
                context.removeService(builder.getServiceName());
            }
        }
    }
}
