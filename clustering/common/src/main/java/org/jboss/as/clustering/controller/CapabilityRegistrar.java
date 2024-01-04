/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Registration facility for capabilities.
 * @author Paul Ferraro
 */
public class CapabilityRegistrar implements ManagementRegistrar<ManagementResourceRegistration> {

    private final Collection<? extends Capability> capabilities;

    public <E extends Enum<E> & Capability> CapabilityRegistrar(Class<E> capabilityClass) {
        this(EnumSet.allOf(capabilityClass));
    }

    public CapabilityRegistrar(Capability... capabilities) {
        this.capabilities = Arrays.asList(capabilities);
    }

    public CapabilityRegistrar(Collection<? extends Capability> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (Capability capability : this.capabilities) {
            registration.registerCapability(capability.getDefinition());
        }
    }
}
