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

import org.jboss.as.clustering.controller.Schema;
import org.jboss.as.clustering.controller.persistence.AttributeXMLBuilderOperator;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * Parser description for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public class DistributableWebXMLParser extends PersistentResourceXMLParser {

    private final Schema<DistributableWebSchema> schema;

    public DistributableWebXMLParser(Schema<DistributableWebSchema> schema) {
        this.schema = schema;
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return builder(DistributableWebResourceDefinition.PATH, this.schema.getNamespaceUri())
                .addAttribute(DistributableWebResourceDefinition.Attribute.DEFAULT_SESSION_MANAGEMENT.getDefinition())
                .addAttribute(DistributableWebResourceDefinition.Attribute.DEFAULT_SSO_MANAGEMENT.getDefinition())
                .addChild(new AttributeXMLBuilderOperator()
                        .addAttributes(SessionManagementResourceDefinition.Attribute.class)
                        .addAttributes(InfinispanSessionManagementResourceDefinition.Attribute.class)
                        .apply(builder(InfinispanSessionManagementResourceDefinition.WILDCARD_PATH))
                        .addChild(builder(NoAffinityResourceDefinition.PATH).setXmlElementName("no-affinity"))
                        .addChild(builder(LocalAffinityResourceDefinition.PATH).setXmlElementName("local-affinity"))
                        .addChild(builder(PrimaryOwnerAffinityResourceDefinition.PATH).setXmlElementName("primary-owner-affinity")))
                .addChild(new AttributeXMLBuilderOperator(InfinispanSSOManagementResourceDefinition.Attribute.class)
                        .apply(builder(InfinispanSSOManagementResourceDefinition.WILDCARD_PATH)))
                .addChild(builder(LocalRoutingProviderResourceDefinition.PATH).setXmlElementName("local-routing"))
                .addChild(new AttributeXMLBuilderOperator(InfinispanRoutingProviderResourceDefinition.Attribute.class).apply(builder(InfinispanRoutingProviderResourceDefinition.PATH)).setXmlElementName("infinispan-routing"))
                .build();
    }
}
