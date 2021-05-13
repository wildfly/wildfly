/*
 * Copyright 2021 JBoss by Red Hat.
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
package org.wildfly.extension.opentelemetry.extension;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Runtime resource definition for the OpenTelemetry configuration of the deployment.
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetryDeploymentDefinition extends SimpleResourceDefinition {

    public static final OpenTelemetryDeploymentDefinition INSTANCE = new OpenTelemetryDeploymentDefinition();

    private OpenTelemetryDeploymentDefinition() {
          super(new Parameters(OpenTelemetrySubsystemExtension.SUBSYSTEM_PATH,
                  OpenTelemetrySubsystemExtension.getResourceDescriptionResolver())
                  .setFeature(false));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
//        resourceRegistration.registerReadOnlyAttribute(TRACER_CONFIGURATION_NAME, null);
//        resourceRegistration.registerReadOnlyAttribute(TRACER_CONFIGURATION, null);
    }
}
