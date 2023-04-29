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
package org.wildfly.extension.micrometer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

public enum MicrometerSubsystemModel implements SubsystemModel {
    VERSION_1_0_0(1, 0, 0),
    VERSION_1_1_0(1, 1, 0);

    public static final MicrometerSubsystemModel CURRENT = VERSION_1_1_0;

    private final ModelVersion version;

    MicrometerSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return version;
    }
}
