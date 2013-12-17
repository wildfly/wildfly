/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.registry;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;

/**
 * {@link ManagementResourceRegistration} implementation that serves as a facade to the two MRRs registered under
 * deployment=x and deployment=x/subdeployment=y. The facade delegates any mutating operations to both of the
 * underlying MRRs, thus ensuring that both are updated and have a consistent set of child resource registrations.
 * This allows subsystems to register their deployment tree APIs without having to concern themselves with
 * subdeployments.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class DeploymentManagementResourceRegistration implements ManagementResourceRegistration {

    private final ManagementResourceRegistration deployments;
    private final ManagementResourceRegistration subdeployments;

    public DeploymentManagementResourceRegistration(final ManagementResourceRegistration deployments) {
        this(deployments, deployments.getSubModel(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT))));
    }

    public DeploymentManagementResourceRegistration(final ManagementResourceRegistration deployments,
                                                    final ManagementResourceRegistration subdeployments) {
        this.deployments = deployments;
        this.subdeployments = subdeployments;
    }

    @Override
    public boolean isRuntimeOnly() {
        return deployments.isRuntimeOnly();
    }

    @Override
    public void setRuntimeOnly(final boolean runtimeOnly) {
        deployments.setRuntimeOnly(runtimeOnly);
        subdeployments.setRuntimeOnly(runtimeOnly);
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
    public ManagementResourceRegistration getOverrideModel(String name) {
        ManagementResourceRegistration depl = deployments.getOverrideModel(name);
        ManagementResourceRegistration subdepl = subdeployments.getOverrideModel(name);
        if (depl == null) {
            return subdepl;
        } else if (subdepl == null) {
            return depl;
        }
        return new DeploymentManagementResourceRegistration(depl, subdepl);
    }

    @Override
    public ManagementResourceRegistration getSubModel(PathAddress address) {

        ManagementResourceRegistration depl = deployments.getSubModel(address);
        PathAddress subdeploymentAddress =
                (address.size() > 0 && ModelDescriptionConstants.SUBDEPLOYMENT.equals(address.getElement(0).getKey()))
                    ? address.subAddress(1) : address;
        ManagementResourceRegistration subdepl = subdeployments.getSubModel(subdeploymentAddress);
        if (depl == null) {
            return subdepl;
        } else if (subdepl == null) {
            return depl;
        }
        return new DeploymentManagementResourceRegistration(depl, subdepl);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return deployments.getAccessConstraints();
    }

    @SuppressWarnings("deprecation")
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
    public boolean isAllowsOverride() {
        return deployments.isAllowsOverride();
    }

    @Override
    public ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
        ManagementResourceRegistration depl = deployments.registerOverrideModel(name, descriptionProvider);
        ManagementResourceRegistration subdepl = subdeployments.registerOverrideModel(name, descriptionProvider);
        return new DeploymentManagementResourceRegistration(depl, subdepl);
    }

    @Override
    public void unregisterOverrideModel(String name) {
        deployments.unregisterOverrideModel(name);
        subdeployments.unregisterOverrideModel(name);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider) {
        deployments.registerOperationHandler(operationName, handler, descriptionProvider);
        subdeployments.registerOperationHandler(operationName, handler, descriptionProvider);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, EnumSet<OperationEntry.Flag> flags) {
        deployments.registerOperationHandler(operationName, handler, descriptionProvider, flags);
        subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, flags);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited) {
        deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
        subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType) {
        deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
        subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
        subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, entryType, flags);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerOperationHandler(String operationName, OperationStepHandler handler, DescriptionProvider descriptionProvider, boolean inherited, EnumSet<OperationEntry.Flag> flags) {
        deployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, flags);
        subdeployments.registerOperationHandler(operationName, handler, descriptionProvider, inherited, flags);
    }

    @Override
    public void registerOperationHandler(OperationDefinition definition, OperationStepHandler handler) {
        registerOperationHandler(definition, handler, false);
    }

    @Override
    public void registerOperationHandler(OperationDefinition definition, OperationStepHandler handler, boolean inherited) {
        deployments.registerOperationHandler(definition, handler, inherited);
        subdeployments.registerOperationHandler(definition, handler, inherited);
    }

    @Override
    public void unregisterOperationHandler(String operationName) {
        deployments.unregisterOperationHandler(operationName);
        subdeployments.unregisterOperationHandler(operationName);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, AttributeAccess.Storage storage) {
        deployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
        subdeployments.registerReadWriteAttribute(attributeName, readHandler, writeHandler, storage);
    }

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
    @Override
    public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, AttributeAccess.Storage storage) {
        deployments.registerReadOnlyAttribute(attributeName, readHandler, storage);
        subdeployments.registerReadOnlyAttribute(attributeName, readHandler, storage);
    }

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
    @Override
    public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
        deployments.registerMetric(attributeName, metricHandler, flags);
        subdeployments.registerMetric(attributeName, metricHandler, flags);
    }

    @Override
    public void unregisterAttribute(String attributeName) {
        deployments.unregisterAttribute(attributeName);
        subdeployments.unregisterAttribute(attributeName);
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

    @Override
    public void registerAlias(PathElement address, AliasEntry alias) {
        deployments.registerAlias(address, alias);
        subdeployments.registerAlias(address, alias);
    }

    @Override
    public void unregisterAlias(PathElement address) {
        deployments.unregisterAlias(address);
        subdeployments.unregisterAlias(address);
    }

    @Override
    public AliasEntry getAliasEntry() {
        return deployments.getAliasEntry();
    }

    @Override
    public boolean isAlias() {
        return deployments.isAlias();
    }
}
