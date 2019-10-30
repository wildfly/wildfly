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

import static org.jboss.as.clustering.infinispan.subsystem.RemoteStoreResourceDefinition.Attribute.CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.RemoteStoreResourceDefinition.Attribute.SOCKET_BINDINGS;
import static org.jboss.as.clustering.infinispan.subsystem.RemoteStoreResourceDefinition.Attribute.SOCKET_TIMEOUT;
import static org.jboss.as.clustering.infinispan.subsystem.RemoteStoreResourceDefinition.Attribute.TCP_NO_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class RemoteStoreServiceConfigurator extends StoreServiceConfigurator<RemoteStoreConfiguration, RemoteStoreConfigurationBuilder> {

    private volatile List<SupplierDependency<OutboundSocketBinding>> bindings;
    private volatile String remoteCacheName;
    private volatile long socketTimeout;
    private volatile boolean tcpNoDelay;

    public RemoteStoreServiceConfigurator(PathAddress address) {
        super(address, RemoteStoreConfigurationBuilder.class);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        for (Dependency dependency : this.bindings) {
            dependency.register(builder);
        }
        return super.register(builder);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.remoteCacheName = CACHE.resolveModelAttribute(context, model).asString();
        this.socketTimeout = SOCKET_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.tcpNoDelay = TCP_NO_DELAY.resolveModelAttribute(context, model).asBoolean();
        List<String> bindings = StringListAttributeDefinition.unwrapValue(context, SOCKET_BINDINGS.resolveModelAttribute(context, model));
        this.bindings = new ArrayList<>(bindings.size());
        for (String binding : bindings) {
            this.bindings.add(new ServiceSupplierDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, binding)));
        }
        return super.configure(context, model);
    }

    @Override
    public void accept(RemoteStoreConfigurationBuilder builder) {
        builder.remoteCacheName(this.remoteCacheName)
                .socketTimeout(this.socketTimeout)
                .tcpNoDelay(this.tcpNoDelay)
                ;
        for (Supplier<OutboundSocketBinding> bindingDependency : this.bindings) {
            OutboundSocketBinding binding = bindingDependency.get();
            builder.addServer().host(binding.getUnresolvedDestinationAddress()).port(binding.getDestinationPort());
        }
    }
}
