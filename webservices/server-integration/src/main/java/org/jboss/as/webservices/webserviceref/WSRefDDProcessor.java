/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.webserviceref;

import static org.jboss.as.ee.utils.InjectionUtils.getInjectionTarget;
import static org.jboss.as.webservices.webserviceref.WSRefUtils.processAnnotatedElement;
import static org.jboss.as.webservices.webserviceref.WSRefUtils.translate;

import java.lang.reflect.AccessibleObject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferencesMetaData;
import org.jboss.modules.Module;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;

/**
 * WebServiceRef DD processor.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSRefDDProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(final DeploymentUnit unit, final DeploymentDescriptorEnvironment environment, final ResourceInjectionTarget resourceInjectionTarget, final ComponentDescription componentDescription, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        final ServiceReferencesMetaData serviceRefsMD = environment.getEnvironment().getServiceReferences();
        if (serviceRefsMD == null) {
            return Collections.<BindingConfiguration> emptyList();
        }

        final List<BindingConfiguration> bindingDescriptions = new LinkedList<BindingConfiguration>();
        for (final ServiceReferenceMetaData serviceRefMD : serviceRefsMD) {
            final UnifiedServiceRefMetaData serviceRefUMDM = getServiceRef(unit, componentDescription, serviceRefMD);
            final Module module = unit.getAttachment(Attachments.MODULE);
            WebServiceManagedReferenceFactory factory = new WebServiceManagedReferenceFactory(serviceRefUMDM, module.getClassLoader());
            final FixedInjectionSource valueSource = new FixedInjectionSource(factory, factory);
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(serviceRefUMDM.getServiceRefName(), valueSource);
            bindingDescriptions.add(bindingConfiguration);
            final String serviceRefTypeName = serviceRefUMDM.getServiceRefType();
            final Class<?> serviceRefType = getClass(classLoader, serviceRefTypeName);
            processInjectionTargets(resourceInjectionTarget, valueSource, classLoader, deploymentReflectionIndex, serviceRefMD, serviceRefType);
        }
        return bindingDescriptions;
    }

    private static UnifiedServiceRefMetaData getServiceRef(final DeploymentUnit unit, final ComponentDescription componentDescription, final ServiceReferenceMetaData serviceRefMD) throws DeploymentUnitProcessingException {
        //check jaxrpc service refs
        if (serviceRefMD.getJaxrpcMappingFile() != null || "javax.xml.rpc.Service".equals(serviceRefMD.getServiceInterface())) {
            throw WSLogger.ROOT_LOGGER.jaxRpcNotSupported();
        }
        // construct service ref
        final UnifiedServiceRefMetaData serviceRefUMDM = translate(serviceRefMD);
        serviceRefUMDM.setVfsRoot(getUnifiedVirtualFile(unit));
        processWSFeatures(unit, serviceRefMD.getInjectionTargets(), serviceRefUMDM);
        final WSRefRegistry wsRefRegistry = ASHelper.getWSRefRegistry(unit);
        wsRefRegistry.add(getCacheKey(componentDescription, serviceRefUMDM), serviceRefUMDM);
        return serviceRefUMDM;
    }

    private static String getCacheKey(final ComponentDescription componentDescription, final UnifiedServiceRefMetaData serviceRefUMMD) {
        if (componentDescription == null) {
            return serviceRefUMMD.getServiceRefName();
        } else {
            return componentDescription.getComponentName() + "/" + serviceRefUMMD.getServiceRefName();
        }
    }


    private static void processWSFeatures(final DeploymentUnit unit, final Set<ResourceInjectionTargetMetaData> injectionTargets, final UnifiedServiceRefMetaData serviceRefUMDM) throws DeploymentUnitProcessingException {
        if (injectionTargets == null || injectionTargets.size() == 0) return;
        if (injectionTargets.size() > 1) {
           // TODO: We should validate all the injection targets whether they're compatible.
           // This means all the injection targets must be assignable or equivalent.
           // If there are @Addressing, @RespectBinding or @MTOM annotations present on injection targets,
           // these annotations must be equivalent for all the injection targets.
        }
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = unit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final ResourceInjectionTargetMetaData injectionTarget = injectionTargets.iterator().next();
        final String injectionTargetClassName = injectionTarget.getInjectionTargetClass();
        final String injectionTargetName = injectionTarget.getInjectionTargetName();
        final AccessibleObject fieldOrMethod = getInjectionTarget(injectionTargetClassName, injectionTargetName, module.getClassLoader(), deploymentReflectionIndex);
        processAnnotatedElement(fieldOrMethod, serviceRefUMDM);
    }

    private Class<?> getClass(final ClassLoader classLoader, final String className) throws DeploymentUnitProcessingException { // TODO: refactor to common code
        if (!isEmpty(className)) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        return null;
    }

    private static UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit deploymentUnit) { // TODO: refactor to common code
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        return new VirtualFileAdaptor(resourceRoot.getRoot());
    }

    private static boolean isEmpty(final String string) { // TODO: some common class - StringUtils ?
        return string == null || string.isEmpty();
    }

}
