/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.cache.CacheInfo;
import org.jboss.as.ejb3.cache.EJBBoundCacheMetaData;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.Cache;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

public class CacheMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {

    public CacheMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses,
            DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
            StatefulComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
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
            componentConfiguration.setCache(cache.getClassLevelAnnotations().get(0));
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
                // if this applies for all EJBs and if there isn't a pool name already explicitly specified
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
