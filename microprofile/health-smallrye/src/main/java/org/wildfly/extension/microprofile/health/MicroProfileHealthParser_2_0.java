/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.EMPTY_LIVENESS_CHECKS_STATUS;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.EMPTY_READINESS_CHECKS_STATUS;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.SECURITY_ENABLED;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2019 Red Hat inc.
 */
public class MicroProfileHealthParser_2_0 extends PersistentResourceXMLParser {
    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:wildfly:microprofile-health-smallrye:2.0";

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MicroProfileHealthExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addAttributes(SECURITY_ENABLED,
                        EMPTY_LIVENESS_CHECKS_STATUS,
                        EMPTY_READINESS_CHECKS_STATUS)
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
