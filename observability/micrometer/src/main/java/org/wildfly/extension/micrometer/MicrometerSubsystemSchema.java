/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.PersistentResourceXMLDescription.factory;
import static org.wildfly.extension.micrometer.MicrometerConfigurationConstants.OTLP_REGISTRY;
import static org.wildfly.extension.micrometer.MicrometerConfigurationConstants.PROMETHEUS_REGISTRY;
import static org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar.RESOURCE_REGISTRATION;

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar;

public enum MicrometerSubsystemSchema implements PersistentSubsystemSchema<MicrometerSubsystemSchema> {
    VERSION_1_0(1, 0, Stability.DEFAULT), // WildFly 28
    VERSION_1_1(1, 1, Stability.DEFAULT), // WildFly 29.0.0.Alpha1
    VERSION_2_0_COMMUNITY(2, 0, Stability.COMMUNITY) // WildFly 36
    ;

    public static final Set<MicrometerSubsystemSchema> CURRENT = EnumSet.of(VERSION_1_1, VERSION_2_0_COMMUNITY);

    private final VersionedNamespace<IntVersion, MicrometerSubsystemSchema> namespace;

    MicrometerSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicrometerConfigurationConstants.NAME, stability,
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
            factory.builder(MicrometerSubsystemRegistrar.SUBSYSTEM_PATH)
                .addChild(factory.builder(OtlpRegistryDefinitionRegistrar.PATH)
                    .addAttributes(OtlpRegistryDefinitionRegistrar.ATTRIBUTES.stream())
                    .setXmlElementName(OTLP_REGISTRY)
                    .build());

        builder.addAttributes(MicrometerSubsystemRegistrar.ATTRIBUTES.stream())
            .addChild(factory.builder(RESOURCE_REGISTRATION)
                .addAttributes(PrometheusRegistryDefinitionRegistrar.ATTRIBUTES.stream())
                .setXmlElementName(PROMETHEUS_REGISTRY)
                .build());

        return builder.build();
    }
}
