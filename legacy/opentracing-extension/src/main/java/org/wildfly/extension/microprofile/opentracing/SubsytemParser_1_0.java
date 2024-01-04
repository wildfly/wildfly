/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

public class SubsytemParser_1_0 extends PersistentResourceXMLParser {
    public static final String NAMESPACE = "urn:wildfly:microprofile-opentracing-smallrye:1.0";

    static final PersistentResourceXMLParser INSTANCE = new SubsytemParser_1_0();

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(SubsystemExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addAttributes()
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
