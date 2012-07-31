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
package org.jboss.as.osgi.naming;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.osgi.framework.BundleContext;

/**
 * Detect @Resource BundleContext injection point and creates the appropriate module dependencies.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Aug-2011
 */
public class BundleContextBindingProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier ORG_JBOSS_OSGI_RESOLVER = ModuleIdentifier.create("org.jboss.osgi.resolver");
    private static final ModuleIdentifier ORG_OSGI_CORE = ModuleIdentifier.create("org.osgi.core");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CompositeIndex compositeIndex = depUnit.getAttachment(COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            LOGGER.warnCannotFindAnnotationIndex(depUnit);
            return;
        }

        // Check if we have a BundleContext injection point
        boolean hasBundleContextResource = false;
        DotName resourceDotName = DotName.createSimple(Resource.class.getName());
        DotName targetDotName = DotName.createSimple(BundleContext.class.getName());
        List<AnnotationInstance> anList = compositeIndex.getAnnotations(resourceDotName);
        for (AnnotationInstance an : anList) {
            AnnotationTarget anTarget = an.target();
            if (anTarget instanceof FieldInfo) {
                FieldInfo fieldInfo = (FieldInfo) anTarget;
                Type targetType = fieldInfo.type();
                if (targetType.name().equals(targetDotName)) {
                    hasBundleContextResource = true;
                    break;
                }
            }
            // [TODO] Method injection
        }

        if (hasBundleContextResource) {
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            ModuleSpecification moduleSpecification = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            ModuleDependency coreDep = new ModuleDependency(moduleLoader, ORG_OSGI_CORE, false, false, false, false);
            ModuleDependency resolverDep = new ModuleDependency(moduleLoader, ORG_JBOSS_OSGI_RESOLVER, false, false, false, false);
            moduleSpecification.addSystemDependencies(Arrays.asList(coreDep, resolverDep));
            // Add a dependency on the binder service
            ServiceName binderServiceName = BundleContextBindingService.getBinderServiceName();
            phaseContext.addDependency(binderServiceName, AttachmentKey.create(ManagedReferenceFactory.class));
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
