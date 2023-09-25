/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.tomcat;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WebMetaDataModifyingDeploymentAspect extends AbstractDeploymentAspect {

    private WebMetaDataModifier webMetaDataModifier = new WebMetaDataModifier();

    @Override
    public void start(final Deployment dep) {
        if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
            WSLogger.ROOT_LOGGER.tracef("Modifying web meta data for webservice deployment: %s", dep.getSimpleName());
        }
        webMetaDataModifier.modify(dep);
    }

}
