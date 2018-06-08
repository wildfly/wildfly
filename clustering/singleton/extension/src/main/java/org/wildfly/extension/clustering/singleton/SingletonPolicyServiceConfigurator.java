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

import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.CACHE_CONTAINER;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.QUORUM;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Capability.POLICY;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a service that provides a {@link SingletonPolicy}.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class SingletonPolicyServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, SingletonPolicy {

    private final SupplierDependency<SingletonElectionPolicy> policy;

    private volatile SupplierDependency<SingletonServiceBuilderFactory> factory;
    private volatile int quorum;

    public SingletonPolicyServiceConfigurator(PathAddress address) {
        super(POLICY, address);
        this.policy = new ServiceSupplierDependency<>(ElectionPolicyResourceDefinition.Capability.ELECTION_POLICY.getServiceName(address.append(ElectionPolicyResourceDefinition.WILDCARD_PATH)));
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SingletonPolicy> policy = new CompositeDependency(this.policy, this.factory).register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(policy, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        String cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.factory = new ServiceSupplierDependency<>(ClusteringCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY.getServiceName(context, containerName, cacheName));
        this.quorum = QUORUM.resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public ServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
        return this.factory.get().createSingletonServiceConfigurator(name)
                .electionPolicy(this.policy.get())
                .requireQuorum(this.quorum)
                ;
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, org.jboss.msc.service.Service<T> service) {
        return this.factory.get().createSingletonServiceBuilder(name, service)
                .electionPolicy(this.policy.get())
                .requireQuorum(this.quorum)
                ;
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, org.jboss.msc.service.Service<T> primaryService, org.jboss.msc.service.Service<T> backupService) {
        return this.factory.get().createSingletonServiceBuilder(name, primaryService, backupService)
                .electionPolicy(this.policy.get())
                .requireQuorum(this.quorum)
                ;
    }

    @Override
    public String toString() {
        return this.getServiceName().getSimpleName();
    }
}
