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

package org.jboss.as.clustering.subsystem;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

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

    public AdditionalInitialization require(Requirement requirement) {
        this.requirements.add(requirement.getName());
        return this;
    }

    public AdditionalInitialization require(UnaryRequirement requirement, String... names) {
        Stream.of(names).forEach(name -> this.requirements.add(requirement.resolve(name)));
        return this;
    }

    public AdditionalInitialization require(BinaryRequirement requirement, String parent, String child) {
        this.requirements.add(requirement.resolve(parent, child));
        return this;
    }
}
