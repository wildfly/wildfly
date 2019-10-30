/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security.elytron;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RestartParentWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.security.Constants;
import org.jboss.as.security.SecurityExtension;
import org.jboss.msc.service.ServiceName;

/**
 * This {@link SimpleResourceDefinition} implementation contains code that is common to all elytron integration resources.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class BasicResourceDefinition extends SimpleResourceDefinition {

    private final String pathKey;
    private final RuntimeCapability<?> firstCapability;
    private final AttributeDefinition[] attributes;

    BasicResourceDefinition(String pathKey, ResourceDescriptionResolver resourceDescriptionResolver, AbstractAddStepHandler add, AttributeDefinition[] attributes, RuntimeCapability<?>... runtimeCapabilities) {
        super(new Parameters(PathElement.pathElement(pathKey),
                resourceDescriptionResolver)
                .setAddHandler(add)
                .setRemoveHandler(new ServiceRemoveStepHandler(add, runtimeCapabilities))
                .setAddRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setCapabilities(runtimeCapabilities)
                .addAccessConstraints(new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(SecurityExtension.SUBSYSTEM_NAME, Constants.ELYTRON_SECURITY, true, true, true)),
                    new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(SecurityExtension.SUBSYSTEM_NAME, Constants.ELYTRON_SECURITY, false))));


        this.pathKey = pathKey;
        this.firstCapability = runtimeCapabilities[0];
        this.attributes = attributes;
    }

    BasicResourceDefinition(String pathKey, AbstractAddStepHandler add, AttributeDefinition[] attributes, RuntimeCapability<?> ... runtimeCapabilities) {
        this(pathKey, SecurityExtension.getResourceDescriptionResolver(pathKey), add, attributes, runtimeCapabilities);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (attributes != null && attributes.length > 0) {
            WriteAttributeHandler write = new WriteAttributeHandler(pathKey, attributes);
            for (AttributeDefinition current : attributes) {
                resourceRegistration.registerReadWriteAttribute(current, null, write);
            }
        }
    }

    private class WriteAttributeHandler extends RestartParentWriteAttributeHandler {

        WriteAttributeHandler(String parentName, AttributeDefinition ... attributes) {
            super(parentName, attributes);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress pathAddress) {
            return firstCapability.fromBaseCapability(pathAddress.getLastElement().getValue()).getCapabilityServiceName();
        }
    }
}
