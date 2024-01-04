/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Paul Ferraro
 */
public class AffinityResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("affinity", value);
    }

    private final Iterable<? extends Capability> capabilities;
    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceConfiguratorFactory factory;

    AffinityResourceDefinition(PathElement path, Iterable<? extends Capability> capabilities, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory factory) {
        super(path, DistributableWebExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, pathElement(PathElement.WILDCARD_VALUE)));
        this.capabilities = capabilities;
        this.configurator = configurator;
        this.factory = factory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver())).addCapabilities(this.capabilities);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this.factory);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);
        return registration;
    }
}
