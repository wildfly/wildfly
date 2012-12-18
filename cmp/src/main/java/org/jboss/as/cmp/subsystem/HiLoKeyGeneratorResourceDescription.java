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

package org.jboss.as.cmp.subsystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Stuart Douglas
 */
public class HiLoKeyGeneratorResourceDescription extends SimpleResourceDefinition {

    public static final HiLoKeyGeneratorResourceDescription INSTANCE = new HiLoKeyGeneratorResourceDescription();

    public static final SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.JNDI_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition BLOCK_SIZE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.BLOCK_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CREATE_TABLE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.CREATE_TABLE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CREATE_TABLE_DDL = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.CREATE_TABLE_DDL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.DATA_SOURCE, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition DROP_TABLE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.DROP_TABLE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ID_COLUMN = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.ID_COLUMN, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SELECT_HI_DDL = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.SELECT_HI_DDL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SEQUENCE_COLUMN = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.SEQUENCE_COLUMN, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SEQUENCE_NAME = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.SEQUENCE_NAME, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition TABLE_NAME = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.TABLE_NAME, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final Map<String, SimpleAttributeDefinition> ATTRIBUTE_MAP;
    public static final SimpleAttributeDefinition[] ATTRIBUTES = {JNDI_NAME, BLOCK_SIZE, CREATE_TABLE, CREATE_TABLE_DDL, DATA_SOURCE, DROP_TABLE, ID_COLUMN, SELECT_HI_DDL, SEQUENCE_COLUMN, SEQUENCE_NAME, TABLE_NAME};

    static {
        Map<String, SimpleAttributeDefinition> map = new LinkedHashMap<String, SimpleAttributeDefinition>();
        map.put(JNDI_NAME.getName(), JNDI_NAME);
        map.put(BLOCK_SIZE.getName(), BLOCK_SIZE);
        map.put(CREATE_TABLE.getName(), CREATE_TABLE);
        map.put(CREATE_TABLE_DDL.getName(), CREATE_TABLE_DDL);
        map.put(DATA_SOURCE.getName(), DATA_SOURCE);
        map.put(DROP_TABLE.getName(), DROP_TABLE);
        map.put(ID_COLUMN.getName(), ID_COLUMN);
        map.put(SELECT_HI_DDL.getName(), SELECT_HI_DDL);
        map.put(SEQUENCE_COLUMN.getName(), SEQUENCE_COLUMN);
        map.put(SEQUENCE_NAME.getName(), SEQUENCE_NAME);
        map.put(TABLE_NAME.getName(), TABLE_NAME);
        ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    private HiLoKeyGeneratorResourceDescription() {
        super(CmpSubsystemModel.HILO_KEY_GENERATOR_PATH,
                CmpExtension.getResourceDescriptionResolver(CmpSubsystemModel.HILO_KEY_GENERATOR),
                HiLoKeyGeneratorAdd.INSTANCE, HiLoKeyGeneratorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTE_MAP.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }
}
