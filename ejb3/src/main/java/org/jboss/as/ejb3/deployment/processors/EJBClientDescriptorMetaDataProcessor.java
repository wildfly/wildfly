/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.remote.DefaultEjbClientContextService;
import org.jboss.as.ejb3.remote.DescriptorBasedEJBClientContextService;
import org.jboss.as.remoting.AbstractOutboundConnectionService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A deployment unit processor which processing only top level deployment units and checks for the presence
 * of a {@link Attachments#EJB_CLIENT_METADATA} key corresponding to {@link EJBClientDescriptorMetaData}, in the
 * deployment unit.
 * <p/>
 * If a {@link EJBClientDescriptorMetaData} is available then this deployment unit processor
 * creates and installs a {@link DescriptorBasedEJBClientContextService}. It then attaches the
 * {@link ServiceName name of the installed service} to the deployment unit at {@link EjbDeploymentAttachmentKeys#EJB_CLIENT_CONTEXT_SERVICE_NAME}
 * <p/>
 * If the deployment unit doesn't have a {@link EJBClientDescriptorMetaData} then this processors attaches
 * {@link DefaultEjbClientContextService#DEFAULT_SERVICE_NAME} as the {@link EjbDeploymentAttachmentKeys#EJB_CLIENT_CONTEXT_SERVICE_NAME}
 *
 * @author Jaikiran Pai
 */
public class EJBClientDescriptorMetaDataProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EJBClientDescriptorMetaDataProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // we only process top level deployment units
        if (deploymentUnit.getParent() != null) {
            return;
        }
        final EJBClientDescriptorMetaData ejbClientDescriptorMetaData = deploymentUnit.getAttachment(Attachments.EJB_CLIENT_METADATA);
        if (ejbClientDescriptorMetaData == null) {
            logger.debug("Deployment unit " + deploymentUnit + " doesn't have any EJB client descriptor metadata associated. " +
                    "Falling back on the default " + DefaultEjbClientContextService.DEFAULT_SERVICE_NAME + " EJB client context service");
            // no client descriptor, so fallback to default EJB client context service
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME, DefaultEjbClientContextService.DEFAULT_SERVICE_NAME);
            return;
        }
        // install the descriptor based EJB client context service
        final ServiceName ejbClientContextServiceName = DescriptorBasedEJBClientContextService.BASE_SERVICE_NAME.append(deploymentUnit.getName());
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        // create the service
        final DescriptorBasedEJBClientContextService service = new DescriptorBasedEJBClientContextService();
        // add the service
        final ServiceBuilder serviceBuilder = serviceTarget.addService(ejbClientContextServiceName, service);
        // add the remoting connection reference dependencies
        for (final String connectionRef : ejbClientDescriptorMetaData.getRemotingReceiverConnectionRefs()) {
            final ServiceName connectionDependencyService = AbstractOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionRef);
            service.addRemotingConnectionDependency(serviceBuilder, connectionDependencyService);
        }
        // install the service
        serviceBuilder.install();
        logger.debug("Deployment unit " + deploymentUnit + " will use " + ejbClientContextServiceName + " as the EJB client context service");

        // attach the service name of this EJB client context to the deployment unit
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME, ejbClientContextServiceName);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
