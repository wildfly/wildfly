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
package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISCOVERY_OPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISCOVERY_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATIC_DISCOVERY;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.discovery.DiscoveryOptionsResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Handles changes to the discovery-options attribute that maintains the order
 * of the discovery options.
 *
 * @author Farah Juma
 */
public class DiscoveryOptionsWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public DiscoveryOptionsWriteAttributeHandler() {
        super(DiscoveryOptionsResourceDefinition.DISCOVERY_OPTIONS);
    }

    /**
     * Validates that the new options list only contains existing options and contains all of them.
     *
     * {@inheritDoc}
     */
    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource resource) throws OperationFailedException {
        final ModelNode model = Resource.Tools.readModel(resource);

        // Get the existing discovery option types and names
        ModelNode unorderedDiscoveryOptions = new ModelNode();
        if (model.hasDefined(DISCOVERY_OPTION)) {
            final ModelNode discoveryOptions = model.get(DISCOVERY_OPTION);
            for (Property prop : discoveryOptions.asPropertyList()) {
                unorderedDiscoveryOptions.add(DISCOVERY_OPTION, prop.getName());
            }
        }
        if (model.hasDefined(STATIC_DISCOVERY)) {
            final ModelNode staticDiscoveryOptions = model.get(STATIC_DISCOVERY);
            for (Property prop : staticDiscoveryOptions.asPropertyList()) {
                unorderedDiscoveryOptions.add(STATIC_DISCOVERY, prop.getName());
            }
        }

        // Make sure the new value is only made up of existing discovery options
        List<ModelNode> newValueList = newValue.isDefined() ? newValue.asList() : Collections.<ModelNode>emptyList();
        List<ModelNode> unorderedDiscoveryOptionsList = unorderedDiscoveryOptions.isDefined() ? unorderedDiscoveryOptions.asList() : Collections.<ModelNode>emptyList();
        if (newValueList.size() != unorderedDiscoveryOptionsList.size() || !newValueList.containsAll(unorderedDiscoveryOptionsList)) {
            throw MESSAGES.invalidDiscoveryOptionsOrdering(DISCOVERY_OPTIONS);
        }
    }
}
