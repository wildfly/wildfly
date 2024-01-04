/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
