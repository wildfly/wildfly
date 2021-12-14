/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.clustering.ejb;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.clustering.controller.Schema;
import org.jboss.as.clustering.controller.persistence.AttributeXMLBuilderOperator;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * Parser description for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableEjbXMLParser extends PersistentResourceXMLParser {

    private final Schema<DistributableEjbSchema> schema;

    public DistributableEjbXMLParser(Schema<DistributableEjbSchema> schema) {
        this.schema = schema;
    }

    /**
     * Create a PersistentResourceXMLDescription that models the path, attributes and children of the XML schema
     * @return the PersistentResourceXMLDescription
     */
    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return new AttributeXMLBuilderOperator().addAttributes(DistributableEjbResourceDefinition.Attribute.class).apply(builder(DistributableEjbResourceDefinition.PATH, this.schema.getNamespaceUri()))
                .addChild(new AttributeXMLBuilderOperator().addAttributes(InfinispanBeanManagementResourceDefinition.Attribute.class).apply(builder(InfinispanBeanManagementResourceDefinition.WILDCARD_PATH)))
                .addChild(builder(LocalClientMappingsRegistryProviderResourceDefinition.PATH).setXmlElementName("local-client-mappings-registry"))
                .addChild(new AttributeXMLBuilderOperator(InfinispanClientMappingsRegistryProviderResourceDefinition.Attribute.class).apply(builder(InfinispanClientMappingsRegistryProviderResourceDefinition.PATH)).setXmlElementName("infinispan-client-mappings-registry"))
                .addChild(new AttributeXMLBuilderOperator(InfinispanTimerManagementResourceDefinition.Attribute.class).apply(builder(InfinispanTimerManagementResourceDefinition.WILDCARD_PATH)).setXmlElementName("infinispan-timer-management"))
                .build();
    }
}
