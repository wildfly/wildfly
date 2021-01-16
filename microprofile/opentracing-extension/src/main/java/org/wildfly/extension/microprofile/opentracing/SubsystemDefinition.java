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
import static org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory.TRACER_CAPABILITY_NAME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;

import java.util.Collection;
import java.util.Collections;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants;

public class SubsystemDefinition extends PersistentResourceDefinition {

    public static final String OPENTRACING_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing";
    public static final String DEFAULT_TRACER_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing.default-tracer";
    public static final String MICROPROFILE_CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";

    public static final RuntimeCapability<Void> OPENTRACING_CAPABILITY = RuntimeCapability.Builder
            .of(OPENTRACING_CAPABILITY_NAME)
            .addRequirements(WELD_CAPABILITY_NAME, MICROPROFILE_CONFIG_CAPABILITY_NAME)
            .build();

    public static final RuntimeCapability<Void> DEFAULT_TRACER_CAPABILITY = RuntimeCapability.Builder
            .of(DEFAULT_TRACER_CAPABILITY_NAME, false, TracerConfiguration.class)
            .build();

    public static final RuntimeCapability<Void> TRACER_CAPABILITY = RuntimeCapability.Builder
            .of(TRACER_CAPABILITY_NAME, true, TracerConfiguration.class)
            .build();

    static final String[] MODULES = {
        "io.opentracing.contrib.opentracing-tracerresolver",
        "io.opentracing.opentracing-api",
        "io.opentracing.opentracing-util",
        "org.eclipse.microprofile.config.api",
        "org.eclipse.microprofile.opentracing",
        "org.eclipse.microprofile.restclient",
        "io.opentracing.contrib.opentracing-jaxrs2",
    };

    static final String[] EXPORTED_MODULES = {
        "io.smallrye.opentracing",
        "org.wildfly.microprofile.opentracing-smallrye",
        "io.opentracing.contrib.opentracing-interceptors",
        };

    public static final SimpleAttributeDefinition DEFAULT_TRACER = SimpleAttributeDefinitionBuilder
            .create(TracerConfigurationConstants.DEFAULT_TRACER, ModelType.STRING, true)
            .setCapabilityReference(TRACER_CAPABILITY_NAME)
            .setRestartAllServices()
            .build();

    protected SubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(SubsystemExtension.SUBSYSTEM_PATH, SubsystemExtension.getResourceDescriptionResolver())
                .setAddHandler(SubsystemAdd.INSTANCE)
                .setRemoveHandler(new SubsystemRemoveHandler())
                .setCapabilities(OPENTRACING_CAPABILITY)
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new JaegerTracerConfigurationDefinition());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singletonList(DEFAULT_TRACER);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(DEFAULT_TRACER, null, new DefaultTracerWriteAttributeHandler());
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

    private static final class DefaultTracerWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

        private DefaultTracerWriteAttributeHandler() {
            super(DEFAULT_TRACER);
        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, AttributeDefinition attributeDefinition,
                ModelNode newValue, ModelNode oldValue) {
            if (oldValue.isDefined()) {
                context.deregisterCapability(DEFAULT_TRACER_CAPABILITY_NAME);
            }
            if (newValue.isDefined()) {
                context.registerCapability(DEFAULT_TRACER_CAPABILITY);
            }
        }
    }

    private static final class SubsystemRemoveHandler extends ReloadRequiredRemoveStepHandler {

        private SubsystemRemoveHandler() {
            super();
        }

        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            ModelNode defaultTracer = DEFAULT_TRACER.resolveModelAttribute(context, resource.getModel());
            if (defaultTracer.isDefined()) {
                context.deregisterCapability(DEFAULT_TRACER_CAPABILITY_NAME);
            }
        }
    }
}
