/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Copy of {@link org.jboss.as.clustering.subsystem.AdditionalInitialization} to workaround classloading issues on the legacy controller.
 *
 * @author Radoslav Husar
 */
class LegacyControllerAdditionalInitialization extends AdditionalInitialization implements Serializable {
    private static final long serialVersionUID = -2920965574657336841L;

    private final List<String> requirements = new LinkedList<>();

    @Override
    protected RunningMode getRunningMode() {
        return RunningMode.ADMIN_ONLY;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource root, ManagementResourceRegistration registration, RuntimeCapabilityRegistry capabilityRegistry) {
        registerCapabilities(capabilityRegistry, this.requirements.stream().toArray(String[]::new));
    }

    public LegacyControllerAdditionalInitialization require(Requirement requirement) {
        this.requirements.add(requirement.getName());
        return this;
    }

    public LegacyControllerAdditionalInitialization require(UnaryRequirement requirement, String... names) {
        Stream.of(names).forEach(name -> this.requirements.add(requirement.resolve(name)));
        return this;
    }
}
