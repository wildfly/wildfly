/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinition;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinition;

public enum MicrometerSubsystemSchema implements PersistentSubsystemSchema<MicrometerSubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 28
    VERSION_1_1(1, 1), // WildFly 29.0.0.Alpha1
    VERSION_2_0(2, 0), // WildFly 32
    ;

    public static final MicrometerSubsystemSchema CURRENT = VERSION_2_0;

    private final VersionedNamespace<IntVersion, MicrometerSubsystemSchema> namespace;

    MicrometerSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicrometerExtension.SUBSYSTEM_NAME,
                new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicrometerSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder =
                builder(MicrometerExtension.SUBSYSTEM_PATH, this.namespace);

        builder.addAttributes(MicrometerSubsystemDefinition.ATTRIBUTES);
        builder.addChild(builder(OtlpRegistryDefinition.PATH_ELEMENT)
                .addAttributes(OtlpRegistryDefinition.ATTRIBUTES)
                .setXmlElementName("otlp-registry"));
        if (this.since(VERSION_2_0)) {
            builder.addChild(builder(PrometheusRegistryDefinition.PATH_ELEMENT)
                    .addAttributes(PrometheusRegistryDefinition.ATTRIBUTES)
                    .setXmlElementName("prometheus-registry"));
        }

        return builder.build();
    }
}
