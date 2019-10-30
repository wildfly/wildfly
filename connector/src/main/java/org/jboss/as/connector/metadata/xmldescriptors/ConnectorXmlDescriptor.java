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

package org.jboss.as.connector.metadata.xmldescriptors;

import java.io.File;
import java.io.Serializable;
import java.net.URL;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.jca.common.api.metadata.spec.Connector;

/**
 * A ConnectorXmlDescriptor.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 */
public final class ConnectorXmlDescriptor implements Serializable {

    private static final long serialVersionUID = 3148478338698997486L;

    public static final AttachmentKey<ConnectorXmlDescriptor> ATTACHMENT_KEY = AttachmentKey
            .create(ConnectorXmlDescriptor.class);

    private final Connector connector;
    private final File root;
    private final URL url;
    private final String deploymentName;

    /**
     * Create a new ConnectorXmlDescriptor.
     * @param connector
     */
    public ConnectorXmlDescriptor(Connector connector, File root, URL url, String deploymentName) {
        super();
        this.connector = connector;
        this.root = root;
        this.url = url;
        this.deploymentName = deploymentName;

    }

    /**
     * Get the connector.
     * @return the connector.
     */
    public Connector getConnector() {
        return connector;
    }

    /**
     * get file root of this deployment
     * @return the root directory
     */
    public File getRoot() {
        return root;
    }

    /**
     * get url for this deployment
     * @return the url of deployment
     */
    public URL getUrl() {
        return url;
    }

    /**
     * return this deployment name
     * @return the deployment name
     */
    public String getDeploymentName() {
        return deploymentName;
    }
}
