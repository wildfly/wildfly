/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.capability.RuntimeCapability;

public class MicroProfileTelemetrySubsystemDefinition extends PersistentResourceDefinition {
    static final String MICROPROFILE_TELEMETRY_MODULE = "org.wildfly.extension.microprofile.telemetry";
    static final String MICROPROFILE_TELEMETRY_API_MODULE = "org.wildfly.extension.microprofile.telemetry-api";
    static final String OPENTELEMETRY_CAPABILITY_NAME = "org.wildfly.extension.opentelemetry";

    public static final String[] EXPORTED_MODULES = {
            "io.opentelemetry.api",
            "io.opentelemetry.context",
            "io.opentelemetry.exporter",
            "io.opentelemetry.sdk",
            "io.smallrye.config",
            "io.smallrye.opentelemetry",
            "org.eclipse.microprofile.config.api",
            MICROPROFILE_TELEMETRY_API_MODULE
    };

    static final RuntimeCapability<Void> MICROPROFILE_TELEMETRY_CAPABILITY =
            RuntimeCapability.Builder.of(MICROPROFILE_TELEMETRY_MODULE)
                    .addRequirements(WELD_CAPABILITY_NAME, OPENTELEMETRY_CAPABILITY_NAME)
                    .build();

    protected MicroProfileTelemetrySubsystemDefinition() {
        super(new Parameters(MicroProfileTelemetryExtension.SUBSYSTEM_PATH, MicroProfileTelemetryExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new MicroProfileTelemetrySubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(MICROPROFILE_TELEMETRY_CAPABILITY)
        );
    }
    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.EMPTY_LIST;
    }
}
