/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi;

import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of MicroProfile OpenAPI subsystem schema versions.
 * @author Paul Ferraro
 */
public enum MicroProfileOpenAPISubsystemSchema implements SubsystemResourceXMLSchema<MicroProfileOpenAPISubsystemSchema> {

    VERSION_1_0(1, 0), // WildFly 19
    ;
    static final MicroProfileOpenAPISubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileOpenAPISubsystemSchema> namespace;
    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);

    MicroProfileOpenAPISubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileOpenAPISubsystemRegistrar.REGISTRATION.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileOpenAPISubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        return this.factory.subsystemElement(MicroProfileOpenAPISubsystemRegistrar.REGISTRATION).build();
    }
}
