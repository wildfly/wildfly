/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.management.JndiViewOperation;
import org.jboss.as.naming.service.NamingService;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the Naming subsystem's root management resource.
 *
 * @author Stuart Douglas
 */
public class NamingSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    enum Capability {
        NAMING_STORE(NamingService.CAPABILITY_NAME, NamingStore.class),
        ;
        private final RuntimeCapability<?> definition;

        Capability(String name, Class<?> type) {
            this.definition = RuntimeCapability.Builder.of(name, type).build();
        }

        RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    static final SimpleOperationDefinition JNDI_VIEW = new SimpleOperationDefinitionBuilder(JndiViewOperation.OPERATION_NAME, NamingExtension.getResourceDescriptionResolver(NamingExtension.SUBSYSTEM_NAME))
            .addAccessConstraint(NamingExtension.JNDI_VIEW_CONSTRAINT)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .build();

    NamingSubsystemRootResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME),
                NamingExtension.getResourceDescriptionResolver(NamingExtension.SUBSYSTEM_NAME),
                new NamingSubsystemAdd(), new NamingSubsystemRemove());
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration registration) {
        for (Capability capability : EnumSet.allOf(Capability.class)) {
            RuntimeCapability<?> definition = capability.getDefinition();
            registration.registerCapability(definition);
        }
    }
}
