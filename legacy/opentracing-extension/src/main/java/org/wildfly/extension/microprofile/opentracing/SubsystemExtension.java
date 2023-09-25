/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.opentracing;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

public class SubsystemExtension extends AbstractLegacyExtension {
    public static final String SUBSYSTEM_NAME = "microprofile-opentracing-smallrye";
    public static final String EXTENSION_NAME = "org.wildfly.extension.microprofile.opentracing-smallrye";
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = SubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    protected static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    protected static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_3_0_0;

    private static final PersistentResourceXMLParser PARSER = SubsytemParser_3_0.INSTANCE;
    public static final String NAMESPACE = SubsytemParser_3_0.OPENTRACING_NAMESPACE;

    public SubsystemExtension() {
        super(EXTENSION_NAME, SUBSYSTEM_NAME);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(false, keyPrefix);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, SubsystemExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(PARSER);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new SubsystemDefinition());
        subsystem.registerDeploymentModel(new TracingDeploymentDefinition());

        return Collections.singleton(registration);
    }

    @Override
    public void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, SubsytemParser_1_0.NAMESPACE, SubsytemParser_1_0.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, SubsytemParser_2_0.OPENTRACING_NAMESPACE, SubsytemParser_2_0.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, PARSER);
    }
}
