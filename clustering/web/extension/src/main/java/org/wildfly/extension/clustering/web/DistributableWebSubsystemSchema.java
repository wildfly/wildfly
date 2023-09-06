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

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the schema versions for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public enum DistributableWebSubsystemSchema implements PersistentSubsystemSchema<DistributableWebSubsystemSchema> {

    VERSION_1_0(1, 0), // WildFly 17
    VERSION_2_0(2, 0), // WildFly 18-26.1, EAP 7.4
    VERSION_3_0(3, 0), // WildFly 27-29
    VERSION_4_0(4, 0), // WildFly 30-present, EAP 8.0
    ;
    static final DistributableWebSubsystemSchema CURRENT = VERSION_4_0;

    private final VersionedNamespace<IntVersion, DistributableWebSubsystemSchema> namespace;

    DistributableWebSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(DistributableWebExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableWebSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(DistributableWebResourceDefinition.PATH, this.namespace).addAttributes(Attribute.stream(DistributableWebResourceDefinition.Attribute.class))
                .addChild(this.getInfinispanSessionManagementResourceXMLBuilder())
                .addChild(this.getHotRodSessionManagementResourceXMLBuilder())
                .addChild(builder(InfinispanSSOManagementResourceDefinition.WILDCARD_PATH).addAttributes(Attribute.stream(InfinispanSSOManagementResourceDefinition.Attribute.class)))
                .addChild(builder(HotRodSSOManagementResourceDefinition.WILDCARD_PATH).addAttributes(Attribute.stream(HotRodSSOManagementResourceDefinition.Attribute.class)))
                .addChild(builder(LocalRoutingProviderResourceDefinition.PATH).setXmlElementName("local-routing"))
                .addChild(builder(InfinispanRoutingProviderResourceDefinition.PATH).addAttributes(Attribute.stream(InfinispanRoutingProviderResourceDefinition.Attribute.class)).setXmlElementName("infinispan-routing"))
                .build();
    }

    private PersistentResourceXMLBuilder getInfinispanSessionManagementResourceXMLBuilder() {
        PersistentResourceXMLBuilder builder = builder(InfinispanSessionManagementResourceDefinition.WILDCARD_PATH).addAttributes(Stream.concat(Attribute.stream(InfinispanSessionManagementResourceDefinition.Attribute.class), Attribute.stream(SessionManagementResourceDefinition.Attribute.class)));
        addAffinityChildren(builder).addChild(builder(PrimaryOwnerAffinityResourceDefinition.PATH).setXmlElementName("primary-owner-affinity"));
        if (this.namespace.since(DistributableWebSubsystemSchema.VERSION_2_0)) {
            builder.addChild(builder(RankedAffinityResourceDefinition.PATH).addAttributes(Attribute.stream(RankedAffinityResourceDefinition.Attribute.class)).setXmlElementName("ranked-affinity"));
        }
        return builder;
    }

    private PersistentResourceXMLBuilder getHotRodSessionManagementResourceXMLBuilder() {
        return addAffinityChildren(builder(HotRodSessionManagementResourceDefinition.WILDCARD_PATH).addAttributes(Stream.concat(Attribute.stream(HotRodSessionManagementResourceDefinition.Attribute.class), Attribute.stream(SessionManagementResourceDefinition.Attribute.class))));
    }

    private static PersistentResourceXMLBuilder addAffinityChildren(PersistentResourceXMLBuilder builder) {
        return builder
                .addChild(builder(NoAffinityResourceDefinition.PATH).setXmlElementName("no-affinity"))
                .addChild(builder(LocalAffinityResourceDefinition.PATH).setXmlElementName("local-affinity"))
                ;
    }
}
