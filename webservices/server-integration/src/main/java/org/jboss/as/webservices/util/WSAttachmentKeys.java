/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.util;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Collection of attachment keys
 *
 * @author alessio.soldano@jboss.com
 * @since 11-Nov-2010
 */
public final class WSAttachmentKeys {

    public static final AttachmentKey<Deployment> DEPLOYMENT_KEY = AttachmentKey.create(Deployment.class);
    public static final AttachmentKey<DeploymentType> DEPLOYMENT_TYPE_KEY = AttachmentKey.create(DeploymentType.class);
    public static final AttachmentKey<JBossAppMetaData> JBOSS_APP_METADATA_KEY = AttachmentKey.create(JBossAppMetaData.class);
    public static final AttachmentKey<JMSEndpointsMetaData> JMS_ENDPOINT_METADATA_KEY = AttachmentKey.create(JMSEndpointsMetaData.class);
    public static final AttachmentKey<WebServiceDeployment> WEBSERVICE_DEPLOYMENT_KEY = AttachmentKey.create(WebServiceDeployment.class);
    public static final AttachmentKey<WebservicesMetaData> WEBSERVICES_METADATA_KEY = AttachmentKey.create(WebservicesMetaData.class);

    private WSAttachmentKeys() {
        // forbidden inheritance
    }

}
