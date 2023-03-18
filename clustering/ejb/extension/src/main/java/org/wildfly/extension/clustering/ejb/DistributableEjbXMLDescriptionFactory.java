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

import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PersistentResourceXMLDescription;

/**
 * Parser description for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DistributableEjbXMLDescriptionFactory implements Function<DistributableEjbSubsystemSchema, PersistentResourceXMLDescription> {
    INSTANCE;

    @Override
    public PersistentResourceXMLDescription apply(DistributableEjbSubsystemSchema schema) {
        return builder(DistributableEjbResourceDefinition.PATH, schema.getNamespace()).addAttributes(Attribute.stream(DistributableEjbResourceDefinition.Attribute.class))
                .addChild(builder(InfinispanBeanManagementResourceDefinition.WILDCARD_PATH).addAttributes(Stream.concat(Attribute.stream(BeanManagementResourceDefinition.Attribute.class), Attribute.stream(InfinispanBeanManagementResourceDefinition.Attribute.class))))
                .addChild(builder(LocalClientMappingsRegistryProviderResourceDefinition.PATH).setXmlElementName("local-client-mappings-registry"))
                .addChild(builder(InfinispanClientMappingsRegistryProviderResourceDefinition.PATH).addAttributes(Attribute.stream(InfinispanClientMappingsRegistryProviderResourceDefinition.Attribute.class)).setXmlElementName("infinispan-client-mappings-registry"))
                .addChild(builder(InfinispanTimerManagementResourceDefinition.WILDCARD_PATH).addAttributes(Attribute.stream(InfinispanTimerManagementResourceDefinition.Attribute.class)).setXmlElementName("infinispan-timer-management"))
                .build();
    }
}
