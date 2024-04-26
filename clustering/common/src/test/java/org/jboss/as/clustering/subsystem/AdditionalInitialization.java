/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.subsystem;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * {@link AdditionalInitialization} extension that simplifies setup of required capabilities.
 * @author Paul Ferraro
 */
public class AdditionalInitialization extends org.jboss.as.subsystem.test.AdditionalInitialization implements Serializable {
    private static final long serialVersionUID = 7496922674294804719L;

    private final RunningMode mode;
    private final List<String> requirements = new LinkedList<>();

    public AdditionalInitialization() {
        this(RunningMode.ADMIN_ONLY);
    }

    public AdditionalInitialization(RunningMode mode) {
        this.mode = mode;
    }

    @Override
    protected RunningMode getRunningMode() {
        return this.mode;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        registerCapabilities(capabilityRegistry, this.requirements.stream().toArray(String[]::new));
    }

    public AdditionalInitialization require(String requirement) {
        this.requirements.add(requirement);
        return this;
    }

    @Deprecated(forRemoval = true)
    public AdditionalInitialization require(Requirement requirement) {
        this.requirements.add(requirement.getName());
        return this;
    }

    @Deprecated(forRemoval = true)
    public AdditionalInitialization require(UnaryRequirement requirement, String... names) {
        Stream.of(names).forEach(name -> this.requirements.add(requirement.resolve(name)));
        return this;
    }

    @Deprecated(forRemoval = true)
    public AdditionalInitialization require(BinaryRequirement requirement, String parent, String child) {
        this.requirements.add(requirement.resolve(parent, child));
        return this;
    }

    public AdditionalInitialization require(NullaryServiceDescriptor<?> requirement) {
        this.requirements.add(requirement.getName());
        return this;
    }

    public AdditionalInitialization require(UnaryServiceDescriptor<?> requirement, String name) {
        this.requirements.add(RuntimeCapability.resolveCapabilityName(requirement, name));
        return this;
    }

    public AdditionalInitialization require(BinaryServiceDescriptor<?> requirement, String parent, String child) {
        this.requirements.add(RuntimeCapability.resolveCapabilityName(requirement, parent, child));
        return this;
    }
}
