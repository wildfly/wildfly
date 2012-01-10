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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.deployment.BeanArchiveMetadata;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.deployment.WeldDeploymentMetadata;
import org.jboss.as.weld.ejb.EjbDescriptorImpl;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.injection.spi.JpaInjectionServices;

/**
 * Deployment processor that builds bean archives and attaches them to the deployment
 * <p/>
 * Currently this is done by pulling the information out of the jandex {@link Index}.
 * <p/>
 *
 * @author Stuart Douglas
 */
public class BeanArchiveProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WeldDeploymentMetadata cdiDeploymentMetadata = deploymentUnit
                .getAttachment(WeldDeploymentMetadata.ATTACHMENT_KEY);
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        final String beanArchiveIdPrefix;
        if (deploymentUnit.getParent() == null) {
            beanArchiveIdPrefix = deploymentUnit.getName();
        } else {
            beanArchiveIdPrefix = deploymentUnit.getParent().getName() + "." + deploymentUnit.getName();
        }

        final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>();
        WeldLogger.DEPLOYMENT_LOGGER.processingWeldDeployment(phaseContext.getDeploymentUnit().getName());

        final Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
        final Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap = new HashMap<ResourceRoot, BeanDeploymentArchiveImpl>();

        final Module module = phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE);
        BeanDeploymentArchiveImpl rootBda = null;
        if (cdiDeploymentMetadata != null) {
            // this can be null for ear deployments
            // however we still want to create a module level bean manager
            for (BeanArchiveMetadata beanArchiveMetadata : cdiDeploymentMetadata.getBeanArchiveMetadata()) {
                BeanDeploymentArchiveImpl bda = createBeanDeploymentArchive(indexes.get(beanArchiveMetadata.getResourceRoot()),
                        beanArchiveMetadata, module, beanArchiveIdPrefix);
                beanDeploymentArchives.add(bda);
                bdaMap.put(beanArchiveMetadata.getResourceRoot(), bda);
                if (beanArchiveMetadata.isDeploymentRoot()) {
                    rootBda = bda;
                    deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bda);
                }
            }
        }
        if (rootBda == null) {
            BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(Collections.<String>emptySet(),
                    BeansXml.EMPTY_BEANS_XML, module, beanArchiveIdPrefix);
            beanDeploymentArchives.add(bda);
            deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bda);
            rootBda = bda;
        }
        processEEComponents(deploymentUnit, bdaMap, rootBda, indexes, reflectionIndex);

        final JpaInjectionServices jpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit, deploymentUnit.getServiceRegistry());

        final BeanDeploymentModule bdm = new BeanDeploymentModule(beanDeploymentArchives);
        bdm.addService(JpaInjectionServices.class, jpaInjectionServices);
        deploymentUnit.putAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE, bdm);
    }

    private void processEEComponents(DeploymentUnit deploymentUnit, Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap, BeanDeploymentArchiveImpl rootBda, Map<ResourceRoot, Index> indexes, DeploymentReflectionIndex reflectionIndex) {
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            BeanDeploymentArchiveImpl bda = resolveComponentBda(component.getComponentClassName(), bdaMap, rootBda, indexes);
            component.setBeanDeploymentArchiveId(bda.getId());
            if (component instanceof EJBComponentDescription) {
                final EJBComponentDescription componentDescription = (EJBComponentDescription) component;
                //first we need to resolve the correct BDA for the bean
                bda.addEjbDescriptor(new EjbDescriptorImpl<Object>(componentDescription, bda, reflectionIndex));
            }
        }
    }

    /**
     * Resolves the bean deployment archive for a session bean
     *
     * @param ejbClassName the session bean's class
     * @param bdaMap       The BDA's keyed by resource root
     * @param rootBda      The root bda, this is used as the BDA of last resort if the correct BDA cannot be found
     * @param indexes      The jandex indexes
     * @return The correct BDA for the EJB
     */
    private BeanDeploymentArchiveImpl resolveComponentBda(String ejbClassName, Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap, BeanDeploymentArchiveImpl rootBda, Map<ResourceRoot, Index> indexes) {
        final DotName className = DotName.createSimple(ejbClassName);
        for (Map.Entry<ResourceRoot, BeanDeploymentArchiveImpl> entry : bdaMap.entrySet()) {
            final Index index = indexes.get(entry.getKey());
            if (index != null) {
                if (index.getClassByName(className) != null) {
                    return entry.getValue();
                }
            }
        }
        return rootBda;
    }

    private BeanDeploymentArchiveImpl createBeanDeploymentArchive(final Index index, BeanArchiveMetadata beanArchiveMetadata,
                                                                  Module module, String beanArchivePrefix) throws DeploymentUnitProcessingException {

        Set<String> classNames = new HashSet<String>();
        // index may be null if a war has a beans.xml but no WEB-INF/classes
        if (index != null) {
            for (ClassInfo classInfo : index.getKnownClasses()) {
                classNames.add(classInfo.name().toString());
            }
        }
        return new BeanDeploymentArchiveImpl(classNames, beanArchiveMetadata.getBeansXml(), module, beanArchivePrefix
                + beanArchiveMetadata.getResourceRoot().getRoot().getPathName());
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
