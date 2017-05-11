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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadata;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.PropertyReplacingBeansXmlParser;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
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


        Map<ResourceRoot, ExplicitBeanArchiveMetadata> beanArchiveMetadata = new HashMap<ResourceRoot, ExplicitBeanArchiveMetadata>();
        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot == null) {
            return;
        }

        PropertyReplacingBeansXmlParser parser = new PropertyReplacingBeansXmlParser(deploymentUnit);

        ResourceRoot classesRoot = null;
        List<ResourceRoot> structure = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : structure) {
            if (ModuleRootMarker.isModuleRoot(resourceRoot) && !SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                if (resourceRoot.getRootName().equals("classes")) {
                    // hack for dealing with war modules
                    classesRoot = resourceRoot;
                    deploymentUnit.putAttachment(WeldAttachments.CLASSES_RESOURCE_ROOT, resourceRoot);
                } else {
                    VirtualFile beansXml = resourceRoot.getRoot().getChild(META_INF_BEANS_XML);
                    if (beansXml.exists() && beansXml.isFile()) {
                        WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", beansXml.toString());
                        beanArchiveMetadata.put(resourceRoot, new ExplicitBeanArchiveMetadata(beansXml, resourceRoot, parseBeansXml(beansXml, parser, deploymentUnit), false));
                    }
                }
            }
        }

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            final VirtualFile rootBeansXml = deploymentRoot.getRoot().getChild(WEB_INF_BEANS_XML);
            final boolean rootBeansXmlPresent = rootBeansXml.exists() && rootBeansXml.isFile();

            VirtualFile beansXml = null;
            if (classesRoot != null) {
                beansXml = classesRoot.getRoot().getChild(META_INF_BEANS_XML);
            }
            final boolean beansXmlPresent = beansXml != null && beansXml.exists() && beansXml.isFile();

            if (rootBeansXmlPresent) {
                if (beansXmlPresent) {
                    // warn that it is not portable to use both locations at the same time
                    WeldLogger.DEPLOYMENT_LOGGER.duplicateBeansXml();
                    beanArchiveMetadata.put(deploymentRoot, new ExplicitBeanArchiveMetadata(rootBeansXml, beansXml, classesRoot, parseBeansXml(rootBeansXml, parser, deploymentUnit), true));
                } else {
                    WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", rootBeansXml);
                    beanArchiveMetadata.put(deploymentRoot, new ExplicitBeanArchiveMetadata(rootBeansXml, classesRoot, parseBeansXml(rootBeansXml, parser, deploymentUnit), true));
                }
            } else if (beansXmlPresent) {
                WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", beansXml);
                beanArchiveMetadata.put(deploymentRoot, new ExplicitBeanArchiveMetadata(beansXml, classesRoot, parseBeansXml(beansXml, parser, deploymentUnit), true));
            }
        } else if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            final VirtualFile rootBeansXml = deploymentRoot.getRoot().getChild(META_INF_BEANS_XML);
            if (rootBeansXml.exists() && rootBeansXml.isFile()) {
                WeldLogger.DEPLOYMENT_LOGGER.debugf("Found beans.xml: %s", rootBeansXml.toString());
                beanArchiveMetadata.put(deploymentRoot, new ExplicitBeanArchiveMetadata(rootBeansXml, deploymentRoot, parseBeansXml(rootBeansXml, parser, deploymentUnit), true));
            }
        }

        if (!beanArchiveMetadata.isEmpty()) {
            ExplicitBeanArchiveMetadataContainer deploymentMetadata = new ExplicitBeanArchiveMetadataContainer(beanArchiveMetadata);
            deploymentUnit.putAttachment(ExplicitBeanArchiveMetadataContainer.ATTACHMENT_KEY, deploymentMetadata);

            for (Iterator<Entry<ResourceRoot, ExplicitBeanArchiveMetadata>> iterator = beanArchiveMetadata.entrySet().iterator(); iterator.hasNext(); ) {
                if (BeanDiscoveryMode.NONE != iterator.next().getValue().getBeansXml().getBeanDiscoveryMode()) {
                    // mark the deployment as requiring CDI integration as long as it contains at least one bean archive with bean-discovery-mode other than "none"
                    WeldDeploymentMarker.mark(deploymentUnit);
                    break;
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(WeldAttachments.CLASSES_RESOURCE_ROOT);
        deploymentUnit.removeAttachment(ExplicitBeanArchiveMetadataContainer.ATTACHMENT_KEY);
    }

    private BeansXml parseBeansXml(VirtualFile beansXmlFile, PropertyReplacingBeansXmlParser parser, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        try {
            return parser.parse(beansXmlFile.asFileURL());
        } catch (MalformedURLException e) {
            throw WeldLogger.ROOT_LOGGER.couldNotGetBeansXmlAsURL(beansXmlFile.toString(), e);
        } catch (RuntimeException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }
}