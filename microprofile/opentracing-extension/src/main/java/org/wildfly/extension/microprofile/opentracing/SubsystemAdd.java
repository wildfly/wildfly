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

import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.DEFAULT_TRACER;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.DEFAULT_TRACER_CAPABILITY;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.OPENTRACING_CAPABILITY;
import static org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory.ENV_TRACER;
import static org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory.TRACER_CAPABILITY_NAME;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.opentracing.resolver.JaegerEnvTracerConfiguration;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;
import org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory;

/**
 * OSH for adding the OpneTracing subsystem.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
class SubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final SubsystemAdd INSTANCE = new SubsystemAdd();

    private SubsystemAdd() {
        super(DEFAULT_TRACER);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        WildFlyTracerFactory.registerTracer(ENV_TRACER).accept(new JaegerEnvTracerConfiguration());
        TracingExtensionLogger.ROOT_LOGGER.activatingSubsystem();String defaultTracer = DEFAULT_TRACER.resolveModelAttribute(context, operation).asStringOrNull();
        if (defaultTracer != null && !defaultTracer.isEmpty()) {
            CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(DEFAULT_TRACER_CAPABILITY);
            final Supplier<TracerConfiguration> config = builder.requiresCapability(TRACER_CAPABILITY_NAME, TracerConfiguration.class, defaultTracer);
            final Consumer<TracerConfiguration> injector = builder.provides(DEFAULT_TRACER_CAPABILITY);
            builder.setInstance(new Service() {
                @Override
                public void start(StartContext context) throws StartException {
                    injector.accept(config.get());
                    WildFlyTracerFactory.registerDefaultTracer().accept(config.get());
                }

                @Override
                public void stop(StopContext context) {
                    injector.accept(null);
                    WildFlyTracerFactory.registerDefaultTracer().accept(config.get());
                }
            }).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(
                        SubsystemExtension.SUBSYSTEM_NAME,
                        Phase.DEPENDENCIES,
                        Phase.DEPENDENCIES_MICROPROFILE_OPENTRACING,
                        new TracingDependencyProcessor()
                );
                processorTarget.addDeploymentProcessor(SubsystemExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE,
                        Phase.POST_MODULE_MICROPROFILE_OPENTRACING,
                        new TracingDeploymentProcessor()
                );
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        context.registerCapability(OPENTRACING_CAPABILITY);
        ModelNode defaultTracer = DEFAULT_TRACER.resolveModelAttribute(context, operation);
        if (defaultTracer.isDefined()) {
            context.registerCapability(DEFAULT_TRACER_CAPABILITY);
        }
    }
}
