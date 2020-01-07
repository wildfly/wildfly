/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.opentracing;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;

import java.util.Collection;
import java.util.Collections;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;

public class SubsystemDefinition extends PersistentResourceDefinition {
    private static final String OPENTRACING_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing";

    private static final RuntimeCapability<Void> OPENTRACING_CAPABILITY = RuntimeCapability.Builder
            .of(OPENTRACING_CAPABILITY_NAME)
            .addRequirements(WELD_CAPABILITY_NAME)
            .build();

    static final String[] MODULES = {
            "io.jaegertracing.jaeger",
            "io.opentracing.contrib.opentracing-tracerresolver",
            "io.opentracing.opentracing-api",
            "io.opentracing.opentracing-util",
            "org.eclipse.microprofile.opentracing",
            "org.eclipse.microprofile.restclient",
            "io.opentracing.contrib.opentracing-jaxrs2"
    };

    static final String[] EXPORTED_MODULES = {
            "io.smallrye.opentracing",
            "org.wildfly.microprofile.opentracing-smallrye",
            "io.opentracing.contrib.opentracing-interceptors",
    };
    protected SubsystemDefinition() {
        super( new SimpleResourceDefinition.Parameters(SubsystemExtension.SUBSYSTEM_PATH, SubsystemExtension.getResourceDescriptionResolver(SubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(SubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(OPENTRACING_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerAdditionalRuntimePackages(final ManagementResourceRegistration resourceRegistration) {
        for (String m : MODULES) {
            resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(m));
        }
        for (String m : EXPORTED_MODULES) {
            resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(m));
        }
    }
}
