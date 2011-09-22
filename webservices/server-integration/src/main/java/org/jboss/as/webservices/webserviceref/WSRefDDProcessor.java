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

import static org.jboss.as.webservices.util.ASHelper.getWSRefRegistry;
import static org.jboss.as.webservices.webserviceref.WSRefTranslator.translate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.metadata.javaee.spec.ServiceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferencesMetaData;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;

/**
 * WebServiceRef DD processor.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSRefDDProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(final DeploymentUnit unit, final DeploymentDescriptorEnvironment environment, final EEModuleDescription moduleDescription, final ComponentDescription componentDescription, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        final ServiceReferencesMetaData serviceRefsMD = environment.getEnvironment().getServiceReferences();
        if (serviceRefsMD == null) {
            return Collections.<BindingConfiguration> emptyList();
        }

        final List<BindingConfiguration> bindingDescriptions = new LinkedList<BindingConfiguration>();
        for (final ServiceReferenceMetaData serviceRefMD : serviceRefsMD) {
            final String serviceRefTypeName = serviceRefMD.getServiceRefType();
            final Class<?> serviceRefType = getClass(classLoader, serviceRefTypeName);
            final UnifiedServiceRefMetaData serviceRefUMDM = new UnifiedServiceRefMetaData(getUnifiedVirtualFile(unit));
            translate(serviceRefMD, serviceRefUMDM);
            final WSReferences wsRefRegistry = getWSRefRegistry(unit);
            wsRefRegistry.add(serviceRefMD.getName(), serviceRefUMDM);
            final WSRefValueSource valueSource = new WSRefValueSource(serviceRefUMDM);
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(serviceRefMD.getName(), valueSource);
            bindingDescriptions.add(bindingConfiguration);
            // TODO: do we need to process injection targets annotations here, or later is enough? (@MTOM, @HandlerChain, @Addressing & @RespectBinding)
            this.processInjectionTargets(moduleDescription, applicationClasses, valueSource, classLoader, deploymentReflectionIndex, serviceRefMD, serviceRefType);
        }
        return bindingDescriptions;
    }

    private Class<?> getClass(final ClassLoader classLoader, final String className) throws DeploymentUnitProcessingException {
        if (!isEmpty(className)) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Could not load class " + className, e);
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
