/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
                .addChild(builder(LocalModuleAvailabilityRegistrarProviderResourceDefinition.PATH).setXmlElementName("local-module-availability-registrar"))
                .addChild(builder(InfinispanModuleAvailabilityRegistrarProviderResourceDefinition.PATH).addAttributes(Attribute.stream(InfinispanModuleAvailabilityRegistrarProviderResourceDefinition.Attribute.class)).setXmlElementName("infinispan-module-availability-registrar"))
                .addChild(builder(InfinispanTimerManagementResourceDefinition.WILDCARD_PATH).addAttributes(Attribute.stream(InfinispanTimerManagementResourceDefinition.Attribute.class)).setXmlElementName("infinispan-timer-management"))
                .build();
    }
}
