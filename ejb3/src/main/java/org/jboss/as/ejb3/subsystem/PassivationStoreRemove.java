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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderServiceNameProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Handler to remove resource at /subsystem=ejb3/passivation-store=X
 *
 * In order to work with capabilities from org.wildfly.clustering.infinispan.spi, the handler is modified
 * to remove capabilities for the resource before the resource itself is removed.
 *
 * @author Paul Ferraro
 */
public class PassivationStoreRemove extends ServiceRemoveStepHandler {

    private final Set<RuntimeCapability> unavailableCapabilities;

    public PassivationStoreRemove(final AbstractAddStepHandler addOperation) {
        super(addOperation);
        this.unavailableCapabilities = new LinkedHashSet<>();
    }

    public PassivationStoreRemove(final AbstractAddStepHandler addOperation, RuntimeCapability ...  unavailableCapabilities) {
        super(addOperation, unavailableCapabilities);
        this.unavailableCapabilities = new LinkedHashSet<>(Arrays.asList(unavailableCapabilities));
    }

    @Override
    protected ServiceName serviceName(final String name) {
        return new DistributableCacheFactoryBuilderServiceNameProvider(name).getServiceName();
    }

    /**
     *  Override AbstractRemoveStepHandler.performRemove() to remove capabilities before removing child resources
     */
    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        if (removeInCurrentStep(resource)) {
            // We need to remove capabilities *before* removing the resource, since the capability reference resolution might involve reading the resource
            Set<RuntimeCapability> capabilitySet = unavailableCapabilities.isEmpty() ? context.getResourceRegistration().getCapabilities() : unavailableCapabilities;
            PathAddress address = context.getCurrentAddress();

            // deregister capabilities which will no longer be available after remove
            for (RuntimeCapability capability : capabilitySet) {
                if (capability.isDynamicallyNamed()) {
                    context.deregisterCapability(capability.getDynamicName(context.getCurrentAddress()));
                } else {
                    context.deregisterCapability(capability.getName());
                }
            }

            // remove capability requiremnts for attributes which will no longer be required after remove
            ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
            for (String attributeName : registration.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
                AttributeDefinition attribute = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName).getAttributeDefinition();
                if (attribute.hasCapabilityRequirements()) {
                    attribute.removeCapabilityRequirements(context, resource, model.get(attributeName));
                }
            }

            // remove capability requiremnts for reference recorders which will no longer be required
            for (CapabilityReferenceRecorder recorder : registration.getRequirements()) {
                recorder.removeCapabilityRequirements(context, resource, null);
            }

        }
        super.performRemove(context, operation, model);
    }

    /*
     * Determines whether resource removal happens in this step, or a subsequent step
     */
    private static boolean removeInCurrentStep(Resource resource) {
        for (String childType : resource.getChildTypes()) {
            for (Resource.ResourceEntry entry : resource.getChildren(childType)) {
                if (!entry.isRuntime() && resource.hasChild(entry.getPathElement())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        // we already unregistered our capabilities in performRemove(...)
    }
}
