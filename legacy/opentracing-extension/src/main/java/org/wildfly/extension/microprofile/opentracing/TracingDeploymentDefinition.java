/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Runtime resource definition for the OpenTracing configuration of the deployment.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TracingDeploymentDefinition extends ModelOnlyResourceDefinition {

    public static final AttributeDefinition TRACER_CONFIGURATION_NAME = new SimpleAttributeDefinitionBuilder(
            TracerConfigurationConstants.TRACER_CONFIGURATION_NAME, ModelType.STRING, true)
            .setStorageRuntime()
            .build();
    public static final AttributeDefinition TRACER_CONFIGURATION = new SimpleAttributeDefinitionBuilder(
            TracerConfigurationConstants.TRACER_CONFIGURATION, ModelType.OBJECT, true)
            .setStorageRuntime()
            .build();

    TracingDeploymentDefinition() {
          super(new Parameters(SubsystemExtension.SUBSYSTEM_PATH, SubsystemExtension.getResourceDescriptionResolver())
                  .setFeature(false).setRuntime());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(TRACER_CONFIGURATION_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(TRACER_CONFIGURATION, null);
    }
}
