/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the database data store resource.
 *
 */
public class DatabaseDataStoreResourceDefinition extends SimpleResourceDefinition {
    public static final String DATA_SOURCE_CAPABILITY_NAME = "org.wildfly.data-source";

    public static final SimpleAttributeDefinition DATASOURCE_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DATASOURCE_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setAlternatives(EJB3SubsystemModel.DATASOURCE_JNDI_NAME)
                    .setCapabilityReference(DATA_SOURCE_CAPABILITY_NAME, TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition DATASOURCE_JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DATASOURCE_JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setAlternatives(EJB3SubsystemModel.DATASOURCE_NAME)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDeprecated(ModelVersion.create(10))
                    .build();

    public static final SimpleAttributeDefinition DATABASE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DATABASE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition PARTITION =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PARTITION, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode("default"))
                    .setValidator(new StringLengthValidator(0))
                    .build();

    public static final SimpleAttributeDefinition REFRESH_INTERVAL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.REFRESH_INTERVAL, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(-1))
                    .build();


    public static final SimpleAttributeDefinition ALLOW_EXECUTION =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ALLOW_EXECUTION, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { DATASOURCE_JNDI_NAME, DATASOURCE_NAME,
            DATABASE, PARTITION, REFRESH_INTERVAL, ALLOW_EXECUTION };
    private static final DatabaseDataStoreAdd ADD_HANDLER = new DatabaseDataStoreAdd(ATTRIBUTES);

    DatabaseDataStoreResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.DATABASE_DATA_STORE))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(new ServiceRemoveStepHandler(TimerPersistence.SERVICE_NAME, ADD_HANDLER))
                .setCapabilities(TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

}
