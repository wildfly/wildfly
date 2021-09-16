/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
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

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
                OpenTelemetrySubsystemExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME,
                OpenTelemetryModel.CURRENT.getVersion());
        subsystem.registerXMLElementWriter(new OpenTelemetryParser(OpenTelemetrySchema.CURRENT));

        final ManagementResourceRegistration registration =
                subsystem.registerSubsystemModel(new OpenTelemetrySubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION,
                GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (OpenTelemetrySchema schema : EnumSet.allOf(OpenTelemetrySchema.class)) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespaceUri(), new OpenTelemetryParser(schema));
        }
    }
}
