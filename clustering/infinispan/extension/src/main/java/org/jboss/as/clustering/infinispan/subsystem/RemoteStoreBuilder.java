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

import static org.jboss.as.clustering.infinispan.subsystem.RemoteStoreResourceDefinition.Attribute.*;

import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class RemoteStoreBuilder extends StoreBuilder {

    private final List<ValueDependency<OutboundSocketBinding>> bindings = new LinkedList<>();

    private volatile RemoteStoreConfigurationBuilder storeBuilder;

    public RemoteStoreBuilder(PathAddress cacheAddress) {
        super(cacheAddress);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        ServiceBuilder<PersistenceConfiguration> builder = super.build(target);
        for (Dependency dependency : this.bindings) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public PersistenceConfiguration getValue() {
        for (Value<OutboundSocketBinding> value : this.bindings) {
            OutboundSocketBinding binding = value.getValue();
            try {
                this.storeBuilder.addServer().host(binding.getResolvedDestinationAddress().getHostAddress()).port(binding.getDestinationPort());
            } catch (UnknownHostException e) {
                throw InfinispanLogger.ROOT_LOGGER.failedToInjectSocketBinding(e, binding);
            }
        }
        return super.getValue();
    }

    @Override
    StoreConfigurationBuilder<?, ?> createStore(OperationContext context, ModelNode model) throws OperationFailedException {
        this.storeBuilder = new ConfigurationBuilder().persistence().addStore(RemoteStoreConfigurationBuilder.class)
                .remoteCacheName(CACHE.resolveModelAttribute(context, model).asString())
                .socketTimeout(SOCKET_TIMEOUT.resolveModelAttribute(context, model).asLong())
                .tcpNoDelay(TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean())
        ;
        for (String binding : StringListAttributeDefinition.unwrapValue(context, SOCKET_BINDINGS.resolveModelAttribute(context, model))) {
            this.bindings.add(new InjectedValueDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, binding), OutboundSocketBinding.class));
        }
        return this.storeBuilder;
    }
}
