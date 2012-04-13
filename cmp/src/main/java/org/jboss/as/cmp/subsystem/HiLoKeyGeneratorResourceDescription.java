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
    public static final SimpleAttributeDefinition[] ATTRIBUTES = {BLOCK_SIZE, CREATE_TABLE, CREATE_TABLE_DDL, DATA_SOURCE, DROP_TABLE, ID_COLUMN, SELECT_HI_DDL, SEQUENCE_COLUMN, SEQUENCE_NAME, TABLE_NAME};

    static {
        Map<String, SimpleAttributeDefinition> map = new LinkedHashMap<String, SimpleAttributeDefinition>();
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
