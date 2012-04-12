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
package org.jboss.as.osgi.deployment;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.naming.deployment.ContextNames.contextServiceNameOfComponent;
import static org.jboss.as.naming.deployment.ContextNames.contextServiceNameOfModule;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX;

import java.util.List;

import javax.annotation.Resource;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleContext;

/**
 * Processor responsible for binding OSGi resources to JNDI. </p> Unlike other resource injections this binding happens for all
 * eligible components, regardless of the presence of the {@link javax.annotation.Resource} annotation.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Aug-2011
 */
public class BundleContextJndiBindingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDesc = depUnit.getAttachment(EE_MODULE_DESCRIPTION);
        if (moduleDesc == null) {
            return;
        }

        final CompositeIndex compositeIndex = depUnit.getAttachment(COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            LOGGER.warnCannotFindAnnotationIndex(depUnit);
            return;
        }

        // Check if we have a BundleContext injection point
        boolean hasBundleContextResource = false;
        final DotName resourceDotName = DotName.createSimple(Resource.class.getName());
        final DotName targetDotName = DotName.createSimple(BundleContext.class.getName());
        final List<AnnotationInstance> anList = compositeIndex.getAnnotations(resourceDotName);
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
        }
        if (hasBundleContextResource == false) {
            return;
        }

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        if (DeploymentTypeMarker.isType(DeploymentType.WAR, depUnit)) {
            ServiceName serviceName = contextServiceNameOfModule(moduleDesc.getApplicationName(), moduleDesc.getModuleName());
            bindServices(depUnit, serviceTarget, moduleDesc, moduleDesc.getModuleName(), serviceName);
        }

        for (ComponentDescription comp : moduleDesc.getComponentDescriptions()) {
            if (comp.getNamingMode() == ComponentNamingMode.CREATE) {
                ServiceName serviceName = contextServiceNameOfComponent(moduleDesc.getApplicationName(), moduleDesc.getModuleName(), comp.getComponentName());
                bindServices(depUnit, serviceTarget, moduleDesc, comp.getComponentName(), serviceName);
            }
        }
    }

    /**
     * Binds the java:comp/BundleContext service
     *
     * @param depUnit The deployment unit
     * @param serviceTarget The service target
     * @param contextServiceName The service name of the context to bind to
     */
    private void bindServices(DeploymentUnit depUnit, ServiceTarget serviceTarget, EEModuleDescription description, String componentName, ServiceName contextServiceName) {
        final ServiceName serviceName = contextServiceName.append("BundleContext");
        BinderService binderService = new BinderService("BundleContext");
        LOGGER.debugf("Install BundleContext binder service: %s", binderService);
        ServiceBuilder<ManagedReferenceFactory> builder = serviceTarget.addService(serviceName, binderService);
        builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, new ManagedReferenceInjector<BundleContext>(binderService.getManagedObjectInjector()));
        builder.addDependency(contextServiceName, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(Services.FRAMEWORK_ACTIVATOR);
        builder.install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
