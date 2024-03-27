/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.dmr.ModelType;

import java.util.Collection;
import java.util.List;

/**
 * Global mime mapping config for file types
 *
 * @author Stuart Douglas
 */
class MimeMappingDefinition extends PersistentResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.MIME_MAPPING);
    protected static final SimpleAttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING, false)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();


    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(VALUE);

    MimeMappingDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(ReloadRequiredAddStepHandler.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
