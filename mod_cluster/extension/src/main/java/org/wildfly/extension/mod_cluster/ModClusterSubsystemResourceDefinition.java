/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * Resource definition for mod_cluster subsystem resource, children of which are respective proxy configurations.
 * Also registers wrong, legacy and deprecated proxy operations (WFLY-10439).
 *
 * @author Radoslav Husar
 */
class ModClusterSubsystemResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> {

    public static final PathElement PATH = pathElement(ModClusterExtension.SUBSYSTEM_NAME);

    ModClusterSubsystemResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(SubsystemRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(this);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());

        ResourceServiceHandler handler = new ModClusterSubsystemServiceHandler();
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        new ProxyConfigurationResourceDefinition().register(registration);

        // Deprecated legacy operations which are exposed at the wrong location
        if (parent.isRuntimeOnlyRegistrationValid()) {
            for (LegacyProxyOperation legacyProxyOperation : LegacyProxyOperation.values()) {
                registration.registerOperationHandler(legacyProxyOperation.getDefinition(), legacyProxyOperation);
            }
        }
    }

    static TransformationDescription buildTransformation(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        ProxyConfigurationResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }
}
