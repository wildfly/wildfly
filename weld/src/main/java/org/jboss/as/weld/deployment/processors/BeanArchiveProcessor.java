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

import static org.jboss.as.weld.util.Utils.getDeploymentUnitId;
import static org.jboss.as.weld.util.Utils.getRootDeploymentUnit;
import static org.jboss.as.weld.util.Utils.isClassesRoot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl.BeanArchiveType;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadata;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.ejb.EjbDescriptorImpl;
import org.jboss.as.weld.services.bootstrap.WeldJaxwsInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.injection.spi.JaxwsInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Deployment processor that builds bean archives and attaches them to the deployment
 * <p/>
 * Currently this is done by pulling the information out of the jandex {@link Index}.
 * <p/>
 *
 * @author Stuart Douglas
 * @author Jozef Hartinger
 */
public class BeanArchiveProcessor implements DeploymentUnitProcessor {

    private static final DotName EXTENSION_NAME = DotName.createSimple(Extension.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        WeldLogger.DEPLOYMENT_LOGGER.processingWeldDeployment(deploymentUnit.getName());

        final Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
        final Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap = new HashMap<ResourceRoot, BeanDeploymentArchiveImpl>();

        final Components components = new Components(deploymentUnit, indexes);

        final ResourceRootHandler handler = new ResourceRootHandler(deploymentUnit, components, indexes);

        for (ResourceRoot resourceRoot : deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS)) {
            if (ModuleRootMarker.isModuleRoot(resourceRoot) && !SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                if (isClassesRoot(resourceRoot)) {
                    continue; // this is handled below
                }
                handler.handleResourceRoot(bdaMap, resourceRoot);
            }
        }
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            handler.handleResourceRoot(bdaMap, handler.deploymentResourceRoot);
        }
        if (!bdaMap.containsKey(handler.deploymentResourceRoot)) {
            // there is not root bda, let's create one
            BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(Collections.<String>emptySet(), BeansXml.EMPTY_BEANS_XML, handler.module, getDeploymentUnitId(deploymentUnit), BeanArchiveType.SYNTHETIC, true);
            WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);
            bdaMap.put(handler.deploymentResourceRoot, bda);
        }
        deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bdaMap.get(handler.deploymentResourceRoot));

        /*
         * Finish EE component processing
         */
        for (Map.Entry<ResourceRoot, ComponentDescription> entry : components.componentDescriptions.entries()) {
            BeanDeploymentArchiveImpl bda = bdaMap.get(entry.getKey());
            String id = null;
            if (bda != null) {
                id = bda.getId();
            } else {
                id = deploymentUnit.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE).getId();
            }
            entry.getValue().setBeanDeploymentArchiveId(id);
        }

        final JpaInjectionServices jpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit);
        final JaxwsInjectionServices jaxwsInjectionServices = new WeldJaxwsInjectionServices(deploymentUnit);

        final BeanDeploymentModule bdm = new BeanDeploymentModule(bdaMap.values());
        bdm.addService(JpaInjectionServices.class, jpaInjectionServices);
        bdm.addService(JaxwsInjectionServices.class, jaxwsInjectionServices);
        deploymentUnit.putAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE, bdm);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        context.removeAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE);
        context.removeAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);
    }

    /**
     * Arranges component descriptions into maps keyed on the resource root a component is located under.
     */
    private static class Components {

        private final Multimap<ResourceRoot, ComponentDescription> componentDescriptions = HashMultimap.create();
        private final Multimap<ResourceRoot, EJBComponentDescription> ejbComponentDescriptions = HashMultimap.create();

        public Components(DeploymentUnit deploymentUnit, Map<ResourceRoot, Index> indexes) {
            for (ComponentDescription component : deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION).getComponentDescriptions()) {
                ResourceRoot resourceRoot = null;
                DotName componentClassName = DotName.createSimple(component.getComponentClassName());
                for (Entry<ResourceRoot, Index> entry : indexes.entrySet()) {
                    final Index index = entry.getValue();
                    if (index != null) {
                        if (index.getClassByName(componentClassName) != null) {
                            resourceRoot = entry.getKey();
                            break;
                        }
                    }
                }
                if (resourceRoot == null || isClassesRoot(resourceRoot)) {
                    // special handling
                    resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                }

                componentDescriptions.put(resourceRoot, component);
                if (component instanceof EJBComponentDescription) {
                    ejbComponentDescriptions.put(resourceRoot, (EJBComponentDescription) component);
                }
            }
        }
    }

    private static class ResourceRootHandler {
        private final DeploymentUnit deploymentUnit;
        private final Module module;
        private final Map<ResourceRoot, Index> indexes;
        private final Components components;
        private final DeploymentReflectionIndex reflectionIndex;
        private final ResourceRoot deploymentResourceRoot;
        private final ResourceRoot classesResourceRoot;
        private final ExplicitBeanArchiveMetadataContainer explicitBeanArchives;
        private final Set<AnnotationType> beanDefiningAnnotations;
        private final boolean requireBeanDescriptor;

        private ResourceRootHandler(DeploymentUnit deploymentUnit, Components components, Map<ResourceRoot, Index> indexes) {
            this.deploymentUnit = deploymentUnit;
            this.explicitBeanArchives = deploymentUnit.getAttachment(ExplicitBeanArchiveMetadataContainer.ATTACHMENT_KEY);
            this.module = deploymentUnit.getAttachment(Attachments.MODULE);
            this.indexes = indexes;
            this.components = components;
            this.reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
            this.deploymentResourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            this.classesResourceRoot = deploymentUnit.getAttachment(WeldAttachments.CLASSES_RESOURCE_ROOT);
            HashSet<AnnotationType> annotationTypes = new HashSet<>(getRootDeploymentUnit(deploymentUnit).getAttachment(WeldAttachments.BEAN_DEFINING_ANNOTATIONS));
            annotationTypes.addAll(getRootDeploymentUnit(deploymentUnit).getAttachmentList(WeldAttachments.INJECTION_TARGET_DEFINING_ANNOTATIONS));
            this.beanDefiningAnnotations = annotationTypes;
            this.requireBeanDescriptor = getRootDeploymentUnit(deploymentUnit).getAttachment(WeldConfiguration.ATTACHMENT_KEY).isRequireBeanDescriptor();
        }

        private void handleResourceRoot(Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap, ResourceRoot resourceRoot) throws DeploymentUnitProcessingException {
            BeanDeploymentArchiveImpl bda = processResourceRoot(resourceRoot);
            if (bda != null) {
                bdaMap.put(resourceRoot, bda);
            }
        }

        /**
         * Process a resource root eventually creating a bean archive out of it if it matches requirements for either an
         * implicit or explicit bean archive. There requirements are laid down by the CDI spec.
         *
         * If the resource root does not represent a bean archive, null is returned.
         */
        private BeanDeploymentArchiveImpl processResourceRoot(ResourceRoot resourceRoot) throws DeploymentUnitProcessingException {
            ExplicitBeanArchiveMetadata metadata = null;
            if (explicitBeanArchives != null) {
                metadata = explicitBeanArchives.getBeanArchiveMetadata().get(resourceRoot);
            }
            BeanDeploymentArchiveImpl bda = null;
            if (metadata == null && requireBeanDescriptor) {
                /*
                 * For compatibility with Contexts and Dependency 1.0, products must contain an option to cause an archive to be ignored by the
                 * container when no beans.xml is present.
                 */
                return null;
            }
            if (metadata == null || metadata.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.ANNOTATED)) {
                // this is either an implicit bean archive or not a bean archive at all!

                final boolean isRootBda = resourceRoot.equals(deploymentResourceRoot);

                ResourceRoot indexResourceRoot = resourceRoot;
                if (resourceRoot == deploymentResourceRoot && classesResourceRoot != null) {
                    // this is WEB-INF/classes BDA
                    indexResourceRoot = classesResourceRoot;
                }

                final Index index = indexes.get(indexResourceRoot);
                if (index == null) {
                    return null; // index may be null for some resource roots
                }

                /*
                 * An archive which contains an extension and no beans.xml file is not a bean archive.
                 */
                if (metadata == null && !index.getAllKnownImplementors(EXTENSION_NAME).isEmpty()) {
                    return null;
                }

                Set<String> beans = getImplicitBeanClasses(index, resourceRoot);

                if (beans.isEmpty() && components.ejbComponentDescriptions.get(resourceRoot).isEmpty()) {
                    return null;
                }

                BeansXml beansXml = null;
                if (metadata != null) {
                    beansXml = metadata.getBeansXml();
                }

                bda = new BeanDeploymentArchiveImpl(beans, beansXml, module, resourceRoot.getRoot().getPathName(), BeanArchiveType.IMPLICIT, isRootBda);
                WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);
            } else if (metadata.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
                // scanning suppressed per spec in this archive
                return null;
            } else {
                boolean isRootBda = metadata.isDeploymentRoot();
                bda = createExplicitBeanDeploymentArchive(indexes.get(metadata.getResourceRoot()), metadata, isRootBda);
                WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);
            }

            Collection<EJBComponentDescription> ejbComponents = components.ejbComponentDescriptions.get(resourceRoot);

            // register EJBs with the BDA
            for (EJBComponentDescription ejb : ejbComponents) {
                bda.addEjbDescriptor(new EjbDescriptorImpl<Object>(ejb, bda, reflectionIndex));
                bda.addBeanClass(ejb.getComponentClassName());
            }

            return bda;
        }

        private Set<String> getImplicitBeanClasses(Index index, ResourceRoot resourceRoot) {
            Set<String> implicitBeanClasses = new HashSet<String>();
            for (AnnotationType beanDefiningAnnotation : beanDefiningAnnotations) {
                List<AnnotationInstance> annotationInstances = index.getAnnotations(beanDefiningAnnotation.getName());
                implicitBeanClasses.addAll(Lists.transform(Indices.getAnnotatedClasses(annotationInstances), Indices.CLASS_INFO_TO_FQCN));
            }
            //make all components into implicit beans so they will support injection
            for(ComponentDescription description : components.componentDescriptions.get(resourceRoot)) {
                implicitBeanClasses.add(description.getComponentClassName());
            }
            return implicitBeanClasses;
        }

        private BeanDeploymentArchiveImpl createExplicitBeanDeploymentArchive(final Index index, ExplicitBeanArchiveMetadata beanArchiveMetadata, boolean root) throws DeploymentUnitProcessingException {

            Set<String> classNames = new HashSet<String>();
            // index may be null if a war has a beans.xml but no WEB-INF/classes
            if (index != null) {
                classNames.addAll(Collections2.transform(index.getKnownClasses(), Indices.CLASS_INFO_TO_FQCN));
            }

            String beanArchiveId = getDeploymentUnitId(deploymentUnit);
            if (beanArchiveMetadata.getResourceRoot() != null) {
                final VirtualFile deploymentRootResource = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                beanArchiveId += "/" + beanArchiveMetadata.getResourceRoot().getRoot().getPathNameRelativeTo(deploymentRootResource);
            }
            return new BeanDeploymentArchiveImpl(classNames, beanArchiveMetadata.getBeansXml(), module, beanArchiveId, BeanArchiveType.EXPLICIT, root);
        }
    }
}
