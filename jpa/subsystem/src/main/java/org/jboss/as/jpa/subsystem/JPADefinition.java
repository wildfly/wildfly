/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Jakarta Persistence ResourceDefinition
 *
 */
public class JPADefinition extends SimpleResourceDefinition {

    // This a private capability. Its runtime API or capability service tyoe are subject to change.
    // See WFLY-7521
    // Currently it exists to indicate a model dependency from the Jakarta Persistence subsystem to the transactions subsystem
    private static final RuntimeCapability<Void> JPA_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jpa")
            .addRequirements(JPAServiceNames.LOCAL_TRANSACTION_PROVIDER_CAPABILITY)
            .build();

    protected static final SimpleAttributeDefinition DEFAULT_DATASOURCE =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_DATASOURCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(CommonAttributes.DEFAULT_DATASOURCE)
                    .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(EnumValidator.create(ExtendedPersistenceInheritance.class))
                    .setDefaultValue(new ModelNode(ExtendedPersistenceInheritance.DEEP.toString()))
                    .build();

    JPADefinition() {
        super(getParameters());
    }

    private static Parameters getParameters() {
        Parameters result = new Parameters(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME),
                JPAExtension.getResourceDescriptionResolver())
                .setFeature(true)
                .setCapabilities(JPA_CAPABILITY)
                .setAddHandler(JPASubSystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE);
        return result;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadWriteAttribute(DEFAULT_DATASOURCE, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_DATASOURCE));
        registration.registerReadWriteAttribute(DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE));
    }

    @Override
    public void registerAdditionalRuntimePackages(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                // Only if annotation is in use.
                RuntimePackageDependency.optional("org.hibernate.search.mapper.orm"),
                RuntimePackageDependency.required("org.hibernate"),
                // An alias to org.hibernate module.
                RuntimePackageDependency.optional("org.hibernate.envers"));
    }
}
