/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public abstract class TableResourceDefinition extends SimpleResourceDefinition implements Registration {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String value) {
        return PathElement.pathElement("table", value);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        BATCH_SIZE("batch-size", ModelType.INT, new ModelNode(100)),
        FETCH_SIZE("fetch-size", ModelType.INT, new ModelNode(100)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum ColumnAttribute implements org.jboss.as.clustering.controller.Attribute {
        ID("id-column", "id", "VARCHAR"),
        DATA("data-column", "datum", "BINARY"),
        TIMESTAMP("timestamp-column", "version", "BIGINT"),
        ;
        private final org.jboss.as.clustering.controller.Attribute name;
        private final org.jboss.as.clustering.controller.Attribute type;
        private final AttributeDefinition definition;

        ColumnAttribute(String name, String defaultName, String defaultType) {
            this.name = new SimpleAttribute(new SimpleAttributeDefinitionBuilder("name", ModelType.STRING)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(defaultName))
                    .build());
            this.type = new SimpleAttribute(new SimpleAttributeDefinitionBuilder("type", ModelType.STRING)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(defaultType))
                    .build());
            this.definition = ObjectTypeAttributeDefinition.Builder.of(name, this.name.getDefinition(), this.type.getDefinition())
                    .setAllowNull(true)
                    .setSuffix("column")
                    .build();
        }

        org.jboss.as.clustering.controller.Attribute getColumnName() {
            return this.name;
        }

        org.jboss.as.clustering.controller.Attribute getColumnType() {
            return this.type;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    TableResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(path, resolver);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);
        new ReloadRequiredWriteAttributeHandler(ColumnAttribute.class).register(registration);
    }
}
