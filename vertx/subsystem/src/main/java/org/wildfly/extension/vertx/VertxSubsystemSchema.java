/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.jboss.as.controller.PersistentResourceXMLDescription.decorator;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTIONS;
import static org.wildfly.extension.vertx.VertxSubsystemExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.vertx.VertxSubsystemExtension.SUBSYSTEM_PATH;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
enum VertxSubsystemSchema implements PersistentSubsystemSchema<VertxSubsystemSchema> {
    VERSION_1_0_PREVIEW(1, 0,Stability.PREVIEW),
            ;

    static final VertxSubsystemSchema CURRENT = VERSION_1_0_PREVIEW;

    private final VersionedNamespace<IntVersion, VertxSubsystemSchema> namespace;

    VertxSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(SUBSYSTEM_NAME, stability,
                new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, VertxSubsystemSchema> getNamespace() {
        return namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(SUBSYSTEM_PATH, namespace)
                .addChild(
                        builder(VertxResourceDefinition.INSTANCE.getPathElement())
                                .addAttributes(VertxAttributes.getSimpleAttributes().toArray(new AttributeDefinition[0]))
                )
                .addChild(
                        decorator(ELEMENT_VERTX_OPTIONS)
                                .addChild(
                                        builder(VertxOptionsResourceDefinition.INSTANCE.getPathElement())
                                                .addAttributes(VertxOptionsAttributes.getVertxOptionsAttributes().toArray(new AttributeDefinition[0]))
                                )
                                .addChild(
                                        builder(AddressResolverResourceDefinition.INSTANCE.getPathElement())
                                                .addAttributes(AddressResolverResourceDefinition.getVertxAddressResolverOptionsAttrs().toArray(new AttributeDefinition[0]))
                                )
                )
                .build();
    }
}
