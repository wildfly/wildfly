/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.metrics;

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

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileMetricsExtension extends AbstractLegacyExtension {

    static final String EXTENSION_NAME = "org.wildfly.extension.microprofile.metrics-smallrye";

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-metrics-smallrye";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = MicroProfileMetricsExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    protected static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_2_0_0;

    private static final PersistentResourceXMLParser CURRENT_PARSER = new MicroProfileMetricsParser_2_0();

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);

    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MicroProfileMetricsExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public MicroProfileMetricsExtension() {
        super(EXTENSION_NAME, SUBSYSTEM_NAME);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(CURRENT_PARSER);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new MicroProfileMetricsSubsystemDefinition());

        return Collections.singleton(registration);
    }

    @Override
    public void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileMetricsParser_1_0.NAMESPACE, MicroProfileMetricsParser_1_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileMetricsParser_2_0.NAMESPACE, CURRENT_PARSER);
    }
}
