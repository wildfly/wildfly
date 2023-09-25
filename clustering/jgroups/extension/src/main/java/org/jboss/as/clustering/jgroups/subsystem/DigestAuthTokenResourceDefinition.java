/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class DigestAuthTokenResourceDefinition extends AuthTokenResourceDefinition<BinaryAuthToken> {

    static final PathElement PATH = pathElement("digest");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        ALGORITHM("algorithm", ModelType.STRING, new ModelNode("SHA-256")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    DigestAuthTokenResourceDefinition() {
        super(PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), DigestAuthTokenServiceConfigurator::new);
    }
}
