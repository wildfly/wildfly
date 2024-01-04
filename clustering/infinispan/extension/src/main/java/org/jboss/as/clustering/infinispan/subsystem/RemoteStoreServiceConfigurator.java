/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        builder.segmented(false)
                .remoteCacheName(this.remoteCacheName)
                .socketTimeout(this.socketTimeout)
                .tcpNoDelay(this.tcpNoDelay)
                ;
        for (Supplier<OutboundSocketBinding> bindingDependency : this.bindings) {
            OutboundSocketBinding binding = bindingDependency.get();
            builder.addServer().host(binding.getUnresolvedDestinationAddress()).port(binding.getDestinationPort());
        }
    }
}
