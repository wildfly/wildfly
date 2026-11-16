/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

public abstract class VertxAttributes {

    public static final SimpleAttributeDefinition OPTION_NAME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_OPTION_NAME, ModelType.STRING)
        .setRequired(false)
        .setAllowExpression(true)
        .setRestartAllServices()
        .build();

    private static final List<AttributeDefinition> ATTRS = new ArrayList<>();
    static {
        ATTRS.add(OPTION_NAME);
    }

    static List<AttributeDefinition> getSimpleAttributes() {
        return ATTRS;
    }

}
