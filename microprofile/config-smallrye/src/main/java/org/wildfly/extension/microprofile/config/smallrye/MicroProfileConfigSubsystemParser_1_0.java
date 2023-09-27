/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class MicroProfileConfigSubsystemParser_1_0 extends PersistentResourceXMLParser {
    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:wildfly:microprofile-config-smallrye:1.0";

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MicroProfileConfigExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addChild(builder(MicroProfileConfigExtension.CONFIG_SOURCE_PATH)
                        .addAttributes(
                                ConfigSourceDefinition.ORDINAL,
                                ConfigSourceDefinition.PROPERTIES,
                                ConfigSourceDefinition.CLASS,
                                ConfigSourceDefinition.DIR_1_0))
                .addChild(builder(MicroProfileConfigExtension.CONFIG_SOURCE_PROVIDER_PATH)
                        .addAttributes(
                                ConfigSourceProviderDefinition.CLASS))
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
