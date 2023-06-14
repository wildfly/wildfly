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

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

public enum MicrometerSubsystemSchema implements PersistentSubsystemSchema<MicrometerSubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 28
    VERSION_1_1(1, 1), // WildFly 29.0.0.Alpha1
    ;

    public static final MicrometerSubsystemSchema CURRENT = VERSION_1_1;

    private final VersionedNamespace<IntVersion, MicrometerSubsystemSchema> namespace;

    MicrometerSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicrometerExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicrometerSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(org.wildfly.extension.micrometer.MicrometerExtension.SUBSYSTEM_PATH, this.namespace)
                .addAttributes(MicrometerSubsystemDefinition.ATTRIBUTES)
                .build();
    }
}
