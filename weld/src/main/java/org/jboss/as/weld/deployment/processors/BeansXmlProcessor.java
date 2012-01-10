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
package org.jboss.as.weld.deployment.processors;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.WeldMessages;
import org.jboss.as.weld.deployment.BeanArchiveMetadata;
import org.jboss.as.weld.deployment.BeansXmlParser;
import org.jboss.as.weld.deployment.WeldDeploymentMetadata;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeansXml;

/**
 * Deployment processor that finds <literal>beans.xml</literal> files and attaches the information to the deployment
 *
 * @author Stuart Douglas
 */
public class BeansXmlProcessor implements DeploymentUnitProcessor {

    private static final String WEB_INF_BEANS_XML = "WEB-INF/beans.xml";
    private static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        BeansXmlParser parser = new BeansXmlParser();

        Set<BeanArchiveMetadata> beanArchiveMetadata = new HashSet<BeanArchiveMetadata>();
        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        AttachmentList<ResourceRoot> structure = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (deploymentRoot == null) {
            return;
        }

        ResourceRoot classesRoot = null;

        if (structure != null) {
            for (ResourceRoot resourceRoot : structure) {
                if (ModuleRootMarker.isModuleRoot(resourceRoot) && !SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                    if (resourceRoot.getRootName().equals("classes")) {
                        // hack for dealing with war modules
                        classesRoot = resourceRoot;
                    } else {
                        VirtualFile beansXml = resourceRoot.getRoot().getChild(META_INF_BEANS_XML);
                        if (beansXml.exists() && beansXml.isFile()) {
                            WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", beansXml.toString());
                            beanArchiveMetadata.add(new BeanArchiveMetadata(beansXml, resourceRoot, parseBeansXml(beansXml,
                                    parser), false));
                        }
                    }
                }
            }
        }

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            // look for WEB-INF/beans.xml
            final VirtualFile rootBeansXml = deploymentRoot.getRoot().getChild(WEB_INF_BEANS_XML);
            if (rootBeansXml.exists() && rootBeansXml.isFile()) {
                WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", rootBeansXml);
                beanArchiveMetadata.add(new BeanArchiveMetadata(rootBeansXml, classesRoot, parseBeansXml(rootBeansXml, parser), true));
            } else if (classesRoot != null) {

                //look for beans.xml files in the wrong location
                VirtualFile beansXml = classesRoot.getRoot().getChild(META_INF_BEANS_XML);
                if (beansXml.exists() && beansXml.isFile()) {
                    WeldLogger.DEPLOYMENT_LOGGER.beansXmlInNonStandardLocation(beansXml.toString());
                    beanArchiveMetadata.add(new BeanArchiveMetadata(beansXml, classesRoot, parseBeansXml(beansXml, parser), true));
                }
            }
        } else if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            final VirtualFile rootBeansXml = deploymentRoot.getRoot().getChild(META_INF_BEANS_XML);
            if (rootBeansXml.exists() && rootBeansXml.isFile()) {
                WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", rootBeansXml.toString());
                beanArchiveMetadata.add(new BeanArchiveMetadata(rootBeansXml, deploymentRoot, parseBeansXml(rootBeansXml, parser), true));
            }
        }

        if (!beanArchiveMetadata.isEmpty()) {
            WeldDeploymentMetadata deploymentMetadata = new WeldDeploymentMetadata(beanArchiveMetadata);
            deploymentUnit.putAttachment(WeldDeploymentMetadata.ATTACHMENT_KEY, deploymentMetadata);
            // mark the deployment as requiring CDI integration
            WeldDeploymentMarker.mark(deploymentUnit);
            if (deploymentUnit.getParent() != null) {
                WeldDeploymentMarker.mark(deploymentUnit.getParent());
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private BeansXml parseBeansXml(VirtualFile beansXmlFile, BeansXmlParser parser) throws DeploymentUnitProcessingException {
        try {
            return parser.parse(beansXmlFile.asFileURL());
        } catch (MalformedURLException e) {
            throw WeldMessages.MESSAGES.couldNotGetBeansXmlAsURL(beansXmlFile.toString(), e);
        }
    }

}
