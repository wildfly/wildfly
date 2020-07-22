package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

public abstract class VertxAttributes {

    private static final String VERTX_PARAM_GROUP = "vertx";

    public static final SimpleAttributeDefinition EVENT_LOOP_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.EVENT_LOOP_POOL_SIZE, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(10000000))
            .setAttributeGroup(VERTX_PARAM_GROUP)
            .build();

    public static final SimpleAttributeDefinition HA_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.HA_ENABLED, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
            .setDefaultValue(new ModelNode(false))
            .setAttributeGroup(VERTX_PARAM_GROUP)
            .build();

    static List<AttributeDefinition> getSimpleAttributes() {
        List<AttributeDefinition> attrs = new ArrayList<>();
        attrs.add(EVENT_LOOP_POOL_SIZE);
        attrs.add(HA_ENABLED);
        return attrs;
    }

    static SimpleAttributeDefinition getSimpleAttribute(String name) {
        if (name.equals(VertxConstants.EVENT_LOOP_POOL_SIZE)) {
            return EVENT_LOOP_POOL_SIZE;
        } else if (name.equals(VertxConstants.HA_ENABLED)) {
            return HA_ENABLED;
        }
        return null;
    }

}
