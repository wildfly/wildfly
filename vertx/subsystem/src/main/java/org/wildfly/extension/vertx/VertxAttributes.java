package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

public abstract class VertxAttributes {

    public static final SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(VertxConstants.JNDI_NAME, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(false)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    public static final SimpleAttributeDefinition VERTX_OPTIONS_FILE = new SimpleAttributeDefinitionBuilder(VertxConstants.VERTX_OPTIONS_FILE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .build();

    private static final List<AttributeDefinition> ATTRS = new ArrayList<>();
    static {
        ATTRS.add(JNDI_NAME);
        ATTRS.add(VERTX_OPTIONS_FILE);
    }

    static List<AttributeDefinition> getSimpleAttributes() {
        return ATTRS;
    }

}
