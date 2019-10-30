/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
