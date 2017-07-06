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

package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the databse data store resource.
 *
 */
public class DatabaseDataStoreResourceDefinition extends SimpleResourceDefinition {

    public static final DatabaseDataStoreResourceDefinition INSTANCE = new DatabaseDataStoreResourceDefinition();

    public static final SimpleAttributeDefinition DATASOURCE_JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DATASOURCE_JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setValidator(new ModelTypeValidator(ModelType.STRING, true, false))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
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
                    .setDefaultValue(new ModelNode(true))
                    .build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES ;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(DATASOURCE_JNDI_NAME.getName(), DATASOURCE_JNDI_NAME);
        map.put(DATABASE.getName(), DATABASE);
        map.put(PARTITION.getName(), PARTITION);
        map.put(REFRESH_INTERVAL.getName(), REFRESH_INTERVAL);
        map.put(ALLOW_EXECUTION.getName(), ALLOW_EXECUTION);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    private DatabaseDataStoreResourceDefinition() {
        super(EJB3SubsystemModel.DATABASE_DATA_STORE_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.DATABASE_DATA_STORE),
                DatabaseDataStoreAdd.INSTANCE,
                new ServiceRemoveStepHandler(TimerPersistence.SERVICE_NAME, DatabaseDataStoreAdd.INSTANCE));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

}
