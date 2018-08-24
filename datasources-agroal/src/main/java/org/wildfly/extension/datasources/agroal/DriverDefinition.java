/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.datasources.agroal.AgroalExtension.getResolver;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Definition for the driver resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class DriverDefinition extends PersistentResourceDefinition {

    private static final String AGROAL_DRIVER_CAPABILITY_NAME = "org.wildfly.data-source.agroal-driver";

    static final RuntimeCapability<Void> AGROAL_DRIVER_CAPABILITY = RuntimeCapability.Builder.of(AGROAL_DRIVER_CAPABILITY_NAME, true, Class.class).build();

    static final String DRIVERS_ELEMENT_NAME = "drivers";

    static final SimpleAttributeDefinition MODULE_ATTRIBUTE = create("module", ModelType.STRING)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static final SimpleAttributeDefinition CLASS_ATTRIBUTE = create("class", ModelType.STRING)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static final PropertiesAttributeDefinition CLASS_INFO = new PropertiesAttributeDefinition.Builder("class-info", true)
            .setStorageRuntime()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = unmodifiableList(asList(MODULE_ATTRIBUTE, CLASS_ATTRIBUTE));

    static final DriverDefinition INSTANCE = new DriverDefinition();

    // --- //

    private DriverDefinition() {
        // TODO The cast to PersistentResourceDefinition.Parameters is a workaround to WFCORE-4040
        super((Parameters) new Parameters(pathElement("driver"), getResolver("driver"))
                .setCapabilities(AGROAL_DRIVER_CAPABILITY)
                .setAddHandler(DriverOperations.ADD_OPERATION)
                .setRemoveHandler(DriverOperations.REMOVE_OPERATION)
                .setAccessConstraints(new ApplicationTypeAccessConstraintDefinition(
                        new ApplicationTypeConfig(AgroalExtension.SUBSYSTEM_NAME, "driver"))));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(CLASS_INFO, DriverOperations.INFO_OPERATION);
    }
}
