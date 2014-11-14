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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.remote.DefaultEjbClientContextService;
import org.jboss.as.ejb3.remote.TCCLEJBClientContextSelectorService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A deployment processor which associates the {@link EJBClientContext}, belonging to a deployment unit,
 * with the deployment unit's classloader, so that the {@link org.jboss.as.ejb3.remote.TCCLEJBClientContextSelectorService} can then
 * be used to return an appropriate {@link EJBClientContext} based on the classloader.
 *
 * @author Stuart Douglas
 * @author Jaikiran Pai
 */
public class EjbClientContextSetupProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EjbClientContextSetupProcessor.class);


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            return;
        }

        RegistractionService registractionService = new RegistractionService(module);
        ServiceName registrationServiceName = deploymentUnit.getServiceName().append("ejb3","client-context","registration-service");
        phaseContext.getServiceTarget().addService(registrationServiceName, registractionService)
                .addDependency(getEJBClientContextServiceName(phaseContext), EJBClientContext.class, registractionService.ejbClientContextInjectedValue)
                .addDependency(TCCLEJBClientContextSelectorService.TCCL_BASED_EJB_CLIENT_CONTEXT_SELECTOR_SERVICE_NAME, TCCLEJBClientContextSelectorService.class, registractionService.tcclEJBClientContextSelectorServiceController)
                .install();


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
        return DefaultEjbClientContextService.DEFAULT_SERVICE_NAME;
    }

    private static final class RegistractionService implements Service<Void> {

        private final Module module;

        final InjectedValue<TCCLEJBClientContextSelectorService> tcclEJBClientContextSelectorServiceController = new InjectedValue<>();
        final InjectedValue<EJBClientContext> ejbClientContextInjectedValue = new InjectedValue<>();

        private RegistractionService(Module module) {
            this.module = module;
        }

        @Override
        public void start(StartContext context) throws StartException {

            final EJBClientContext ejbClientContext = ejbClientContextInjectedValue.getValue();
            final TCCLEJBClientContextSelectorService tcclBasedEJBClientContextSelector = tcclEJBClientContextSelectorServiceController.getValue();
            // associate the EJB client context with the deployment classloader
            logger.debugf("Registering EJB client context %s for classloader %s", ejbClientContext, module.getClassLoader());
            tcclBasedEJBClientContextSelector.registerEJBClientContext(ejbClientContext, module.getClassLoader());
        }

        @Override
        public void stop(StopContext context) {
            final TCCLEJBClientContextSelectorService tcclBasedEJBClientContextSelector = tcclEJBClientContextSelectorServiceController.getValue();
            // de-associate the EJB client context with the deployment classloader
            logger.debugf("unRegistering EJB client context for classloader %s", module.getClassLoader());
            tcclBasedEJBClientContextSelector.unRegisterEJBClientContext(module.getClassLoader());
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }
}
