/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.EJBClientContextService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.discovery.Discovery;

/**
 * A deployment processor which associates the {@link EJBClientContext}, belonging to a deployment unit,
 * with the deployment unit's classloader.
 *
 * @author Stuart Douglas
 * @author Jaikiran Pai
 */
public class EjbClientContextSetupProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }

        RegistrationService registrationService = new RegistrationService(module);
        ServiceName registrationServiceName = deploymentUnit.getServiceName().append("ejb3","client-context","registration-service");
        final ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(registrationServiceName, registrationService)
            .addDependency(getEJBClientContextServiceName(phaseContext), EJBClientContext.class, registrationService.ejbClientContextInjectedValue)
            .addDependency(getDiscoveryServiceName(phaseContext), Discovery.class, registrationService.discoveryInjector);
        builder.install();


        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        //we need to make sure all our components have a dependency on the EJB client context registration, which in turn implies a dependency on the context
        for(final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            component.addDependency(registrationServiceName, ServiceBuilder.DependencyType.REQUIRED);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
    }

    private ServiceName getEJBClientContextServiceName(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parentDeploymentUnit = deploymentUnit.getParent();
        // The top level parent deployment unit will have the attachment containing the EJB client context
        // service name
        ServiceName serviceName;
        if (parentDeploymentUnit != null) {
            serviceName = parentDeploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME);
        } else {
            serviceName = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME);
        }
        if (serviceName != null) {
            return serviceName;
        }
        return EJBClientContextService.DEFAULT_SERVICE_NAME;
    }

    private ServiceName getDiscoveryServiceName(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parentDeploymentUnit = deploymentUnit.getParent();
        if (parentDeploymentUnit != null) {
            return DiscoveryService.BASE_NAME.append(parentDeploymentUnit.getName());
        } else {
            return DiscoveryService.BASE_NAME.append(deploymentUnit.getName());
        }
    }

    private static final class RegistrationService implements Service<Void> {

        private final Module module;

        final InjectedValue<EJBClientContext> ejbClientContextInjectedValue = new InjectedValue<>();
        final InjectedValue<Discovery> discoveryInjector = new InjectedValue<>();

        private RegistrationService(Module module) {
            this.module = module;
        }

        @Override
        public void start(StartContext context) throws StartException {

            doPrivileged((PrivilegedAction<Void>) () -> {
                // associate the EJB client context and discovery setup with the deployment classloader
                final EJBClientContext ejbClientContext = ejbClientContextInjectedValue.getValue();
                final ModuleClassLoader classLoader = module.getClassLoader();
                EjbLogger.DEPLOYMENT_LOGGER.debugf("Registering EJB client context %s for classloader %s", ejbClientContext, classLoader);
                EJBClientContext.getContextManager().setClassLoaderDefault(classLoader, ejbClientContext);
                Discovery.getContextManager().setClassLoaderDefault(classLoader, discoveryInjector.getValue());
                return null;
            });
        }

        @Override
        public void stop(StopContext context) {
            // de-associate the EJB client context with the deployment classloader
            doPrivileged((PrivilegedAction<Void>) () -> {
                final ModuleClassLoader classLoader = module.getClassLoader();
                EjbLogger.DEPLOYMENT_LOGGER.debugf("unRegistering EJB client context for classloader %s", classLoader);
                EJBClientContext.getContextManager().setClassLoaderDefault(classLoader, null);
                Discovery.getContextManager().setClassLoaderDefault(classLoader, null);
                return null;
            });
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }
}
