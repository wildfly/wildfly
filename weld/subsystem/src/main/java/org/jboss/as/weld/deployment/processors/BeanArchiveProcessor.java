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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.InjectionTargetDefiningAnnotations;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
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
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl.BeanArchiveType;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadata;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.spi.ComponentDescriptionProcessor;
import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.util.collections.Multimap;
import org.jboss.weld.util.collections.SetMultimap;
import org.wildfly.security.manager.WildFlySecurityManager;

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
            BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(Collections.<String>emptySet(), Collections.<String>emptySet(), BeansXml.EMPTY_BEANS_XML, handler.module, getDeploymentUnitId(deploymentUnit), BeanArchiveType.SYNTHETIC, true);
            WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);
            bdaMap.put(handler.deploymentResourceRoot, bda);
        }
        deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bdaMap.get(handler.deploymentResourceRoot));

        /*
         * Finish EE component processing
         */
        for (Entry<ResourceRoot, Collection<ComponentDescription>> entry : components.componentDescriptions.entrySet()) {
            BeanDeploymentArchiveImpl bda = bdaMap.get(entry.getKey());
            String id = null;
            if (bda != null) {
                id = bda.getId();
            } else {
                id = deploymentUnit.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE).getId();
            }
            for (ComponentDescription componentDescription : entry.getValue()) {
                componentDescription.setBeanDeploymentArchiveId(id);
            }
        }

        final BeanDeploymentModule bdm = new BeanDeploymentModule(handler.module.getIdentifier().toString(), deploymentUnit, bdaMap.values());
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

        private final Multimap<ResourceRoot, ComponentDescription> componentDescriptions = SetMultimap.newSetMultimap();
        private final List<ComponentDescription> implicitComponentDescriptions = new ArrayList<ComponentDescription>();

        private final Iterable<ComponentDescriptionProcessor> componentDescriptionProcessors;

        public Components(DeploymentUnit deploymentUnit, Map<ResourceRoot, Index> indexes) {

            componentDescriptionProcessors = ServiceLoader.load(ComponentDescriptionProcessor.class,
                    WildFlySecurityManager.getClassLoaderPrivileged(BeanArchiveProcessor.class));

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
                if (resourceRoot == null) {
                    implicitComponentDescriptions.add(component);
                }
                if (resourceRoot == null || isClassesRoot(resourceRoot)) {
                    // special handling
                    resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                }
                componentDescriptions.put(resourceRoot, component);

                // Process component descriptions
                for (ComponentDescriptionProcessor processor : componentDescriptionProcessors) {
                    processor.processComponentDescription(resourceRoot, component);
                }
            }
        }

        boolean hasBeanComponents(ResourceRoot resourceRoot) {
            for (ComponentDescriptionProcessor processor : componentDescriptionProcessors) {
                if (processor.hasBeanComponents(resourceRoot)) {
                    return true;
                }
            }
            return false;
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
            List<DotName> definingAnnotations = getRootDeploymentUnit(deploymentUnit).getAttachmentList(InjectionTargetDefiningAnnotations.INJECTION_TARGET_DEFINING_ANNOTATIONS);
            for(DotName annotation : definingAnnotations) {
                annotationTypes.add(new AnnotationType(annotation, false));
            }
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
                Set<String> allKnownClasses = getAllKnownClasses(index);

                if (beans.isEmpty() && !components.hasBeanComponents(resourceRoot)) {
                    return null;
                }

                BeansXml beansXml = null;
                if (metadata != null) {
                    beansXml = metadata.getBeansXml();
                }

                bda = new BeanDeploymentArchiveImpl(beans, allKnownClasses, beansXml, module, createBeanArchiveId(resourceRoot), BeanArchiveType.IMPLICIT, isRootBda);
                WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);
            } else if (metadata.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
                // scanning suppressed per spec in this archive
                return null;
            } else {
                boolean isRootBda = metadata.isDeploymentRoot();
                bda = createExplicitBeanDeploymentArchive(indexes.get(metadata.getResourceRoot()), metadata, isRootBda);
                WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);
            }

            // Register processed components
            for (ComponentDescriptionProcessor processor : components.componentDescriptionProcessors) {
                processor.registerComponents(resourceRoot, bda, reflectionIndex);
            }

            return bda;
        }

        private Set<String> getAllKnownClasses(Index index) {
            Set<String> allKnownClasses = new HashSet<String>();
            // index may be null if a war has a beans.xml but no WEB-INF/classes
            if (index != null) {
                for (ClassInfo classInfo : index.getKnownClasses()) {
                    allKnownClasses.add(Indices.CLASS_INFO_TO_FQCN.apply(classInfo));
                }
            }
            return allKnownClasses;
        }

        private Set<String> getImplicitBeanClasses(Index index, ResourceRoot resourceRoot) {
            Set<String> implicitBeanClasses = new HashSet<String>();
            for (AnnotationType beanDefiningAnnotation : beanDefiningAnnotations) {
                List<AnnotationInstance> annotationInstances = index.getAnnotations(beanDefiningAnnotation.getName());
                for (ClassInfo classInfo : Indices.getAnnotatedClasses(annotationInstances)) {
                    implicitBeanClasses.add(Indices.CLASS_INFO_TO_FQCN.apply(classInfo));
                }
            }
            // Make all explicit components into implicit beans so they will support injection
            for(ComponentDescription description : components.componentDescriptions.get(resourceRoot)) {
                if(!components.implicitComponentDescriptions.contains(description)) {
                    implicitBeanClasses.add(description.getComponentClassName());
                }
            }
            return implicitBeanClasses;
        }

        private BeanDeploymentArchiveImpl createExplicitBeanDeploymentArchive(final Index index, ExplicitBeanArchiveMetadata beanArchiveMetadata, boolean root) throws DeploymentUnitProcessingException {

            Set<String> classNames = getAllKnownClasses(index);
            return new BeanDeploymentArchiveImpl(classNames, classNames, beanArchiveMetadata.getBeansXml(), module, createBeanArchiveId(beanArchiveMetadata.getResourceRoot()), BeanArchiveType.EXPLICIT, root);
        }

        private String createBeanArchiveId(ResourceRoot resourceRoot) {
            String beanArchiveId = getDeploymentUnitId(deploymentUnit);
            if (resourceRoot != null) {
                final VirtualFile deploymentRootResource = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                beanArchiveId += "/" + resourceRoot.getRoot().getPathNameRelativeTo(deploymentRootResource);
            }
            return beanArchiveId;
        }
    }
}
