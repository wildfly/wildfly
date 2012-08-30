/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.connections.database;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DATABASE_CONNECTION;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a connection factory for an Database security store.
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement RESOURCE_PATH = PathElement.pathElement(DATABASE_CONNECTION);

    public static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DATA_SOURCE, ModelType.STRING, true)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true)).build();

    public static final SimpleAttributeDefinition DATABASE_URL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URL, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition DATABASE_DRIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DRIVER, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition DATABASE_MODULE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MODULE, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition DATABASE_USERNAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USERNAME, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition DATABASE_PASSWORD = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PASSWORD, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, true))
        .setAllowExpression(true)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition DATABASE_MAX_POOL_SIZE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MAX_POOL_SIZE, ModelType.INT, true)
        .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, false))
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition DATABASE_MIN_POOL_SIZE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MIN_POOL_SIZE, ModelType.INT, true)
        .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, false))
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {DATA_SOURCE, DATABASE_MODULE, DATABASE_DRIVE, DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, DATABASE_MAX_POOL_SIZE, DATABASE_MIN_POOL_SIZE};

    public static final DatabaseConnectionResourceDefinition INSTANCE = new DatabaseConnectionResourceDefinition();

    private DatabaseConnectionResourceDefinition() {
        super(RESOURCE_PATH, ManagementDescription.getResourceDescriptionResolver("core.management.database-connection"),
                DatabaseConnectionAddHandler.INSTANCE, DatabaseConnectionRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        DatabaseConnectionWriteAttributeHandler writeHandler = new DatabaseConnectionWriteAttributeHandler();
        writeHandler.registerAttributes(resourceRegistration);
    }
}
