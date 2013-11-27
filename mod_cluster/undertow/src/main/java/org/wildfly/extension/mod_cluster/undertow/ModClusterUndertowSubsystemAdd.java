/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster.undertow;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerService;
import org.wildfly.extension.mod_cluster.ModClusterExtension;
import org.wildfly.extension.mod_cluster.undertow.container.UndertowEventHandlerAdapter;
import org.wildfly.extension.mod_cluster.undertow.container.metric.MetricDeploymentProcessor;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Radoslav Husar
 * @version Dec 2013
 * @since 8.0
 */
public class ModClusterUndertowSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final ModClusterUndertowSubsystemAdd INSTANCE = new ModClusterUndertowSubsystemAdd();

    public static final ServiceName SERVICE_NAME = ContainerEventHandlerService.SERVICE_NAME.append("undertow");

    public ModClusterUndertowSubsystemAdd() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : ModClusterUndertowResourceDefinition.ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        // Register Metric deployment unit processor
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(ModClusterExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_UNDERTOW_MODCLUSTER, new MetricDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        // Install services for web container integration
        ServiceTarget target = context.getServiceTarget();
        final String connector = ModClusterUndertowResourceDefinition.LISTENER.resolveModelAttribute(context, model).asString();

        InjectedValue<ContainerEventHandler> eventHandler = new InjectedValue<>();
        InjectedValue<UndertowService> undertowService = new InjectedValue<>();
        InjectedValue<ListenerService> listener = new InjectedValue<>();

        newControllers.add(target.addService(SERVICE_NAME, new UndertowEventHandlerAdapter(eventHandler, undertowService, listener))
                .addDependency(ContainerEventHandlerService.SERVICE_NAME, ContainerEventHandler.class, eventHandler)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, undertowService)
                .addDependency(UndertowService.listenerName(connector), ListenerService.class, listener) // TODO from Tomaz: this is wrong, it should be replaced with injecting server instead of directly listener
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }

}
