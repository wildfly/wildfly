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
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.SingletonServiceName;

/**
 * Builds a service that provides a deployment policy.
 * @author Paul Ferraro
 */
public class DeploymentPolicyBuilder extends DeploymentPolicyServiceNameProvider implements ResourceServiceBuilder<DeploymentPolicy>, DeploymentPolicy {

    private final InjectedValue<SingletonServiceBuilderFactory> factory = new InjectedValue<>();
    private final InjectedValue<SingletonElectionPolicy> policy = new InjectedValue<>();

    private volatile String containerName;
    private volatile String cacheName;
    private volatile int quorum;

    public DeploymentPolicyBuilder(String name) {
        super(name);
    }

    @Override
    public ServiceBuilder<DeploymentPolicy> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(new ImmediateValue<DeploymentPolicy>(this)))
                .addDependency(SingletonServiceName.BUILDER.getServiceName(this.containerName, this.cacheName), SingletonServiceBuilderFactory.class, this.factory)
                .addDependency(new ElectionPolicyServiceNameProvider(this.getName()).getServiceName(), SingletonElectionPolicy.class, this.policy)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Builder<DeploymentPolicy> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = DeploymentPolicyResourceDefinition.Attribute.CACHE_CONTAINER.getDefinition().resolveModelAttribute(context, model).asString();
        this.cacheName = DeploymentPolicyResourceDefinition.Attribute.CACHE.getDefinition().resolveModelAttribute(context, model).asString();
        this.quorum = DeploymentPolicyResourceDefinition.Attribute.QUORUM.getDefinition().resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public SingletonServiceBuilderFactory getSingletonServiceBuilderFactory() {
        return this.factory.getValue();
    }

    @Override
    public SingletonElectionPolicy getElectionPolicy() {
        return this.policy.getValue();
    }

    @Override
    public int getQuorum() {
        return this.quorum;
    }
}
