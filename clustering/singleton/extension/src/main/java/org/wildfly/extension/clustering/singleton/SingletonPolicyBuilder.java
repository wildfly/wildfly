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

import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.*;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Capability.*;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a service that provides a {@link SingletonPolicy}.
 * @author Paul Ferraro
 */
public class SingletonPolicyBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<SingletonPolicy>, SingletonPolicy {

    private final ValueDependency<SingletonElectionPolicy> policy;

    private volatile ValueDependency<SingletonServiceBuilderFactory> factory;
    private volatile int quorum;

    public SingletonPolicyBuilder(PathAddress address) {
        super(POLICY, address);
        this.policy = new InjectedValueDependency<>(ElectionPolicyResourceDefinition.Capability.ELECTION_POLICY.getServiceName(address.append(ElectionPolicyResourceDefinition.WILDCARD_PATH)), SingletonElectionPolicy.class);
    }

    @Override
    public ServiceBuilder<SingletonPolicy> build(ServiceTarget target) {
        Service<SingletonPolicy> service = new ValueService<>(new ImmediateValue<>(this));
        ServiceBuilder<SingletonPolicy> builder = target.addService(this.getServiceName(), service).setInitialMode(ServiceController.Mode.PASSIVE);
        return new CompositeDependency(this.policy, this.factory).register(builder);
    }

    @Override
    public Builder<SingletonPolicy> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        String cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.factory = new InjectedValueDependency<>(ClusteringCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY.getServiceName(context, containerName, cacheName), SingletonServiceBuilderFactory.class);
        this.quorum = QUORUM.resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
        return this.factory.getValue().createSingletonServiceBuilder(name, service)
                .electionPolicy(this.policy.getValue())
                .requireQuorum(this.quorum)
                ;
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService) {
        return this.factory.getValue().createSingletonServiceBuilder(name, primaryService, backupService)
                .electionPolicy(this.policy.getValue())
                .requireQuorum(this.quorum)
                ;
    }

    @Override
    public String toString() {
        return this.getServiceName().getSimpleName();
    }
}
