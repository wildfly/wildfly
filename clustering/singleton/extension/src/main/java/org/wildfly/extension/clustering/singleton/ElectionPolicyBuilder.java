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

import static org.wildfly.extension.clustering.singleton.ElectionPolicyResourceDefinition.Attribute.*;
import static org.wildfly.extension.clustering.singleton.ElectionPolicyResourceDefinition.Capability.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.NamePreference;
import org.wildfly.clustering.singleton.election.Preference;
import org.wildfly.clustering.singleton.election.PreferredSingletonElectionPolicy;
import org.wildfly.extension.clustering.singleton.election.OutboundSocketBindingPreference;

/**
 * Builds a service that provides an election policy.
 * @author Paul Ferraro
 */
public abstract class ElectionPolicyBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<SingletonElectionPolicy>, Value<SingletonElectionPolicy> {

    private final List<Preference> preferences = new CopyOnWriteArrayList<>();
    private final List<Dependency> dependencies = new CopyOnWriteArrayList<>();

    protected ElectionPolicyBuilder(PathAddress address) {
        super(ELECTION_POLICY, address);
    }

    @Override
    public ServiceBuilder<SingletonElectionPolicy> build(ServiceTarget target) {
        Value<SingletonElectionPolicy> value = () -> this.preferences.isEmpty() ? this.getValue() : new PreferredSingletonElectionPolicy(this.getValue(), this.preferences);
        ServiceBuilder<SingletonElectionPolicy> builder = target.addService(this.getServiceName(), new ValueService<>(value));
        for (Dependency dependency : this.dependencies) {
            dependency.register(builder);
        }
        return builder;
    }

    @Override
    public Builder<SingletonElectionPolicy> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.preferences.clear();
        this.dependencies.clear();
        for (ModelNode preference : ModelNodes.optionalList(SOCKET_BINDING_PREFERENCES.resolveModelAttribute(context, model)).orElse(Collections.emptyList())) {
            InjectedValueDependency<OutboundSocketBinding> binding = new InjectedValueDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, preference.asString()), OutboundSocketBinding.class);
            this.preferences.add(new OutboundSocketBindingPreference(binding));
            this.dependencies.add(binding);
        }
        for (ModelNode preference : ModelNodes.optionalList(NAME_PREFERENCES.resolveModelAttribute(context, model)).orElse(Collections.emptyList())) {
            this.preferences.add(new NamePreference(preference.asString()));
        }
        return this;
    }
}
