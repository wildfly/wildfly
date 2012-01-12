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

package org.jboss.as.server.deployment.scanner.api;

import org.jboss.msc.service.ServiceName;

/**
 * The deployment scanner.
 *
 * @author Emanuel Muckenhuber
 */
public interface DeploymentScanner {

    ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "deployment", "scanner");

    /**
     * Check whether the scanner is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Get the current scan interval
     *
     * @return the scan interval in ms
     */
    long getScanInterval();

    /**
     * Set the scan interval.
     *
     * @param scanInterval the scan interval in ms
     */
    void setScanInterval(long scanInterval);

    /**
     * Start the scanner, if not already started.
     */
    void startScanner();

    /**
     * Stop the scanner, if not already stopped.
     */
    void stopScanner();

    /**
     * Gets whether the scanner will attempt to deploy zipped content based solely
     * on detecting a change in the content; i.e. without requiring a
     * marker file to trigger the deployment.
     *
     * @return true if auto-deployment of zipped content is enabled
     */
    boolean isAutoDeployZippedContent();

    /**
     * Sets whether the scanner will attempt to deploy zipped content based solely
     * on detecting a change in the content; i.e. without requiring a
     * marker file to trigger the deployment.
     *
     * param autoDeployZip true if auto-deployment of zipped content is enabled
     */
    void setAutoDeployZippedContent(boolean autoDeployZip);

    /**
     * Gets whether the scanner will attempt to deploy exploded content based solely
     * on detecting a change in the content; i.e. without requiring a
     * marker file to trigger the deployment.
     *
     * @return true if auto-deployment of exploded content is enabled
     */
    boolean isAutoDeployExplodedContent() ;

    /**
     * Sets whether the scanner will attempt to deploy exploded content based solely
     * on detecting a change in the content; i.e. without requiring a
     * marker file to trigger the deployment.
     *
     * param autoDeployZip true if auto-deployment of exploded content is enabled
     */
    void setAutoDeployExplodedContent(boolean autoDeployExploded);

    /**
     * Gets whether the scanner will attempt to deploy XML content based solely
     * on detecting a change in the content; i.e. without requiring a
     * marker file to trigger the deployment.
     *
     * @return true if auto-deployment of XML content is enabled
     */
    boolean isAutoDeployXMLContent();

    /**
     * Sets whether the scanner will attempt to deploy XML content based solely
     * on detecting a change in the content; i.e. without requiring a
     * marker file to trigger the deployment.
     *
     * param autoDeployXML true if auto-deployment of XML content is enabled
     */
    void setAutoDeployXMLContent(boolean autoDeployXML);

    /**
     * Set the timeout used for deployments.
     *
     * @param timeout The deployment timeout
     */
    void setDeploymentTimeout(long timeout);
}
