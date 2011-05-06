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

import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;

/**
 * An aspect that builds container independent meta data.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class ContainerMetaDataDeploymentAspect extends AbstractDeploymentAspect {
    /** JSE meta data builder. */
    private final MetaDataBuilderJSE metaDataBuilderJSE = new MetaDataBuilderJSE();

    /** EJB3 meta data builder. */
    private final MetaDataBuilderEJB3 metaDataBuilderEJB3 = new MetaDataBuilderEJB3();
// TODO
//   /** EJB21 meta data builder. */
//   private final MetaDataBuilderEJB21 metaDataBuilderEJB21 = new MetaDataBuilderEJB21();

    /**
     * Constructor.
     */
    public ContainerMetaDataDeploymentAspect() {
        super();
    }

    /**
     * Build container independent meta data.
     *
     * @param dep webservice deployment
     */
    @Override
    public void start(final Deployment dep) {
        if (WSHelper.isJseDeployment(dep)) {
            this.log.debug("Creating JBoss agnostic JSE meta data for deployment: " + dep.getSimpleName());
            final JSEArchiveMetaData jseMetaData = this.metaDataBuilderJSE.create(dep);
            dep.addAttachment(JSEArchiveMetaData.class, jseMetaData);
        }
        else if (WSHelper.isJaxwsEjbDeployment(dep)) {
            this.log.debug("Creating JBoss agnostic EJB3 meta data for deployment: " + dep.getSimpleName());
            final EJBArchiveMetaData ejbMetaData = this.metaDataBuilderEJB3.create(dep);
            dep.addAttachment(EJBArchiveMetaData.class, ejbMetaData);
        }
//      else if (WSHelper.isJaxrpcEjbDeployment(dep))
//      {
//         this.log.debug("Creating JBoss agnostic EJB21 meta data for deployment: " + dep.getSimpleName());
//         final EJBArchiveMetaData ejbMetaData = this.metaDataBuilderEJB21.create(dep);
//         dep.addAttachment(EJBArchiveMetaData.class, ejbMetaData);
//      }
   }
}
