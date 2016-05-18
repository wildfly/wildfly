/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.RestartParentResourceAddStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRemoveStepHandler;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;


/**
 * Fork-specific protocol resource definition.
 * @author Paul Ferraro
 */
public class ForkProtocolResourceDefinition extends ProtocolResourceDefinition {

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_4_1_0.requiresTransformation(version)) {
            // See WFLY-6782, add-index parameter was missing from add operation definition
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, ModelDescriptionConstants.ADD_INDEX);
        }
    }

    final boolean allowRuntimeOnlyRegistration;

    public ForkProtocolResourceDefinition(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory, boolean allowRuntimeOnlyRegistration) {
        super(parentBuilderFactory);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new ProtocolConfigurationBuilder(address));
        new RestartParentResourceAddStepHandler<ChannelFactory>(this.parentBuilderFactory, descriptor, handler) {
            @Override
            protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                super.populateModel(context, operation, resource);
                // Register runtime resource children for fork protocols
                if (ForkProtocolResourceDefinition.this.allowRuntimeOnlyRegistration && (context.getRunningMode() == RunningMode.NORMAL)) {
                    context.addStep(new ForkProtocolResourceRegistrationHandler(), OperationContext.Stage.MODEL);
                }
            }
        }.register(registration);
        new RestartParentResourceRemoveStepHandler<>(this.parentBuilderFactory, descriptor, handler).register(registration);

        for (DeprecatedAttribute attribute : DeprecatedAttribute.values()) {
            registration.registerReadOnlyAttribute(attribute.getDefinition(), null);
        }

        super.register(registration);
    }
}
