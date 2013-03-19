/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.web;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.AbstractSubsystemExtension;
import org.jboss.as.osgi.parser.OSGiExtension;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.framework.spi.FrameworkBuilder;
import org.jboss.osgi.framework.spi.FrameworkBuilder.FrameworkPhase;
import org.jboss.osgi.framework.spi.IntegrationServices;

/**
 * A WebApp extension to the OSGi subsystem
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 11-Jul-2012
 */
public class WebExtension extends AbstractSubsystemExtension {

    public static final String OSGI_BUNDLECONTEXT = "osgi-bundlecontext";
    public static final String WEB_CONTEXT_PATH = "Web-ContextPath";
    public static final String WEBBUNDLE_PREFIX = "webbundle://";

    @Override
    public void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(WebContextLifecycleInterceptor.addService(serviceTarget, verificationHandler));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_OSGI_WEBBUNDLE, new WebBundleStructureProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REMOUNT_EXPLODED, new RemountDeploymentRootProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WAB_CONTEXT_FACTORY, new WebBundleContextProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WAB_FRAGMENTS, new WebBundleFragmentProcessor());
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WAB_DEPLOYMENT, new WebContextActivationProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    public void registerIntegrationServices(FrameworkBuilder builder, SubsystemState subsystemState) {
        builder.registerIntegrationService(FrameworkPhase.CREATE, new DeploymentProviderIntegration());
    }

    @Override
    public void configureServiceDependencies(ServiceName serviceName, ServiceBuilder<?> builder) {
        if (serviceName.equals(IntegrationServices.SYSTEM_SERVICES_PLUGIN)) {
            builder.addDependency(WebContextLifecycleInterceptor.SERVICE_NAME);
        }
    }
}
