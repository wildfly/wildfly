/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * JBossWS deployment model builder.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSDeploymentBuilder {

    private static final WSDeploymentBuilder SINGLETON = new WSDeploymentBuilder();
    private static final DeploymentModelBuilder JAXWS_JSE = new DeploymentModelBuilderJAXWS_POJO();
    private static final DeploymentModelBuilder JAXWS_EJB = new DeploymentModelBuilderJAXWS_EJB();
    private static final DeploymentModelBuilder JAXWS_JMS = new DeploymentModelBuilderJAXWS_JMS();

    private WSDeploymentBuilder() {
        super();
    }

    public static WSDeploymentBuilder getInstance() {
        return WSDeploymentBuilder.SINGLETON;
    }

    public void build(final DeploymentUnit unit) {
        if (isJaxwsPojoDeployment(unit)) {
            WSLogger.ROOT_LOGGER.trace("Detected JAXWS POJO deployment");
            JAXWS_JSE.newDeploymentModel(unit);
        }
        if (isJaxwsJmsDeployment(unit)) {
            WSLogger.ROOT_LOGGER.trace("Detected JAXWS JMS deployment");
            JAXWS_JMS.newDeploymentModel(unit);
        }
        if (isJaxwsEjbDeployment(unit)) {
            WSLogger.ROOT_LOGGER.trace("Detected JAXWS EJB deployment");
            JAXWS_EJB.newDeploymentModel(unit);
        }
    }

    private static boolean isJaxwsPojoDeployment(final DeploymentUnit unit) {
        return !getJaxwsPojos(unit).isEmpty();
    }

    private static boolean isJaxwsEjbDeployment(final DeploymentUnit unit) {
        return !getJaxwsEjbs(unit).isEmpty();
    }

    private static boolean isJaxwsJmsDeployment(final DeploymentUnit unit) {
        final JMSEndpointsMetaData jmsEndpointsMD = getOptionalAttachment(unit, JMS_ENDPOINT_METADATA_KEY);
        if (jmsEndpointsMD != null) {
            return !jmsEndpointsMD.getEndpointsMetaData().isEmpty();
        }
        return false;
    }

}
