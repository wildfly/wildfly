/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.webservices.deployers.deployment.WSDeploymentBuilder;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.wsf.spi.deployment.Endpoint.EndpointType;

/**
 * Detects Web Service deployment type.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSTypeDeploymentProcessor extends TCCLDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void internalDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (this.isJaxwsJseDeployment(unit)) {
            //JAXWS_JSE and JAXWS_JMS type endpoint is built with same DeploymentBuilder
            WSDeploymentBuilder.getInstance().build(unit, EndpointType.JAXWS_JSE);
        }
        if (this.isJaxwsEjbDeployment(unit)) {
            WSDeploymentBuilder.getInstance().build(unit, EndpointType.JAXWS_EJB3);
        }
        //TODO: look at if this two jaxprc types can be in one archive and deployed
        if (this.isJaxrpcJseDeployment(unit) && !isJaxwsJseDeployment(unit) && !isJaxwsEjbDeployment(unit)) {
            WSDeploymentBuilder.getInstance().build(unit, EndpointType.JAXRPC_JSE);
        }
        if (this.isJaxrpcEjbDeployment(unit)  && !isJaxwsJseDeployment(unit) && !isJaxwsEjbDeployment(unit)) {
            WSDeploymentBuilder.getInstance().build(unit, EndpointType.JAXRPC_EJB21);
        }
    }

    @Override
    public void internalUndeploy(DeploymentUnit context) {
        //NOOP
    }

    /**
     * Returns true if JAXRPC EJB deployment is detected.
     *
     * @param unit deployment unit
     * @return true if JAXRPC EJB, false otherwise
     */
    private boolean isJaxrpcEjbDeployment(final DeploymentUnit unit) {
        //TODO
//        final boolean hasWebservicesMD = ASHelper.hasAttachment(unit, AttachmentKeys.WEBSERVICES_METADATA_KEY);
//        final boolean hasJBossMD = unit.getAllMetaData(JBossMetaData.class).size() > 0;
//
//        return hasWebservicesMD && hasJBossMD;
        return false;
    }

    /**
     * Returns true if JAXRPC JSE deployment is detected.
     *
     * @param unit deployment unit
     * @return true if JAXRPC JSE, false otherwise
     */
    private boolean isJaxrpcJseDeployment(final DeploymentUnit unit) {
        final boolean hasWebservicesMD = ASHelper.hasAttachment(unit, WSAttachmentKeys.WEBSERVICES_METADATA_KEY);
        final boolean hasJBossWebMD = ASHelper.getJBossWebMetaData(unit) != null;

        if (hasWebservicesMD && hasJBossWebMD) {
            return ASHelper.getJaxrpcServlets(unit).size() > 0;
        }

        return false;
    }

    /**
     * Returns true if JAXWS EJB deployment is detected.
     *
     * @param unit deployment unit
     * @return true if JAXWS EJB, false otherwise
     */
    private boolean isJaxwsEjbDeployment(final DeploymentUnit unit) {
        return ASHelper.hasAttachment(unit, WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY);
    }

    /**
     * Returns true if JAXWS JSE deployment is detected.
     *
     * @param unit deployment unit
     * @return true if JAXWS JSE, false otherwise
     */
    private boolean isJaxwsJseDeployment(final DeploymentUnit unit) {
        final boolean hasWarMetaData = ASHelper.hasAttachment(unit, WarMetaData.ATTACHMENT_KEY);
        if (hasWarMetaData) {
            //once the deployment is a WAR, the endpoint(s) can be on either http (servlet) transport or jms transport
            return ASHelper.getJaxwsServlets(unit).size() > 0 || ASHelper.hasAttachment(unit, WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY);
        } else {
            //otherwise the (JAR) deployment can be a jaxws_jse one if there're jms transport endpoints only (no ejb3)
            return !ASHelper.hasAttachment(unit, WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY) &&
                    ASHelper.hasAttachment(unit, WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY);
        }
    }
}
