/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;

/**
 * XML description factory for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public enum DistributableWebXMLDescriptionFactory implements Function<DistributableWebSubsystemSchema, PersistentResourceXMLDescription> {
    INSTANCE;

    @Override
    public PersistentResourceXMLDescription apply(DistributableWebSubsystemSchema schema) {
        return builder(DistributableWebResourceDefinition.PATH, schema.getNamespace()).addAttributes(Attribute.stream(DistributableWebResourceDefinition.Attribute.class))
                .addChild(getInfinispanSessionManagementResourceXMLBuilder(schema))
                .addChild(getHotRodSessionManagementResourceXMLBuilder(schema))
                .addChild(builder(InfinispanSSOManagementResourceDefinition.WILDCARD_PATH).addAttributes(Attribute.stream(InfinispanSSOManagementResourceDefinition.Attribute.class)))
                .addChild(builder(HotRodSSOManagementResourceDefinition.WILDCARD_PATH).addAttributes(Attribute.stream(HotRodSSOManagementResourceDefinition.Attribute.class)))
                .addChild(builder(LocalRoutingProviderResourceDefinition.PATH).setXmlElementName("local-routing"))
                .addChild(builder(InfinispanRoutingProviderResourceDefinition.PATH).addAttributes(Attribute.stream(InfinispanRoutingProviderResourceDefinition.Attribute.class)).setXmlElementName("infinispan-routing"))
                .build();
    }

    private static PersistentResourceXMLBuilder getInfinispanSessionManagementResourceXMLBuilder(DistributableWebSubsystemSchema schema) {
        PersistentResourceXMLBuilder builder = builder(InfinispanSessionManagementResourceDefinition.WILDCARD_PATH).addAttributes(Stream.concat(Attribute.stream(InfinispanSessionManagementResourceDefinition.Attribute.class), Attribute.stream(SessionManagementResourceDefinition.Attribute.class)));
        addAffinityChildren(builder).addChild(builder(PrimaryOwnerAffinityResourceDefinition.PATH).setXmlElementName("primary-owner-affinity"));
        if (schema.since(DistributableWebSubsystemSchema.VERSION_2_0)) {
            builder.addChild(builder(RankedAffinityResourceDefinition.PATH).addAttributes(Attribute.stream(RankedAffinityResourceDefinition.Attribute.class)).setXmlElementName("ranked-affinity"));
        }
        return builder;
    }

    private static PersistentResourceXMLBuilder getHotRodSessionManagementResourceXMLBuilder(DistributableWebSubsystemSchema schema) {
        return addAffinityChildren(builder(HotRodSessionManagementResourceDefinition.WILDCARD_PATH).addAttributes(Stream.concat(Attribute.stream(HotRodSessionManagementResourceDefinition.Attribute.class), Attribute.stream(SessionManagementResourceDefinition.Attribute.class))));
    }

    private static PersistentResourceXMLBuilder addAffinityChildren(PersistentResourceXMLBuilder builder) {
        return builder
                .addChild(builder(NoAffinityResourceDefinition.PATH).setXmlElementName("no-affinity"))
                .addChild(builder(LocalAffinityResourceDefinition.PATH).setXmlElementName("local-affinity"))
                ;
    }
}
