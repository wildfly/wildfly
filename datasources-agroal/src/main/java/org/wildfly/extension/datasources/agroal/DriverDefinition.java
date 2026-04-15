/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
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

    static final PathElement PATH = pathElement("driver");
    static final RuntimeCapability<Void> AGROAL_DRIVER_CAPABILITY = RuntimeCapability.Builder.of(AGROAL_DRIVER_CAPABILITY_NAME, true, Class.class).build();

    static final String DRIVERS_ELEMENT_NAME = "drivers";

    static final SimpleAttributeDefinition MODULE_ATTRIBUTE = create("module", ModelType.STRING)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .setCorrector(ModuleIdentifierUtil.MODULE_NAME_CORRECTOR)
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

    // --- //

    DriverDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH, AgroalExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH))
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
