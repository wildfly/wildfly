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
package org.jboss.as.osgi.naming;

import java.util.List;

import javax.naming.spi.ObjectFactory;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.InitialContextFactoryBuilder;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.osgi.AbstractSubsystemExtension;
import org.jboss.as.osgi.parser.OSGiExtension;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.IntegrationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

/**
 * A Naming extension to the OSGi subsystem
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Jul-2012
 */
public class NamingExtension extends AbstractSubsystemExtension {

    private final InjectedValue<NamingStore> injectedNamingStore = new InjectedValue<NamingStore>();
    private JNDIServiceListener jndiServiceListener;

    @Override
    public void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(BundleContextBindingService.addService(serviceTarget));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(OSGiExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_BUNDLE_CONTEXT_BINDING, new BundleContextBindingProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    public void configureServiceDependencies(ServiceName serviceName, ServiceBuilder<?> builder) {
        if (serviceName.equals(IntegrationService.SYSTEM_SERVICES_PLUGIN)) {
            builder.addDependency(DependencyType.OPTIONAL, NamingService.SERVICE_NAME, NamingStore.class, injectedNamingStore);
        }
    }

    @Override
    public void startSystemServices(StartContext startContext, BundleContext systemContext) {
        NamingStore namingStore = injectedNamingStore.getOptionalValue();
        if (namingStore != null) {
            // Register the {@link InitialContextFactoryBuilder} service
            String[] classes = new String[] {javax.naming.spi.InitialContextFactoryBuilder.class.getName(), InitialContextFactoryBuilder.class.getName()};
            systemContext.registerService(classes, new InitialContextFactoryBuilder(), null);

            // Register the JNDI service listener
            jndiServiceListener = new JNDIServiceListener(systemContext);
            try {
                String filter = "(" + Constants.OBJECTCLASS + "=" + ObjectFactory.class.getName() + ")";
                systemContext.addServiceListener(jndiServiceListener, filter);
            } catch (InvalidSyntaxException e) {
                // ignore
            }
        }
    }

    @Override
    public void stopSystemServices(StopContext stopContext, BundleContext systemContext) {
        if (jndiServiceListener != null) {
            systemContext.removeServiceListener(jndiServiceListener);
        }
    }
}
