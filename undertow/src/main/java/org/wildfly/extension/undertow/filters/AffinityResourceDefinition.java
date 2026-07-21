/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.RestartParentResourceRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.undertow.Constants;

/**
 * Base class for affinity resources.
 *
 * @author Radoslav Husar
 */
public abstract class AffinityResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    protected static PathElement pathElement(String value) {
        return PathElement.pathElement(Constants.AFFINITY, value);
    }
    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private final UnaryOperator<ResourceDescriptor> configurator;

    AffinityResourceDefinition(ResourceRegistration registration, UnaryOperator<ResourceDescriptor> configurator) {
        super(new Parameters(registration, ModClusterDefinition.RESOLVER.createChildResolver(registration.getPathElement())));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()));
        new RestartParentResourceRegistrar(ModClusterServiceConfigurator::new, descriptor).register(registration);

        return registration;
    }
}
