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

import java.lang.reflect.Constructor;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.PrivateSubDeploymentMarker;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.WeldMessages;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.MetadataImpl;

/**
 * Deployment processor that loads CDI portable extensions.
 *
 * @author Stuart Douglas
 */
public class WeldPortableExtensionProcessor implements DeploymentUnitProcessor {

    private static final String[] EMPTY_STRING_ARRAY = {};

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // for war modules we require a beans.xml to load portable extensions
        if (PrivateSubDeploymentMarker.isPrivate(deploymentUnit)) {
            if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                return;
            }
        } else if (deploymentUnit.getParent() == null) {
            // if any sub deployments have beans.xml then the top level deployment is
            // marked as a weld deployment
            if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                return;
            }
        } else {
            // if any deployments have a beans.xml we need to load portable extensions
            // even if this one does not.
            if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit.getParent())) {
                return;
            }
        }

        // we attach extensions directly to the top level deployment
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit
                .getParent();

        final ServicesAttachment services = deploymentUnit.getAttachment(Attachments.SERVICES);

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ClassLoader oldCl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(module.getClassLoader());
            loadAttachments(services, module, topLevelDeployment);
        } finally {
            SecurityActions.setContextClassLoader(oldCl);
        }
    }

    private void loadAttachments(final ServicesAttachment servicesAttachment, Module module, DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        // now load extensions
        final DeploymentReflectionIndex index = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        final List<String> services = servicesAttachment.getServiceImplementations(Extension.class.getName());
        if (services == null) {
            return;
        }
        for (String service : services) {
            final Extension extension = loadExtension(service, index,  module.getClassLoader());
            if(extension == null) {
                continue;
            }
            Metadata<Extension> metadata = new MetadataImpl<Extension>(extension, deploymentUnit.getName());
            WeldLogger.DEPLOYMENT_LOGGER.debug("Loaded portable extension " + extension);
            deploymentUnit.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
        }
    }

    private Extension loadExtension(String serviceClassName, final DeploymentReflectionIndex index, final ClassLoader loader) throws DeploymentUnitProcessingException {
        Class<?> clazz = null;
        Class<Extension> serviceClass = null;
        try {
            clazz = loader.loadClass(serviceClassName);
            serviceClass = (Class<Extension>) clazz;
            final Constructor<Extension> ctor = index.getClassIndex(serviceClass).getConstructor(EMPTY_STRING_ARRAY);
            return ctor.newInstance();
        }  catch (ClassCastException e) {
            throw WeldMessages.MESSAGES.extensionDoesNotImplementExtension(serviceClassName, e);
        } catch (Exception e) {
            WeldLogger.DEPLOYMENT_LOGGER.couldNotLoadPortableExceptionClass(serviceClassName, e);
        }
        return null;
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
