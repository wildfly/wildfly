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
package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Root resource definition for MicroProfile Open API subsystem
 * @author Michael Edgar
 */
public class MicroProfileOpenAPISubsystemDefinition extends SubsystemResourceDefinition<SubsystemRegistration> {

    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, MicroProfileOpenAPIExtension.SUBSYSTEM_NAME);

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        MICROPROFILE_OPENAPI("org.wildfly.microprofile.openapi"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name)
                    .addRequirements("org.wildfly.microprofile.config")
                    .build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    MicroProfileOpenAPISubsystemDefinition() {
        super(PATH, MicroProfileOpenAPIExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(this);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(Capability.class)
                ;
        MicroProfileOpenAPIServiceHandler handler = new MicroProfileOpenAPIServiceHandler();
        new DeploymentChainContributingResourceRegistration(descriptor, handler, handler).register(registration);
    }
}
