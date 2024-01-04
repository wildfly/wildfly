/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.beanvalidation;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enum representing the namespaces defined for the Jakarta Bean Validation subsystem.
 *
 * @author Eduardo Martins
 */
enum BeanValidationSubsystemSchema implements PersistentSubsystemSchema<BeanValidationSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final BeanValidationSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, BeanValidationSubsystemSchema> namespace;

    BeanValidationSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(BeanValidationExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, BeanValidationSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(BeanValidationExtension.SUBSYSTEM_PATH, this.namespace).build();
    }
}
