/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.cache.EJBBoundCacheMetaData;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.Cache;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.msc.service.ServiceName;

public class CacheMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {

    public CacheMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        super.deploy(phaseContext);

        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        EEModuleDescription moduleDescription = unit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        // Collect distinct cache providers required by deployment
        Set<ServiceName> names = new TreeSet<>();
        for (ComponentDescription description : moduleDescription.getComponentDescriptions()) {
            if (description instanceof StatefulComponentDescription statefulDescription) {
                names.add(statefulDescription.getCacheProviderServiceName(support));
            }
        }
        // Make providers available to next phase
        for (ServiceName name : names) {
            phaseContext.addDependency(name, StatefulSessionBeanCacheProvider.ATTACHMENT_KEY);
        }
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses,
            DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
            StatefulComponentDescription description) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<Cache, CacheInfo> cache = clazz.getAnnotationInformation(Cache.class);
        if (cache == null) {
            return;
        }
        if (!cache.getClassLevelAnnotations().isEmpty()) {
            description.setCache(cache.getClassLevelAnnotations().get(0));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(DeploymentUnit deploymentUnit,
            DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
            StatefulComponentDescription description) throws DeploymentUnitProcessingException {
        final String ejbName = description.getEJBName();
        final EjbJarMetaData metaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (metaData == null) {
            return;
        }
        final AssemblyDescriptorMetaData assemblyDescriptor = metaData.getAssemblyDescriptor();
        if (assemblyDescriptor == null) {
            return;
        }
        // get the pool metadata
        final List<EJBBoundCacheMetaData> caches = assemblyDescriptor.getAny(EJBBoundCacheMetaData.class);

        String cacheName = null;
        if (caches != null) {
            for (final EJBBoundCacheMetaData cacheMetaData : caches) {
                // if this applies for all Jakarta Enterprise Beans and if there isn't a pool name already explicitly specified
                // for the specific bean (i.e. via an ejb-name match)
                if ("*".equals(cacheMetaData.getEjbName()) && cacheName == null) {
                    cacheName = cacheMetaData.getCacheName();
                } else if (ejbName.equals(cacheMetaData.getEjbName())) {
                    cacheName = cacheMetaData.getCacheName();
                }
            }
        }
        if (cacheName != null) {
            description.setCache(new CacheInfo(cacheName));
        }
    }
}
