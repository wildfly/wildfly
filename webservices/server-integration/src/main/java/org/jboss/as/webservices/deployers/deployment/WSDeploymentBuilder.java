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
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.util.ASHelper.getJaxrpcEjbs;
import static org.jboss.as.webservices.util.ASHelper.getJaxrpcPojos;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * JBossWS deployment model builder.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSDeploymentBuilder {

    private static final Logger log = Logger.getLogger(WSDeploymentBuilder.class);
    private static final WSDeploymentBuilder SINGLETON = new WSDeploymentBuilder();
    private static final DeploymentModelBuilder JAXWS_JSE = new DeploymentModelBuilderJAXWS_POJO();
    private static final DeploymentModelBuilder JAXWS_EJB = new DeploymentModelBuilderJAXWS_EJB();
    private static final DeploymentModelBuilder JAXWS_JMS = new DeploymentModelBuilderJAXWS_JMS();
    private static final DeploymentModelBuilder JAXRPC_JSE = new DeploymentModelBuilderJAXRPC_POJO();
    private static final DeploymentModelBuilder JAXRPC_EJB = new DeploymentModelBuilderJAXRPC_EJB();

    /**
     * Constructor.
     */
    private WSDeploymentBuilder() {
        super();
    }

    /**
     * Factory method for obtaining builder instance.
     * @return builder instance
     */
    public static WSDeploymentBuilder getInstance() {
        return WSDeploymentBuilder.SINGLETON;
    }

    /**
     * Builds JBossWS deployment model if web service deployment is detected.
     * @param unit deployment unit
     */
    public void build(final DeploymentUnit unit) {
        boolean isJaxwsDeployment = false;
        if (isJaxwsPojoDeployment(unit)) {
            log.debug("Detected JAXWS JSE deployment");
            JAXWS_JSE.newDeploymentModel(unit);
            isJaxwsDeployment = true;
        }
        if (isJaxwsJmsDeployment(unit)) {
            log.debug("Detected JAXWS JMS deployment");
            JAXWS_JMS.newDeploymentModel(unit);
            isJaxwsDeployment = true;
        }
        if (isJaxwsEjbDeployment(unit)) {
            log.debug("Detected JAXWS EJB3 deployment");
            JAXWS_EJB.newDeploymentModel(unit);
            isJaxwsDeployment = true;
        }
        if (!isJaxwsDeployment && isJaxrpcPojoDeployment(unit)) {
            log.debug("Detected JAXRPC JSE deployment");
            JAXRPC_JSE.newDeploymentModel(unit);
        }
        if (!isJaxwsDeployment && isJaxrpcEjbDeployment(unit)) {
            log.debug("Detected JAXRPC EJB21 deployment");
            JAXRPC_EJB.newDeploymentModel(unit);
        }
    }

    private static boolean isJaxwsPojoDeployment(final DeploymentUnit unit) {
        return getJaxwsPojos(unit).size() > 0;
    }

    private static boolean isJaxwsEjbDeployment(final DeploymentUnit unit) {
        return getJaxwsEjbs(unit).size() > 0;
    }

    private static boolean isJaxrpcPojoDeployment(final DeploymentUnit unit) {
        return getJaxrpcPojos(unit).size() > 0;
    }

    private static boolean isJaxrpcEjbDeployment(final DeploymentUnit unit) {
        return getJaxrpcEjbs(unit).size() > 0;
    }

    private static boolean isJaxwsJmsDeployment(final DeploymentUnit unit) {
        final JMSEndpointsMetaData jmsEndpointsMD = getOptionalAttachment(unit, JMS_ENDPOINT_METADATA_KEY);
        if (jmsEndpointsMD != null) {
            return jmsEndpointsMD.getEndpointsMetaData().size() > 0;
        }
        return false;
    }

}
