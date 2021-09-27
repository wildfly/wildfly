/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=mixed-jdbc
 * /subsystem=infinispan/cache-container=X/cache=Y/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class MixedKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("mixed-keyed-jdbc-store", "MIXED_KEYED_JDBC_STORE");
    static final PathElement PATH = pathElement("mixed-jdbc");

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        BINARY_TABLE(BinaryKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE),
        STRING_TABLE(StringKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(org.jboss.as.clustering.controller.Attribute attribute) {
            this.definition = attribute.getDefinition();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addExtraParameters(DeprecatedAttribute.class)
                    .addRequiredChildren(BinaryTableResourceDefinition.PATH, StringTableResourceDefinition.PATH)
                    // Translate deprecated BINARY_TABLE and STRING_TABLE attributes into separate add table operation
                    .setAddOperationTransformation(new TableAttributeTransformation(DeprecatedAttribute.BINARY_TABLE, BinaryTableResourceDefinition.PATH).andThen(new TableAttributeTransformation(DeprecatedAttribute.STRING_TABLE, StringTableResourceDefinition.PATH)));
        }
    }

    MixedKeyedJDBCStoreResourceDefinition() {
        super(PATH, LEGACY_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, JDBCStoreResourceDefinition.PATH, WILDCARD_PATH), new ResourceDescriptorConfigurator());
        this.setDeprecated(InfinispanModel.VERSION_5_0_0.getVersion());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        registration.registerReadWriteAttribute(DeprecatedAttribute.BINARY_TABLE.getDefinition(), BinaryKeyedJDBCStoreResourceDefinition.LEGACY_READ_TABLE_HANDLER, BinaryKeyedJDBCStoreResourceDefinition.LEGACY_WRITE_TABLE_HANDLER);
        registration.registerReadWriteAttribute(DeprecatedAttribute.STRING_TABLE.getDefinition(), StringKeyedJDBCStoreResourceDefinition.LEGACY_READ_TABLE_HANDLER, StringKeyedJDBCStoreResourceDefinition.LEGACY_WRITE_TABLE_HANDLER);

        new BinaryTableResourceDefinition().register(registration);
        new StringTableResourceDefinition().register(registration);

        return registration;
    }
}
