/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;

/**
 * Description of a relay resource.
 * @author Paul Ferraro
 */
public enum RelayResourceDescription implements ProtocolChildResourceDescription {
    INSTANCE;

    enum Attribute implements AttributeDefinitionProvider {
        SITE("site", ModelType.STRING),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = pathElement(RelayConfiguration.PROTOCOL_NAME);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("relay", name);
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public PathElement getPathKey() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(EnumSet.allOf(Attribute.class).stream().map(AttributeDefinitionProvider::get), ProtocolChildResourceDescription.super.getAttributes());
    }
}
