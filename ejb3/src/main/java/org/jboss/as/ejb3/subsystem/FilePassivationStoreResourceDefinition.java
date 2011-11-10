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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ejb3.cache.impl.factory.NonClusteredBackingCacheEntryStoreSource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class FilePassivationStoreResourceDefinition extends PassivationStoreResourceDefinition {

    public static final SimpleAttributeDefinition MAX_SIZE = MAX_SIZE_BUILDER.build();
    public static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.RELATIVE_TO, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.RELATIVE_TO.getLocalName())
                    .setDefaultValue(new ModelNode().set(NonClusteredBackingCacheEntryStoreSource.DEFAULT_RELATIVE_TO))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition GROUPS_PATH =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.GROUPS_PATH, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.GROUPS_PATH.getLocalName())
                    .setDefaultValue(new ModelNode().set(NonClusteredBackingCacheEntryStoreSource.DEFAULT_GROUP_DIRECTORY_NAME))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition SESSIONS_PATH =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.SESSIONS_PATH, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.SESSIONS_PATH.getLocalName())
                    .setDefaultValue(new ModelNode().set(NonClusteredBackingCacheEntryStoreSource.DEFAULT_SESSION_DIRECTORY_NAME))
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    public static final SimpleAttributeDefinition SUBDIRECTORY_COUNT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.SUBDIRECTORY_COUNT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.SUBDIRECTORY_COUNT.getLocalName())
                    .setDefaultValue(new ModelNode().set(NonClusteredBackingCacheEntryStoreSource.DEFAULT_SUBDIRECTORY_COUNT))
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, MAX_SIZE, RELATIVE_TO, GROUPS_PATH, SESSIONS_PATH, SUBDIRECTORY_COUNT };

    private static final FilePassivationStoreAdd ADD = new FilePassivationStoreAdd(ATTRIBUTES);
    private static final FilePassivationStoreRemove REMOVE = new FilePassivationStoreRemove(ADD);
    private static final FilePassivationStoreWriteHandler WRITE_HANDLER = new FilePassivationStoreWriteHandler(ATTRIBUTES);

    public static final FilePassivationStoreResourceDefinition INSTANCE = new FilePassivationStoreResourceDefinition();

    private FilePassivationStoreResourceDefinition() {
        super(EJB3SubsystemModel.FILE_PASSIVATION_STORE, ADD, REMOVE, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES, WRITE_HANDLER, ATTRIBUTES);
    }
}
