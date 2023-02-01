/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
