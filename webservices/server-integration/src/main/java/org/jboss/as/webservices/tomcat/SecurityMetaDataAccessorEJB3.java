/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.tomcat;

import java.util.List;

import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * Creates web app security meta data for EJB 3 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class SecurityMetaDataAccessorEJB3 extends AbstractSecurityMetaDataAccessorEJB {

    @Override
    protected List<EJBEndpoint> getEjbEndpoints(final Deployment dep) {
        final JAXWSDeployment jaxwsDeployment = WSHelper.getRequiredAttachment(dep, JAXWSDeployment.class);
        return jaxwsDeployment.getEjbEndpoints();
    }

}
