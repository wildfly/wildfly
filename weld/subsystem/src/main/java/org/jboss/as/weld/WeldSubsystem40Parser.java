/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

class WeldSubsystem40Parser extends PersistentResourceXMLParser {

    public static final String NAMESPACE = "urn:jboss:domain:weld:4.0";
    static final WeldSubsystem40Parser INSTANCE = new WeldSubsystem40Parser();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = PersistentResourceXMLDescription.builder(WeldExtension.PATH_SUBSYSTEM, NAMESPACE)
                .addAttributes(WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE,
                        WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE, WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE)
                .build();
    }

    private WeldSubsystem40Parser() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
