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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

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
        final EJBClientContext ejbClientContext = this.getEJBClientContext(phaseContext);
        final ServiceController<TCCLEJBClientContextSelectorService> tcclEJBClientContextSelectorServiceController = (ServiceController<TCCLEJBClientContextSelectorService>) phaseContext.getServiceRegistry().getService(TCCLEJBClientContextSelectorService.TCCL_BASED_EJB_CLIENT_CONTEXT_SELECTOR_SERVICE_NAME);
        if (tcclEJBClientContextSelectorServiceController != null) {
            final TCCLEJBClientContextSelectorService tcclBasedEJBClientContextSelector = tcclEJBClientContextSelectorServiceController.getValue();
            // associate the EJB client context with the deployment classloader
            logger.debug("Registering EJB client context " + ejbClientContext + " for classloader " + module.getClassLoader());
            tcclBasedEJBClientContextSelector.registerEJBClientContext(ejbClientContext, module.getClassLoader());
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

    private EJBClientContext getEJBClientContext(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parentDeploymentUnit = deploymentUnit.getParent();
        final EJBClientContext ejbClientContext;
        // The top level parent deployment unit will have the attachment containing the EJB client context
        // service name
        if (parentDeploymentUnit != null) {
            ejbClientContext = parentDeploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT);
        } else {
            ejbClientContext = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT);
        }
        if (ejbClientContext != null) {
            return ejbClientContext;
        }
        logger.debug("Deployment unit " + deploymentUnit + " doesn't have any explicit EJB client context configured. " +
                "Falling back on the default " + DefaultEjbClientContextService.DEFAULT_SERVICE_NAME + " EJB client context service");
        final ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
        final ServiceController<EJBClientContext> ejbClientContextServiceController = (ServiceController<EJBClientContext>) serviceRegistry.getRequiredService(DefaultEjbClientContextService.DEFAULT_SERVICE_NAME);
        return ejbClientContextServiceController.getValue();
    }

}
