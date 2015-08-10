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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.SingletonServiceName;
import org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Capability;

/**
 * Builds a service that provides a {@link SingletonPolicy}.
 * @author Paul Ferraro
 */
public class SingletonPolicyBuilder implements ResourceServiceBuilder<SingletonPolicy>, SingletonPolicy {

    private final InjectedValue<SingletonServiceBuilderFactory> factory = new InjectedValue<>();
    private final InjectedValue<SingletonElectionPolicy> policy = new InjectedValue<>();

    private final String name;

    private volatile String containerName;
    private volatile String cacheName;
    private volatile int quorum;

    public SingletonPolicyBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return (this.name != null) ? Capability.POLICY.getDefinition().getCapabilityServiceName(this.name) : Capability.POLICY.getDefinition().getCapabilityServiceName();
    }

    @Override
    public ServiceBuilder<SingletonPolicy> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(new ImmediateValue<SingletonPolicy>(this)))
                .addDependency(SingletonServiceName.BUILDER.getServiceName(this.containerName, this.cacheName), SingletonServiceBuilderFactory.class, this.factory)
                .addDependency(new ElectionPolicyServiceNameProvider(this.name).getServiceName(), SingletonElectionPolicy.class, this.policy)
        ;
    }

    @Override
    public Builder<SingletonPolicy> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = SingletonPolicyResourceDefinition.Attribute.CACHE_CONTAINER.getDefinition().resolveModelAttribute(context, model).asString();
        this.cacheName = SingletonPolicyResourceDefinition.Attribute.CACHE.getDefinition().resolveModelAttribute(context, model).asString();
        this.quorum = SingletonPolicyResourceDefinition.Attribute.QUORUM.getDefinition().resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
        return this.factory.getValue().createSingletonServiceBuilder(name, service).electionPolicy(this.policy.getValue()).requireQuorum(this.quorum);
    }
}
