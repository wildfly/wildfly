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
package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.Collection;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.modules.Module;

/**
 * Superclass for the EJB metadata merging processors
 *
 * @author Stuart Douglas
 */
public abstract class AbstractMergingProcessor<T extends EJBComponentDescription> implements DeploymentUnitProcessor {

    private final Class<T> typeParam;

    public AbstractMergingProcessor(final Class<T> typeParam) {
        this.typeParam = typeParam;
    }


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);


        if (componentConfigurations == null || componentConfigurations.isEmpty()) {
            return;
        }

        for (ComponentDescription componentConfiguration : componentConfigurations) {
            if (typeParam.isAssignableFrom(componentConfiguration.getClass())) {
                try {
                    processComponentConfig(deploymentUnit, applicationClasses, module, deploymentReflectionIndex, (T) componentConfiguration);
                } catch (Exception e) {
                    throw EjbLogger.ROOT_LOGGER.failToMergeData(componentConfiguration.getComponentName(), e);
                }
            }
        }
    }

    private void processComponentConfig(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final Module module, final DeploymentReflectionIndex deploymentReflectionIndex, final T description) throws DeploymentUnitProcessingException {

        final Class<?> componentClass;
        try {
            componentClass = module.getClassLoader().loadClass(description.getEJBClassName());
        } catch (ClassNotFoundException e) {
            throw EjbLogger.ROOT_LOGGER.failToLoadEjbClass(description.getEJBClassName(), e);
        }

        if (!MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
            handleAnnotations(deploymentUnit, applicationClasses, deploymentReflectionIndex, componentClass, description);
        }
        handleDeploymentDescriptor(deploymentUnit, deploymentReflectionIndex, componentClass, description);
    }

    /**
     * Handle annotations relating to the component that have been found in the deployment. Will not be called if the deployment is metadata complete.
     */
    protected abstract void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final T description) throws DeploymentUnitProcessingException;

    /**
     * Handle the deployment descriptor
     */
    protected abstract void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final T description) throws DeploymentUnitProcessingException;


    protected MethodIntf getMethodIntf(final MethodInterfaceType viewType, final MethodIntf defaultMethodIntf) {
        if (viewType == null) {
            return defaultMethodIntf;
        }
        switch (viewType) {
            case Home:
                return MethodIntf.HOME;
            case LocalHome:
                return MethodIntf.LOCAL_HOME;
            case ServiceEndpoint:
                return MethodIntf.SERVICE_ENDPOINT;
            case Local:
                return MethodIntf.LOCAL;
            case Remote:
                return MethodIntf.REMOTE;
            case Timer:
                return MethodIntf.TIMER;
            case MessageEndpoint:
                return MethodIntf.MESSAGE_ENDPOINT;
        }
        return defaultMethodIntf;
    }


    protected String[] getMethodParams(MethodParametersMetaData methodParametersMetaData) {
        if (methodParametersMetaData == null) {
            return null;
        }
        return methodParametersMetaData.toArray(new String[methodParametersMetaData.size()]);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
