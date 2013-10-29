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

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import static org.jboss.as.remoting.CommonAttributes.HTTP_CONNECTOR;

/**
 * @author Stuart Douglas
 */
public class HttpConnectorResource extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(CommonAttributes.HTTP_CONNECTOR);

    static final HttpConnectorResource INSTANCE = new HttpConnectorResource();

    //FIXME is this attribute still used?
    static final SimpleAttributeDefinition AUTHENTICATION_PROVIDER = new SimpleAttributeDefinitionBuilder(CommonAttributes.AUTHENTICATION_PROVIDER, ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(true)
            .setAttributeMarshaller(new WrappedAttributeMarshaller(Attribute.NAME))
            .build();

    static final SimpleAttributeDefinition CONNECTOR_REF = new SimpleAttributeDefinition(CommonAttributes.CONNECTOR_REF, ModelType.STRING, false);
    static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(
            CommonAttributes.SECURITY_REALM, ModelType.STRING, true).setValidator(
            new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).build();

    private HttpConnectorResource() {
        super(PATH, RemotingExtension.getResourceDescriptionResolver(HTTP_CONNECTOR),
                HttpConnectorAdd.INSTANCE, HttpConnectorRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(AUTHENTICATION_PROVIDER,
                CONNECTOR_REF, SECURITY_REALM);
        resourceRegistration.registerReadWriteAttribute(AUTHENTICATION_PROVIDER, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(CONNECTOR_REF, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SECURITY_REALM, null, writeHandler);
    }
}
