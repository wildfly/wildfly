/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.cmp.subsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * @author Stuart Douglas
 */
class HiLoKeyGeneratorResourceDefinition extends AbstractKeyGeneratorResourceDefinition {

    static final HiLoKeyGeneratorResourceDefinition INSTANCE = new HiLoKeyGeneratorResourceDefinition();

    static final SimpleAttributeDefinition BLOCK_SIZE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.BLOCK_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition CREATE_TABLE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.CREATE_TABLE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition CREATE_TABLE_DDL = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.CREATE_TABLE_DDL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.DATA_SOURCE, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition DROP_TABLE = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.DROP_TABLE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ID_COLUMN = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.ID_COLUMN, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SELECT_HI_DDL = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.SELECT_HI_DDL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SEQUENCE_COLUMN = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.SEQUENCE_COLUMN, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SEQUENCE_NAME = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.SEQUENCE_NAME, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition TABLE_NAME = new SimpleAttributeDefinitionBuilder(CmpSubsystemModel.TABLE_NAME, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES;
    static final Map<String, SimpleAttributeDefinition> ATTRIBUTE_MAP;

    static {
        List<SimpleAttributeDefinition> list = new ArrayList<SimpleAttributeDefinition>(COMMON_ATTRIBUTES.length + 10);
        Collections.addAll(list, COMMON_ATTRIBUTES);
        list.add(BLOCK_SIZE);
        list.add(CREATE_TABLE);
        list.add(CREATE_TABLE_DDL);
        list.add(DATA_SOURCE);
        list.add(DROP_TABLE);
        list.add(ID_COLUMN);
        list.add(SELECT_HI_DDL);
        list.add(SEQUENCE_COLUMN);
        list.add(SEQUENCE_NAME);
        list.add(TABLE_NAME);
        ATTRIBUTES = list.toArray(new SimpleAttributeDefinition[list.size()]);

        Map<String, SimpleAttributeDefinition> map = new LinkedHashMap<String, SimpleAttributeDefinition>();
        for(SimpleAttributeDefinition ad : ATTRIBUTES) {
            map.put(ad.getName(), ad);
        }
        ATTRIBUTE_MAP = Collections.unmodifiableMap(map);
    }

    private HiLoKeyGeneratorResourceDefinition() {
        super(CmpSubsystemModel.HILO_KEY_GENERATOR_PATH,
                CmpExtension.getResolver(CmpSubsystemModel.HILO_KEY_GENERATOR),
                HiLoKeyGeneratorAdd.INSTANCE, HiLoKeyGeneratorRemove.INSTANCE);
        setDeprecated(CmpExtension.DEPRECATED_SINCE);
    }

    @Override
    public Map<String, SimpleAttributeDefinition> getAttributeMap() {
        return ATTRIBUTE_MAP;
    }
}
