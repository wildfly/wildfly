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
package org.jboss.as.webservices.metadata;

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;

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

    private final MetaDataBuilderJAXRPC_POJO jaxrpcPojoMDBuilder = new MetaDataBuilderJAXRPC_POJO();

    private final MetaDataBuilderJAXRPC_EJB jaxrpcEjbMDBuilder = new MetaDataBuilderJAXRPC_EJB();

    @Override
    public void start(final Deployment dep) {
        if (WSHelper.isJaxwsJseDeployment(dep)) {
            if (WSHelper.hasAttachment(dep, JBossWebMetaData.class)) {
                ROOT_LOGGER.creatingDeployment("JAXWS", "POJO", dep.getSimpleName());
                final JSEArchiveMetaData jseMetaData = jaxwsPojoMDBuilder.create(dep);
                dep.addAttachment(JSEArchiveMetaData.class, jseMetaData);
            }
        }
        if (WSHelper.isJaxwsEjbDeployment(dep)) {
            ROOT_LOGGER.creatingDeployment("JAXWS", "EJB", dep.getSimpleName());
            final EJBArchiveMetaData ejbMetaData = jaxwsEjbMDBuilder.create(dep);
            dep.addAttachment(EJBArchiveMetaData.class, ejbMetaData);
        }
        else if (WSHelper.isJaxrpcJseDeployment(dep)) {
            ROOT_LOGGER.creatingDeployment("JAXRPC", "POJO", dep.getSimpleName());
            final JSEArchiveMetaData jseMetaData = jaxrpcPojoMDBuilder.create(dep);
            dep.addAttachment(JSEArchiveMetaData.class, jseMetaData);
        }
        else if (WSHelper.isJaxrpcEjbDeployment(dep)) {
            ROOT_LOGGER.creatingDeployment("JAXRPC", "EJB", dep.getSimpleName());
            final EJBArchiveMetaData ejbMetaData = jaxrpcEjbMDBuilder.create(dep);
            dep.addAttachment(EJBArchiveMetaData.class, ejbMetaData);
        }
    }

}
