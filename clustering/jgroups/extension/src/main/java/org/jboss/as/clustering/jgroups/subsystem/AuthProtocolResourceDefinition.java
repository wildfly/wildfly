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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jgroups.protocols.AUTH;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * @author Paul Ferraro
 */
public class AuthProtocolResourceDefinition extends ProtocolResourceDefinition<AUTH> {

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
    }

    AuthProtocolResourceDefinition(String name, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(pathElement(name), descriptorConfigurator.andThen(descriptor -> descriptor
                .setAddOperationTransformation(new LegacyAddOperationTransformation("auth_class"))
                .addResourceCapabilityReference(new CapabilityReference(Capability.PROTOCOL, AuthTokenResourceDefinition.Capability.AUTH_TOKEN), address -> address.getParent().getLastElement().getValue()))
            , address -> new AuthProtocolConfigurationBuilder(address), parentBuilderFactory, (parent, registration) -> {
                new PlainAuthTokenResourceDefinition().register(registration);
                new DigestAuthTokenResourceDefinition().register(registration);
                new CipherAuthTokenResourceDefinition().register(registration);
        });
    }
}
