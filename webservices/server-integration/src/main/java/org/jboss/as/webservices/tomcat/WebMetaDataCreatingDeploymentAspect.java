/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.tomcat;

import static org.jboss.ws.common.integration.WSHelper.isEjbDeployment;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WebMetaDataCreatingDeploymentAspect extends AbstractDeploymentAspect {

    private WebMetaDataCreator webMetaDataCreator = new WebMetaDataCreator();

    @Override
    public void start(final Deployment dep) {
        if (isEjbDeployment(dep)) {
            if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
                WSLogger.ROOT_LOGGER.tracef("Creating web meta data for EJB webservice deployment: %s", dep.getSimpleName());
            }
            webMetaDataCreator.create(dep);
        }
    }

}
