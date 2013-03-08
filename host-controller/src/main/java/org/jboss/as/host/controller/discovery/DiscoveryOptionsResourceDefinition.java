/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.host.controller.discovery;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.PropertyValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.as.host.controller.operations.DiscoveryOptionsWriteAttributeHandler;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a resource representing discovery options.
 *
 * @author Farah Juma
 */
public class DiscoveryOptionsResourceDefinition extends SimpleResourceDefinition {

    public static DiscoveryOptionsResourceDefinition INSTANCE = new DiscoveryOptionsResourceDefinition();

    public static final PrimitiveListAttributeDefinition DISCOVERY_OPTIONS = new PrimitiveListAttributeDefinition.Builder(ModelDescriptionConstants.DISCOVERY_OPTIONS, ModelType.PROPERTY)
        .setAllowNull(true)
        .setValidator(new PropertyValidator(false, new StringLengthValidator(1)))
        .build();

    private DiscoveryOptionsResourceDefinition() {
        super(PathElement.pathElement(CORE_SERVICE, ModelDescriptionConstants.DISCOVERY_OPTIONS), HostResolver.getResolver(ModelDescriptionConstants.DISCOVERY_OPTIONS));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(DISCOVERY_OPTIONS, null, new DiscoveryOptionsWriteAttributeHandler());
    }
}
