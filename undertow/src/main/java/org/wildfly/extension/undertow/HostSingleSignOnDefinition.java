/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
