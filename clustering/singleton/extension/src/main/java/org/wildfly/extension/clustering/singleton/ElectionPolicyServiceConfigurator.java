/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.ElectionPolicyResourceDefinition.Attribute.NAME_PREFERENCES;
import static org.wildfly.extension.clustering.singleton.ElectionPolicyResourceDefinition.Attribute.SOCKET_BINDING_PREFERENCES;
import static org.wildfly.extension.clustering.singleton.ElectionPolicyResourceDefinition.Capability.ELECTION_POLICY;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.NamePreference;
import org.wildfly.clustering.singleton.election.Preference;
import org.wildfly.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.wildfly.extension.clustering.singleton.election.OutboundSocketBindingPreference;

/**
 * Builds a service that provides an election policy.
 * @author Paul Ferraro
 */
public abstract class ElectionPolicyServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<SingletonElectionPolicy>, UnaryOperator<SingletonElectionPolicy> {

    private volatile List<Preference> preferences;
    private volatile List<Dependency> dependencies;

    protected ElectionPolicyServiceConfigurator(PathAddress address) {
        super(ELECTION_POLICY, address);
    }

    @Override
    public SingletonElectionPolicy apply(SingletonElectionPolicy policy) {
        return this.preferences.isEmpty() ? policy : new PreferredSingletonElectionPolicy(policy, this.preferences);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SingletonElectionPolicy> policy = builder.provides(this.getServiceName());
        for (Dependency dependency : this.dependencies) {
            dependency.register(builder);
        }
        Service service = new FunctionalService<>(policy, this, this);
        return builder.setInstance(service);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<ModelNode> socketBindingPreferences = SOCKET_BINDING_PREFERENCES.resolveModelAttribute(context, model).asListOrEmpty();
        List<ModelNode> namePreferences = NAME_PREFERENCES.resolveModelAttribute(context, model).asListOrEmpty();
        List<Preference> preferences = new ArrayList<>(socketBindingPreferences.size() + namePreferences.size());
        List<Dependency> dependencies = new ArrayList<>(socketBindingPreferences.size());
        for (ModelNode preference : socketBindingPreferences) {
            SupplierDependency<OutboundSocketBinding> binding = new ServiceSupplierDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, preference.asString()));
            preferences.add(new OutboundSocketBindingPreference(binding));
            dependencies.add(binding);
        }
        for (ModelNode preference : namePreferences) {
            preferences.add(new NamePreference(preference.asString()));
        }
        this.dependencies = dependencies;
        this.preferences = preferences;
        return this;
    }
}
