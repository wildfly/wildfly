/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment;

import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeansXml;

/**
 * Maintains information about an explicit bean archive
 * <p>
 * Thread Safety: This class is immutable and does not require a happens before event between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class ExplicitBeanArchiveMetadata {

    /**
     * The location of the beans.xml file for this bean archive
     */
    private final VirtualFile beansXmlFile;

    /**
     * The ResourceRoot for the archive
     */
    private final ResourceRoot resourceRoot;

    /**
     * If this bean archive is the root of the deployment (e.g. an ear or a standalone war)
     */
    private final boolean deploymentRoot;

    /**
     * the parsed beans.xml representation
     */
    private final BeansXml beansXml;

    /**
     * The location of an additional beans.xml file for this bean archive. This may happen if a web archive defines
     * both META-INF/beans.xml and WEB-INF/beans.xml
     */
    private final VirtualFile additionalBeansXmlFile;

    public ExplicitBeanArchiveMetadata(VirtualFile beansXmlFile, ResourceRoot resourceRoot, BeansXml beansXml, boolean deploymentRoot) {
        this(beansXmlFile, null, resourceRoot, beansXml, deploymentRoot);
    }

    public ExplicitBeanArchiveMetadata(VirtualFile beansXmlFile, VirtualFile additionalBeansXmlFile, ResourceRoot resourceRoot, BeansXml beansXml, boolean deploymentRoot) {
        this.beansXmlFile = beansXmlFile;
        this.additionalBeansXmlFile = additionalBeansXmlFile;
        this.resourceRoot = resourceRoot;
        this.deploymentRoot = deploymentRoot;
        this.beansXml = beansXml;
    }

    public VirtualFile getBeansXmlFile() {
        return beansXmlFile;
    }

    public ResourceRoot getResourceRoot() {
        return resourceRoot;
    }

    public boolean isDeploymentRoot() {
        return deploymentRoot;
    }

    public BeansXml getBeansXml() {
        return beansXml;
    }

    public VirtualFile getAdditionalBeansXmlFile() {
        return additionalBeansXmlFile;
    }
}
