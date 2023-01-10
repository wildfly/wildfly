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

import org.jboss.as.clustering.controller.persistence.AttributeXMLBuilderOperator;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;

/**
 * XML description factory for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public enum DistributableWebXMLDescriptionFactory implements Function<DistributableWebSchema, PersistentResourceXMLDescription> {
    INSTANCE;

    @Override
    public PersistentResourceXMLDescription apply(DistributableWebSchema schema) {
        return builder(DistributableWebResourceDefinition.PATH, schema.getUri())
                .addAttribute(DistributableWebResourceDefinition.Attribute.DEFAULT_SESSION_MANAGEMENT.getDefinition())
                .addAttribute(DistributableWebResourceDefinition.Attribute.DEFAULT_SSO_MANAGEMENT.getDefinition())
                .addChild(this.getInfinispanSessionManagementResourceXMLBuilder(schema))
                .addChild(this.getHotRodSessionManagementResourceXMLBuilder(schema))
                .addChild(new AttributeXMLBuilderOperator(InfinispanSSOManagementResourceDefinition.Attribute.class)
                        .apply(builder(InfinispanSSOManagementResourceDefinition.WILDCARD_PATH)))
                .addChild(new AttributeXMLBuilderOperator(HotRodSSOManagementResourceDefinition.Attribute.class)
                        .apply(builder(HotRodSSOManagementResourceDefinition.WILDCARD_PATH)))
                .addChild(builder(LocalRoutingProviderResourceDefinition.PATH).setXmlElementName("local-routing"))
                .addChild(new AttributeXMLBuilderOperator(InfinispanRoutingProviderResourceDefinition.Attribute.class).apply(builder(InfinispanRoutingProviderResourceDefinition.PATH)).setXmlElementName("infinispan-routing"))
                .build();
    }

    private PersistentResourceXMLBuilder getInfinispanSessionManagementResourceXMLBuilder(DistributableWebSchema schema) {
        PersistentResourceXMLBuilder builder = new AttributeXMLBuilderOperator()
                .addAttributes(InfinispanSessionManagementResourceDefinition.Attribute.class)
                .addAttributes(SessionManagementResourceDefinition.Attribute.class)
                .apply(builder(InfinispanSessionManagementResourceDefinition.WILDCARD_PATH));
        this.addAffinityChildren(builder)
                .addChild(builder(PrimaryOwnerAffinityResourceDefinition.PATH).setXmlElementName("primary-owner-affinity"));
        if (schema.since(DistributableWebSchema.VERSION_2_0)) {
            builder.addChild(new AttributeXMLBuilderOperator(RankedAffinityResourceDefinition.Attribute.class).apply(builder(RankedAffinityResourceDefinition.PATH).setXmlElementName("ranked-affinity")));
        }
        return builder;
    }

    private PersistentResourceXMLBuilder getHotRodSessionManagementResourceXMLBuilder(DistributableWebSchema schema) {
        PersistentResourceXMLBuilder builder = new AttributeXMLBuilderOperator()
                .addAttributes(HotRodSessionManagementResourceDefinition.Attribute.class)
                .addAttributes(SessionManagementResourceDefinition.Attribute.class)
                .apply(builder(HotRodSessionManagementResourceDefinition.WILDCARD_PATH));
        return this.addAffinityChildren(builder);
    }

    @SuppressWarnings("static-method")
    private PersistentResourceXMLBuilder addAffinityChildren(PersistentResourceXMLBuilder builder) {
        return builder
                .addChild(builder(NoAffinityResourceDefinition.PATH).setXmlElementName("no-affinity"))
                .addChild(builder(LocalAffinityResourceDefinition.PATH).setXmlElementName("local-affinity"))
                ;
    }
}
