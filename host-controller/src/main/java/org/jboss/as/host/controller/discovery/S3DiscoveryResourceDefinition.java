/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.S3_DISCOVERY;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.S3DiscoveryAddHandler;
import org.jboss.as.host.controller.operations.S3DiscoveryRemoveHandler;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a resource representing an S3 discovery option.
 *
 * @author Farah Juma
 */
public class S3DiscoveryResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition ACCESS_KEY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ACCESS_KEY, ModelType.STRING)
        .setAllowNull(true)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition SECRET_ACCESS_KEY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECRET_ACCESS_KEY, ModelType.STRING)
        .setAllowNull(true)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition LOCATION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.LOCATION, ModelType.STRING)
        .setAllowNull(true)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition PREFIX = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PREFIX, ModelType.STRING)
        .setAllowNull(true)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition PRE_SIGNED_PUT_URL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PRE_SIGNED_PUT_URL, ModelType.STRING)
        .setAllowNull(true)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition PRE_SIGNED_DELETE_URL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PRE_SIGNED_DELETE_URL, ModelType.STRING)
        .setAllowNull(true)
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition[] S3_DISCOVERY_ATTRIBUTES = new SimpleAttributeDefinition[] {ACCESS_KEY, SECRET_ACCESS_KEY, LOCATION,
                                                                                                               PREFIX, PRE_SIGNED_PUT_URL, PRE_SIGNED_DELETE_URL};

    public S3DiscoveryResourceDefinition(final LocalHostControllerInfoImpl hostControllerInfo) {
        super(PathElement.pathElement(DISCOVERY_OPTION, S3_DISCOVERY), HostResolver.getResolver(S3_DISCOVERY),
                new S3DiscoveryAddHandler(hostControllerInfo),
                new S3DiscoveryRemoveHandler(),
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (final SimpleAttributeDefinition attribute : S3_DISCOVERY_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(attribute));
        }
    }
}
