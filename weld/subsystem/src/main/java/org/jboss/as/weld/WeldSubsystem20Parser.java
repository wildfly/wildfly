/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

class WeldSubsystem20Parser extends PersistentResourceXMLParser {

    public static final String NAMESPACE = "urn:jboss:domain:weld:2.0";
    static final WeldSubsystem20Parser INSTANCE = new WeldSubsystem20Parser();
    private static final PersistentResourceXMLDescription xmlDescription;


    static {
        xmlDescription = PersistentResourceXMLDescription.builder(WeldExtension.PATH_SUBSYSTEM, NAMESPACE)
                .addAttributes(WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE)
                .build();
    }

    private WeldSubsystem20Parser() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
