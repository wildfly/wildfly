/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import java.util.Set;

/**
 * Handler to remove resource at /subsystem=ejb3/passivation-store=X
 *
 * In order to work with capabilities from org.wildfly.clustering.infinispan.spi, the handler is modified
 * to remove capabilities for the resource before the resource itself is removed.
 *
 * @author Paul Ferraro
 */
@Deprecated
public class PassivationStoreRemove extends ServiceRemoveStepHandler {

    public PassivationStoreRemove(final AbstractAddStepHandler addOperation) {
        super(addOperation);
    }

    @Override
    protected ServiceName serviceName(final String name) {
        return ServiceNameFactory.resolveServiceName(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, name);
    }

    /**
     *  Override AbstractRemoveStepHandler.performRemove() to remove capabilities before removing child resources
     */
    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        if (removeInCurrentStep(resource)) {
            // We need to remove capabilities *before* removing the resource, since the capability reference resolution might involve reading the resource
            Set<RuntimeCapability> capabilitySet = context.getResourceRegistration().getCapabilities();
            PathAddress address = context.getCurrentAddress();

            // deregister capabilities which will no longer be available after remove
            for (RuntimeCapability capability : capabilitySet) {
                if (capability.isDynamicallyNamed()) {
                    context.deregisterCapability(capability.getDynamicName(address));
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
