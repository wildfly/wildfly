/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.clustering.controller.AdminOnlyOperationStepHandlerTransformer;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
/**
 * @author Paul Ferraro
 */
public class HostSingleSignOnDefinition extends SingleSignOnDefinition {

    public static final RuntimeCapability<Void> HOST_SSO_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_HOST_SSO, true)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            .setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT)
            .build();

    public HostSingleSignOnDefinition() {
        super();
        setDeprecated(UndertowSubsystemModel.VERSION_12_0_0.getVersion());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(SingleSignOnDefinition.Attribute.class).addCapabilities(() -> HOST_SSO_CAPABILITY)
                .setAddOperationTransformation(AdminOnlyOperationStepHandlerTransformer.INSTANCE)
                ;
        new SimpleResourceRegistrar(descriptor, null).register(registration);
    }
}
