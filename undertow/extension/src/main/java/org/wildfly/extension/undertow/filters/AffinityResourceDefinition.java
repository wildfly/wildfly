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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Base class for affinity resources.
 *
 * @author Radoslav Husar
 */
public abstract class AffinityResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    protected static PathElement pathElement(String value) {
        return PathElement.pathElement(Constants.AFFINITY, value);
    }
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private final UnaryOperator<ResourceDescriptor> configurator;

    AffinityResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, UndertowExtension.getResolver(Constants.FILTER, ModClusterDefinition.PATH_ELEMENT.getKey(), path.getKey(), path.getValue()));
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
