/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.network.OutboundSocketBinding;

/**
 * Definition of an election policy resource.
 * @author Paul Ferraro
 */
public class ElectionPolicyResourceDefinition extends SimpleResourceDefinition implements Registration {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("election-policy", value);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        SOCKET_BINDING_PREFERENCE("org.wildfly.clustering.singleton-deployer.deployment-policy.election-policy.socket-binding-preference", OutboundSocketBinding.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> serviceType) {
            this.definition = RuntimeCapability.Builder.of(name, true).setServiceType(serviceType).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> getRuntimeCapability(PathAddress address) {
            return this.definition.fromBaseCapability(address.getParent().getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        NAME_PREFERENCES("name-preferences", "socket-binding-preferences"),
        SOCKET_BINDING_PREFERENCES("socket-binding-preferences", "name-preferences", new CapabilityReference(RequiredCapability.OUTBOUND_SOCKET_BINDING, Capability.SOCKET_BINDING_PREFERENCE)),
        ;
        private final AttributeDefinition definition;

        private Attribute(String name, String alternative) {
            this(name, alternative, null);
        }

        private Attribute(String name, String alternative, CapabilityReferenceRecorder capability) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setAlternatives(alternative)
                    .setCapabilityReference(capability)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    ElectionPolicyResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(path, resolver);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerSubModel(this);
    }
}
