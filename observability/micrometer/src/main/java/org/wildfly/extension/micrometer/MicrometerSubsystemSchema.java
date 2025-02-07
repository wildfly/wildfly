/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.PersistentResourceXMLDescription.factory;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;

public enum MicrometerSubsystemSchema implements PersistentSubsystemSchema<MicrometerSubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 28
    VERSION_1_1(1, 1), // WildFly 29.0.0.Alpha1
    VERSION_2_0(2, 0) // WildFly 33
    ;

    public static final MicrometerSubsystemSchema CURRENT = VERSION_2_0;

    private final VersionedNamespace<IntVersion, MicrometerSubsystemSchema> namespace;

    MicrometerSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicrometerConfigurationConstants.NAME,
                new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicrometerSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Factory factory = factory(this);
        PersistentResourceXMLDescription.Builder builder =
                factory.builder(MicrometerSubsystemRegistrar.PATH);

        builder.addAttributes(MicrometerSubsystemRegistrar.ATTRIBUTES.stream());
        if (this.since(VERSION_2_0)) {
            builder.addChild(factory.builder(OtlpRegistryDefinitionRegistrar.PATH)
                    .addAttributes(OtlpRegistryDefinitionRegistrar.ATTRIBUTES.stream())
                    .setXmlElementName("otlp-registry")
                    .build());
        } else {
            builder.addChild(factory.builder(OtlpRegistryDefinitionRegistrar.PATH)
                    .addAttributes(MicrometerSubsystemRegistrar.ENDPOINT,
                            MicrometerSubsystemRegistrar.STEP)
                    .setXmlElementName("otlp-registry")
                    .build()
            );
        }

        return builder.build();
    }
}
