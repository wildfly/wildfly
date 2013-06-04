/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
