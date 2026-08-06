/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.subsystem;

import org.jboss.as.model.test.ModelTestControllerVersion;

/**
 * Enumerates transformable wildfly-clustering versions.
 * @author Paul Ferraro
 */
public enum WildFlyClusteringVersion {
    EAP_8_1("5.0.11.Final"),
    ;

    private final String version;

    WildFlyClusteringVersion(String version) {
        this.version = version;
    }

    public String toGAV(String artifactId) {
        return String.format("org.wildfly.clustering:%s:%s", artifactId, this.version);
    }

    public static WildFlyClusteringVersion forVersion(ModelTestControllerVersion version) {
        return switch (version) {
            case EAP_8_1_0 -> WildFlyClusteringVersion.EAP_8_1;
            default -> throw new IllegalArgumentException(version.getMavenGavVersion());
        };
    }
}
