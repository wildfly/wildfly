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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISCOVERY_OPTION;
import static org.jboss.as.host.controller.discovery.Constants.DEFAULT_MODULE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.as.host.controller.operations.DiscoveryOptionAddHandler;
import org.jboss.as.host.controller.operations.DiscoveryOptionRemoveHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a resource representing a generic discovery option.
 *
 * @author Farah Juma
 */
public class DiscoveryOptionResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CODE, ModelType.STRING)
        .setValidator(new StringLengthValidator(1))
        .build();

    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MODULE, ModelType.STRING)
        .setDefaultValue(new ModelNode(DEFAULT_MODULE))
        .setValidator(new StringLengthValidator(1))
        .build();

    public static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.PROPERTIES, true)
        .addFlag(Flag.RESTART_ALL_SERVICES)
        .setCorrector(MapAttributeDefinition.LIST_TO_MAP_CORRECTOR)
        .setAllowExpression(true)
        .build();

    public static final AttributeDefinition[] DISCOVERY_ATTRIBUTES = new AttributeDefinition[] {CODE, MODULE, PROPERTIES};

    public DiscoveryOptionResourceDefinition(final LocalHostControllerInfoImpl hostControllerInfo) {
        super(PathElement.pathElement(DISCOVERY_OPTION), HostResolver.getResolver(DISCOVERY_OPTION),
                new DiscoveryOptionAddHandler(hostControllerInfo),
                new DiscoveryOptionRemoveHandler(),
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (final AttributeDefinition attribute : DISCOVERY_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(attribute));
        }
    }
}
