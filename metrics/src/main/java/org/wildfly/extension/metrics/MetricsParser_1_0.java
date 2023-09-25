/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.metrics;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MetricsParser_1_0 extends PersistentResourceXMLParser {
    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:wildfly:metrics:1.0";

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MetricsExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addAttributes(
                        MetricsSubsystemDefinition.SECURITY_ENABLED,
                        MetricsSubsystemDefinition.EXPOSED_SUBSYSTEMS,
                        MetricsSubsystemDefinition.PREFIX)
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}