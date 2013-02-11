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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.deployment.BeanArchiveMetadata;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.BeansXmlParser;
import org.jboss.as.weld.deployment.UrlScanner;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.deployment.WeldDeploymentMetadata;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.injection.spi.JpaInjectionServices;

/**
 * Deployment processor that builds bean archives from external deployments.
 * <p/>
 * This is only run at the top level, as multiple sub deployments can reference the same
 * beans.xml information, so we have to iterate through all bean deployment archives in this processor, to prevent
 * beans.xml from being potentially parsed twice.
 * <p/>
 *
 * @author Stuart Douglas
 */
public class ExternalBeanArchiveProcessor implements DeploymentUnitProcessor {

    private static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WeldDeploymentMetadata cdiDeploymentMetadata = deploymentUnit
                .getAttachment(WeldDeploymentMetadata.ATTACHMENT_KEY);
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        if (deploymentUnit.getParent() != null) {
            return;
        }

        final String beanArchiveIdPrefix = deploymentUnit.getName() + ".external.";

        final List<DeploymentUnit> deploymentUnits = new ArrayList<DeploymentUnit>();
        deploymentUnits.add(deploymentUnit);
        deploymentUnits.addAll(deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS));

        BeansXmlParser parser = new BeansXmlParser();

        final Map<URL, List<DeploymentUnit>> deploymentUnitMap = new HashMap<URL, List<DeploymentUnit>>();

        final HashSet<URL> existing = new HashSet<URL>();

        for (DeploymentUnit deployment : deploymentUnits) {
            try {
                final WeldDeploymentMetadata weldDeploymentMetadata = deployment.getAttachment(WeldDeploymentMetadata.ATTACHMENT_KEY);
                if (weldDeploymentMetadata != null) {
                    for (BeanArchiveMetadata md : weldDeploymentMetadata.getBeanArchiveMetadata()) {
                        URL file = md.getBeansXmlFile().toURL();
                        existing.add(file);
                    }
                    if(deployment.getName().endsWith(".war")) {
                        //war's can also have a META-INF/beans.xml that does not show up as an
                        //existing beans.xml, as they already have a WEB-INF/beans.xml
                        ResourceRoot deploymentRoot = deployment.getAttachment(Attachments.DEPLOYMENT_ROOT);
                        VirtualFile beans = deploymentRoot.getRoot().getChild(META_INF_BEANS_XML);
                        if(beans.exists()) {
                            existing.add(beans.toURL());
                        }
                    }

                }
            } catch (MalformedURLException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        for (DeploymentUnit deployment : deploymentUnits) {
            final Module module = deployment.getAttachment(Attachments.MODULE);
            if (module == null) {
                return;
            }
            try {
                Enumeration<URL> resources = module.getClassLoader().getResources(META_INF_BEANS_XML);
                while (resources.hasMoreElements()) {
                    final URL beansXml = resources.nextElement();
                    if (existing.contains(beansXml)) {
                        continue;
                    }
                    WeldLogger.DEPLOYMENT_LOGGER.debugf("Found external beans.xml: %s", beansXml.toString());
                    List<DeploymentUnit> dus = deploymentUnitMap.get(beansXml);
                    if (dus == null) {
                        deploymentUnitMap.put(beansXml, dus = new ArrayList<DeploymentUnit>());
                    }
                    dus.add(deployment);
                }
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        for (final Map.Entry<URL, List<DeploymentUnit>> entry : deploymentUnitMap.entrySet()) {
            //we just take the first module, it should not make any difference. The idea that
            //the same beans.xml is accessible via two different CL's is not something we can deal with
            final Module module = entry.getValue().get(0).getAttachment(Attachments.MODULE);
            final BeansXml beansXml = parseBeansXml(entry.getKey(), parser, deploymentUnit);

            final UrlScanner urlScanner = new UrlScanner();
            final List<String> discoveredClasses = new ArrayList<String>();
            if(!urlScanner.handleBeansXml(entry.getKey(), discoveredClasses)) {
                continue;
            }

            final BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(new HashSet<String>(discoveredClasses), beansXml, module, beanArchiveIdPrefix + entry.getKey().toExternalForm());

            final BeanDeploymentModule bdm = new BeanDeploymentModule(Collections.singleton(bda));
            final JpaInjectionServices jpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit, deploymentUnit.getServiceRegistry());
            bdm.addService(JpaInjectionServices.class, jpaInjectionServices);
            deploymentUnit.addToAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES, bdm);
            for (DeploymentUnit du : entry.getValue()) {
                du.addToAttachmentList(WeldAttachments.VISIBLE_ADDITIONAL_BEAN_DEPLOYMENT_MODULE, bdm);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private BeansXml parseBeansXml(URL beansXmlFile, BeansXmlParser parser, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        return parser.parse(beansXmlFile, SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
    }
}
