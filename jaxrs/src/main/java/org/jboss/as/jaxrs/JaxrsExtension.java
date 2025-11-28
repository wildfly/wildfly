/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * Domain extension used to initialize the jaxrs subsystem.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 */
public class JaxrsExtension extends SubsystemExtension<JaxrsSubsystemSchema> {

    public static final String SUBSYSTEM_NAME = "jaxrs";

    private static final String RESOURCE_NAME = JaxrsExtension.class.getPackage().getName() + ".LocalDescriptions";
    static PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static ResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JaxrsExtension.class.getClassLoader(), true, false);
    }

    public JaxrsExtension() {
        super(SubsystemConfiguration.of(SUBSYSTEM_NAME, JaxrsSubsystemModel.CURRENT, JaxrsSubsystemRegistrar::new),
                SubsystemPersistence.of(JaxrsSubsystemSchema.CURRENT));
    }

    enum JaxrsSubsystemModel implements SubsystemModel {
        VERSION_4_0_0(ModelVersion.create(4, 0, 0)),
        VERSION_5_0_0(ModelVersion.create(5, 0, 0)),
        VERSION_6_0_0(ModelVersion.create(6, 0, 0)),
        ;

        static final JaxrsSubsystemModel CURRENT = VERSION_6_0_0;
        private final ModelVersion version;

        JaxrsSubsystemModel(final ModelVersion version) {
            this.version = version;
        }

        @Override
        public ModelVersion getVersion() {
            return version;
        }

        @Override
        public boolean requiresTransformation(final ModelVersion version) {
            return SubsystemModel.super.requiresTransformation(version);
        }
    }
}
