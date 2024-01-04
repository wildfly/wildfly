/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.metadata;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;

/**
 * An aspect that builds container independent meta data.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ContainerMetaDataDeploymentAspect extends AbstractDeploymentAspect {

    private final MetaDataBuilderJAXWS_POJO jaxwsPojoMDBuilder = new MetaDataBuilderJAXWS_POJO();

    private final MetaDataBuilderJAXWS_EJB jaxwsEjbMDBuilder = new MetaDataBuilderJAXWS_EJB();

    @Override
    public void start(final Deployment dep) {
        if (WSHelper.isJaxwsJseDeployment(dep)
                && WSHelper.hasAttachment(dep, JBossWebMetaData.class)) {
            if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
                WSLogger.ROOT_LOGGER.tracef("Creating JBoss agnostic JAXWS POJO meta data for deployment: %s",
                        dep.getSimpleName());
            }
            final JSEArchiveMetaData jseMetaData = jaxwsPojoMDBuilder.create(dep);
            dep.addAttachment(JSEArchiveMetaData.class, jseMetaData);
        }
        if (WSHelper.isJaxwsEjbDeployment(dep)) {
            if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
                WSLogger.ROOT_LOGGER.tracef("Creating JBoss agnostic JAXWS EJB meta data for deployment: %s", dep.getSimpleName());
            }
            final EJBArchiveMetaData ejbMetaData = jaxwsEjbMDBuilder.create(dep);
            dep.addAttachment(EJBArchiveMetaData.class, ejbMetaData);
        }
    }

}
