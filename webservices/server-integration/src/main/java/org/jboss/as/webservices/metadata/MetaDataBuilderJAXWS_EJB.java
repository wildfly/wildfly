/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.metadata;

import static org.jboss.as.webservices.util.ASHelper.getContextRoot;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class MetaDataBuilderJAXWS_EJB extends AbstractMetaDataBuilderEJB {

    @Override
    protected void buildEnterpriseBeansMetaData(final Deployment dep, final EJBArchiveMetaData.Builder ejbArchiveMDBuilder) {
        if (!WSHelper.isJaxwsJseDeployment(dep)) { // [AS7-1605] support
            final JBossWebMetaData jbossWebMD = WSHelper.getOptionalAttachment(dep, JBossWebMetaData.class);
            final String contextRoot = getContextRoot(dep, jbossWebMD);
            if (contextRoot != null) {
                final JSEArchiveMetaData.Builder jseArchiveMDBuilder = new JSEArchiveMetaData.Builder();
                jseArchiveMDBuilder.setContextRoot(contextRoot);
                dep.addAttachment(JSEArchiveMetaData.class, jseArchiveMDBuilder.build());
            }
        }

        final JAXWSDeployment jaxwsDeployment = WSHelper.getRequiredAttachment(dep, JAXWSDeployment.class);
        final List<EJBMetaData> wsEjbsMD = new LinkedList<EJBMetaData>();
        final JBossWebservicesMetaData jbossWebservicesMD = WSHelper.getOptionalAttachment(dep, JBossWebservicesMetaData.class);

        for (final EJBEndpoint jbossEjbMD : jaxwsDeployment.getEjbEndpoints()) {
            buildEnterpriseBeanMetaData(wsEjbsMD, jbossEjbMD, jbossWebservicesMD);
        }

        ejbArchiveMDBuilder.setEnterpriseBeans(wsEjbsMD);
    }

}
