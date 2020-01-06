/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants;

/**
 * Runtime resource definition for the OpenTracing configuration of the deployment.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TracingDeploymentDefinition extends SimpleResourceDefinition {

    public static final TracingDeploymentDefinition INSTANCE = new TracingDeploymentDefinition();

    public static final AttributeDefinition TRACER_CONFIGURATION_NAME = new SimpleAttributeDefinitionBuilder(
            TracerConfigurationConstants.TRACER_CONFIGURATION_NAME, ModelType.STRING, true)
            .setStorageRuntime()
            .build();
    public static final AttributeDefinition TRACER_CONFIGURATION = new SimpleAttributeDefinitionBuilder(
            TracerConfigurationConstants.TRACER_CONFIGURATION, ModelType.OBJECT, true)
            .setStorageRuntime()
            .build();

    private TracingDeploymentDefinition() {
          super(new Parameters(SubsystemExtension.SUBSYSTEM_PATH, SubsystemExtension.getResourceDescriptionResolver())
                  .setFeature(false));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(TRACER_CONFIGURATION_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(TRACER_CONFIGURATION, null);
    }
}
