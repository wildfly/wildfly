/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>  2014 Red Hat Inc.
 * @author Paul Ferraro
 */
class SingleSignOnDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.SINGLE_SIGN_ON);
    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DOMAIN(Constants.DOMAIN, ModelType.STRING, null),
        PATH("path", ModelType.STRING, new ModelNode("/")),
        HTTP_ONLY("http-only", ModelType.BOOLEAN, ModelNode.FALSE),
        SECURE("secure", ModelType.BOOLEAN, ModelNode.FALSE),
        COOKIE_NAME("cookie-name", ModelType.STRING, new ModelNode("JSESSIONIDSSO")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SingleSignOnDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getValue())));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        // Attributes will be registered by the parent implementation
        return Collections.emptyList();
    }
}
