package org.wildfly.extension.opentelemetry.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetrySubsystemExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "opentelemetry";

    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME =
            OpenTelemetrySubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;

    private static final OpenTelemetryParser_1_0 CURRENT_PARSER = new OpenTelemetryParser_1_0();

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);

    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes,
                                                                      final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
                OpenTelemetrySubsystemExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(CURRENT_PARSER);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(OpenTelemetrySubsystemDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerDeploymentModel(OpenTelemetryDeploymentDefinition.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, OpenTelemetryParser_1_0.NAMESPACE, CURRENT_PARSER);
    }
}
