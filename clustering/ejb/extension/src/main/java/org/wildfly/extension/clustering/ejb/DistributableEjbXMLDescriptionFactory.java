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

import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;

/**
 * Parser description for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DistributableEjbXMLDescriptionFactory implements Function<DistributableEjbSchema, PersistentResourceXMLDescription> {
    INSTANCE;

    @Override
    public PersistentResourceXMLDescription apply(DistributableEjbSchema schema) {
        return builder(DistributableEjbResourceDefinition.PATH, schema.getUri(), Attribute.stream(DistributableEjbResourceDefinition.Attribute.class))
                .addChild(builder(InfinispanBeanManagementResourceDefinition.WILDCARD_PATH, Stream.concat(Attribute.stream(BeanManagementResourceDefinition.Attribute.class), Attribute.stream(InfinispanBeanManagementResourceDefinition.Attribute.class))))
                .addChild(builder(LocalClientMappingsRegistryProviderResourceDefinition.PATH).setXmlElementName("local-client-mappings-registry"))
                .addChild(builder(InfinispanClientMappingsRegistryProviderResourceDefinition.PATH, Attribute.stream(InfinispanClientMappingsRegistryProviderResourceDefinition.Attribute.class)).setXmlElementName("infinispan-client-mappings-registry"))
                .addChild(builder(InfinispanTimerManagementResourceDefinition.WILDCARD_PATH, Attribute.stream(InfinispanTimerManagementResourceDefinition.Attribute.class)).setXmlElementName("infinispan-timer-management"))
                .build();
    }

    // TODO Drop methods below once WFCORE-6218 is available
    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder(PathElement path) {
        return builder(path, Stream.empty());
    }

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder(PathElement path, Stream<? extends AttributeDefinition> attributes) {
        return builder(path, null, attributes);
    }

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder(PathElement path, String namespaceUri, Stream<? extends AttributeDefinition> attributes) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = PersistentResourceXMLDescription.builder(path, namespaceUri);
        attributes.forEach(builder::addAttribute);
        return builder;
    }
}
