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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemXmlWriterRegistry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * A basic extension context implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ExtensionContextImpl implements ExtensionContext {

    private final ProcessType processType;
    private final ManagementResourceRegistration profileRegistration;
    private final SubsystemXmlWriterRegistry writerRegistry;
    private final ManagementResourceRegistration subsystemDeploymentRegistration;
    private final Map<String, List<String>> subsystemsByModule = new HashMap<String, List<String>>();

    /**
     * Construct a new instance.
     *
     * @param profileRegistration the profile registration
     * @param deploymentOverrideRegistration the deployment override registration
     * @param writerRegistry the registry for extension xml marshallers
     */
    public ExtensionContextImpl(final ManagementResourceRegistration profileRegistration,
            final ManagementResourceRegistration deploymentOverrideRegistration,
            final SubsystemXmlWriterRegistry writerRegistry, ProcessType processType) {
        if (profileRegistration == null) {
            throw MESSAGES.nullVar("profileRegistration");
        }
//        if (deploymentOverrideRegistration == null) {
//            throw new IllegalArgumentException("deploymentOverrideRegistration is null");
//        }
        if (writerRegistry == null) {
            throw MESSAGES.nullVar("writerRegistry");
        }
        this.processType = processType;
        this.profileRegistration = profileRegistration;
        this.writerRegistry = writerRegistry;
        if (deploymentOverrideRegistration != null) {
            PathAddress subdepAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT));
            final ManagementResourceRegistration subdeployments = deploymentOverrideRegistration.getSubModel(subdepAddress);
            this.subsystemDeploymentRegistration = subdeployments == null ? deploymentOverrideRegistration
                    :new DeploymentManagementResourceRegistration(deploymentOverrideRegistration, subdeployments);
        } else {
            this.subsystemDeploymentRegistration = null;
        }
    }

    public ExtensionContext createTracking(String moduleName) {
        return new DelegatingExtensionContext(moduleName, this);
    }


    @Override
    public ProcessType getProcessType() {
        return processType;
    }

    private void trackSubsystem(String moduleName, String subysystemName) {
        synchronized (subsystemsByModule) {
            List<String> subsystems = subsystemsByModule.get(moduleName);
            if (subsystems == null) {
                subsystems = new ArrayList<String>();
                subsystemsByModule.put(moduleName, subsystems);
            }
            subsystems.add(subysystemName);
        }
    }

    @Override
    public void cleanup(Resource rootResource, String moduleName) {
        List<String> subsystems = null;
        synchronized (subsystemsByModule) {
            subsystems = subsystemsByModule.get(moduleName);
            if (subsystems != null) {
                for (String subsystem : subsystems) {
                    if (rootResource.getChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystem)) != null) {
                        throw MESSAGES.removingExtensionWithRegisteredSubsystem(moduleName, subsystem);
                    }
                }
                for (String subsystem : subsystems) {
                    profileRegistration.unregisterSubModel(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystem));
                    subsystemDeploymentRegistration.unregisterSubModel(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystem));
                }
                subsystems.remove(moduleName);
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public SubsystemRegistration registerSubsystem(final String name) throws IllegalArgumentException {
        ROOT_LOGGER.registerSubsystemNoWraper(name);
        return doRegisterSubsystem(name);
    }

    public SubsystemRegistration doRegisterSubsystem(final String name) throws IllegalArgumentException {
        if (name == null) {
            throw MESSAGES.nullVar("name");
        }
        return new SubsystemRegistration() {
            @Override
            public ManagementResourceRegistration registerSubsystemModel(final DescriptionProvider descriptionProvider) {
                if (descriptionProvider == null) {
                    throw MESSAGES.nullVar("descriptionProvider");
                }
                PathElement pathElement = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name);
                return registerSubsystemModel(new SimpleResourceDefinition(pathElement, descriptionProvider));
            }

            @Override
            public ManagementResourceRegistration registerSubsystemModel(ResourceDefinition resourceDefinition) {
                if (resourceDefinition == null) {
                    throw MESSAGES.nullVar("descriptionProviderFactory");
                }
                return profileRegistration.registerSubModel(resourceDefinition);
            }

            @Override
            public ManagementResourceRegistration registerDeploymentModel(final DescriptionProvider descriptionProvider) {
                if (descriptionProvider == null) {
                    throw MESSAGES.nullVar("descriptionProvider");
                }
                PathElement pathElement = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name);
                return registerDeploymentModel(new SimpleResourceDefinition(pathElement, descriptionProvider));
            }

            @Override
            public ManagementResourceRegistration registerDeploymentModel(ResourceDefinition resourceDefinition) {
                if (resourceDefinition == null) {
                    throw MESSAGES.nullVar("descriptionProviderFactory");
                }
                return subsystemDeploymentRegistration.registerSubModel(resourceDefinition);
            }

            @Override
            public void registerXMLElementWriter(XMLElementWriter<SubsystemMarshallingContext> writer) {
                writerRegistry.registerSubsystemWriter(name, writer);
            }
        };
    }

    private static class DeploymentManagementResourceRegistration implements ManagementResourceRegistration {

        private final ManagementResourceRegistration deployments;
        private final ManagementResourceRegistration subdeployments;

        private DeploymentManagementResourceRegistration(final ManagementResourceRegistration deployments,
                                                         final ManagementResourceRegistration subdeployments) {
            this.deployments = deployments;
            this.subdeployments = subdeployments;
        }

        @Override
        public boolean isRuntimeOnly() {
            return deployments.isRuntimeOnly();
        }

        @Override
        public boolean isRemote() {
            return deployments.isRemote();
        }

        @Override
        public OperationStepHandler getOperationHandler(PathAddress address, String operationName) {
            return deployments.getOperationHandler(address, operationName);
        }

        @Override
        public DescriptionProvider getOperationDescription(PathAddress address, String operationName) {
            return deployments.getOperationDescription(address, operationName);
        }

        @Override
        public Set<OperationEntry.Flag> getOperationFlags(PathAddress address, String operationName) {
            return deployments.getOperationFlags(address, operationName);
        }

        @Override
        public OperationEntry getOperationEntry(PathAddress address, String operationName) {
            return deployments.getOperationEntry(address, operationName);
        }

        @Override
        public Set<String> getAttributeNames(PathAddress address) {
            return deployments.getAttributeNames(address);
        }

        @Override
        public AttributeAccess getAttributeAccess(PathAddress address, String attributeName) {
            return deployments.getAttributeAccess(address, attributeName);
        }

        @Override
        public Set<String> getChildNames(PathAddress address) {
            return deployments.getChildNames(address);
        }

        @Override
        public Set<PathElement> getChildAddresses(PathAddress address) {
            return deployments.getChildAddresses(address);
        }

        @Override
        public DescriptionProvider getModelDescription(PathAddress address) {
            return deployments.getModelDescription(address);
        }

        @Override
        public Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited) {
            return deployments.getOperationDescriptions(address, inherited);
        }

        @Override
        public ProxyController getProxyController(PathAddress address) {
            return deployments.getProxyController(address);
        }

        @Override
        public Set<ProxyController> getProxyControllers(PathAddress address) {
            return deployments.getProxyControllers(address);
        }

        @Override
        public ManagementResourceRegistration getSubModel(PathAddress address) {
            return deployments.getSubModel(address);
        }

        @Override
        public ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider) {
            ManagementResourceRegistration depl = deployments.registerSubModel(address, descriptionProvider);
            ManagementResourceRegistration subdepl = subdeployments.registerSubModel(address, descriptionProvider);
            return new DeploymentManagementResourceRegistration(depl, subdepl);
        }

        @Override
        public ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition) {
            ManagementResourceRegistration depl = deployments.registerSubModel(resourceDefinition);
            ManagementResourceRegistration subdepl = subdeployments.registerSubModel(resourceDefinition);
            return new DeploymentManagementResourceRegistration(depl, subdepl);
        }

        @Override
        public void unregisterSubModel(PathElement address) {
            deployments.unregisterSubModel(address);
            subdeployments.unregisterSubModel(address);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, EnumSet<OperationEntry.Flag> flags) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, flags);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, flags);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
            deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
            subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
        }

        @Override
        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
            deployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
            subdeployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
        }

        @Override
        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
            deployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, flags);
            subdeployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, flags);
        }

        @Override
        public void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler) {
            deployments.registerReadWriteAttribute(definition, readHandler, writeHandler);
            subdeployments.registerReadWriteAttribute(definition, readHandler, writeHandler);
        }

        @Override
        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, AttributeAccess.Storage storage) {
            deployments.registerReadOnlyAttribute(attributeName, readHandler, storage);
            subdeployments.registerReadOnlyAttribute(attributeName, readHandler, storage);
        }

        @Override
        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
            deployments.registerReadOnlyAttribute(attributeName, readHandler, flags);
            subdeployments.registerReadOnlyAttribute(attributeName, readHandler, flags);
        }

        @Override
        public void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler) {
            deployments.registerReadOnlyAttribute(definition, readHandler);
            subdeployments.registerReadOnlyAttribute(definition, readHandler);
        }

        @Override
        public void registerMetric(String attributeName, OperationStepHandler metricHandler) {
            deployments.registerMetric(attributeName, metricHandler);
            subdeployments.registerMetric(attributeName, metricHandler);
        }

        @Override
        public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {
            deployments.registerMetric(definition, metricHandler);
            subdeployments.registerMetric(definition, metricHandler);
        }

        @Override
        public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
            deployments.registerMetric(attributeName, metricHandler, flags);
            subdeployments.registerMetric(attributeName, metricHandler, flags);
        }

        @Override
        public void registerProxyController(PathElement address, ProxyController proxyController) {
            deployments.registerProxyController(address, proxyController);
            subdeployments.registerProxyController(address, proxyController);
        }

        @Override
        public void unregisterProxyController(PathElement address) {
            deployments.unregisterProxyController(address);
            subdeployments.unregisterProxyController(address);
        }
    }

    private class DelegatingExtensionContext implements ExtensionContext {
        final String moduleName;

        public DelegatingExtensionContext(String moduleName, ExtensionContext delegate) {
            this.moduleName = moduleName;
        }

        @Override
        public SubsystemRegistration registerSubsystem(String name) throws IllegalArgumentException {
            trackSubsystem(moduleName, name);
            return ExtensionContextImpl.this.registerSubsystem(name);
        }

        @Override
        public ProcessType getProcessType() {
            return ExtensionContextImpl.this.getProcessType();
        }

        @Override
        public ExtensionContext createTracking(String moduleName) {
            return this;
        }

        @Override
        public void cleanup(Resource resource, String moduleName) {
            ExtensionContextImpl.this.cleanup(resource, moduleName);
        }

    }
}
