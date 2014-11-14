/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConnectorResource extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(CommonAttributes.CONNECTOR);

    static final ConnectorResource INSTANCE = new ConnectorResource();

    //FIXME is this attribute still used?
    static final SimpleAttributeDefinition AUTHENTICATION_PROVIDER = new SimpleAttributeDefinitionBuilder(CommonAttributes.AUTHENTICATION_PROVIDER, ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(true)
            .setAttributeMarshaller(new WrappedAttributeMarshaller(Attribute.NAME))
            .addAccessConstraint(RemotingExtension.REMOTING_SECURITY_DEF)
            .build();

    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.SOCKET_BINDING, ModelType.STRING, false)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(CommonAttributes.SECURITY_REALM, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .addAccessConstraint(RemotingExtension.REMOTING_SECURITY_DEF)
            .setNullSignficant(true)
            .build();

    private ConnectorResource() {
        super(PATH, RemotingExtension.getResourceDescriptionResolver(CONNECTOR),
                ConnectorAdd.INSTANCE, ConnectorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(AUTHENTICATION_PROVIDER,
                SOCKET_BINDING, SECURITY_REALM);
        resourceRegistration.registerReadWriteAttribute(AUTHENTICATION_PROVIDER, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SECURITY_REALM, null, writeHandler);
    }
}
