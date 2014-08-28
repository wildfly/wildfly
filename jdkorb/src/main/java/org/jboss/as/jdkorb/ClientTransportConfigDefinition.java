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

package org.jboss.as.jdkorb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.metadata.ejb.jboss.ClientTransportConfigMetaData;

/**
 * <p>
 * Defines a resource that encompasses the attributes used to configure the transport settings of the client.
 * </p>
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class ClientTransportConfigDefinition extends PersistentResourceDefinition {


    static final AttributeDefinition REQUIRES_SSL =
            new SimpleAttributeDefinitionBuilder(JdkORBSubsystemConstants.CLIENT_TRANSPORT_REQUIRES_SSL, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .setAllowExpression(true)
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(REQUIRES_SSL);

    static final ClientTransportConfigDefinition INSTANCE = new ClientTransportConfigDefinition();

    private ClientTransportConfigDefinition() {
        super(PathElement.pathElement(JdkORBSubsystemConstants.CLIENT_TRANSPORT_CONFIG, JdkORBSubsystemConstants.DEFAULT),
                JdkORBExtension.getResourceDescriptionResolver(JdkORBSubsystemConstants.CLIENT_TRANSPORT_CONFIG),
                new AbstractAddStepHandler(ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.singletonList((AccessConstraintDefinition) JdkORBSubsystemDefinitions.JDKORB_SECURITY_DEF);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    /**
     * <p>
     * Builds a {@code ClientTransportConfigMetaData} using the specified {@code OperationContext}.
     * </p>
     *
     * @param context a reference to the {@code OperationContext}.
     * @return the constructed {@code ClientTransportConfigMetaData} or {@code null} if the specified model is undefined.
     * @throws OperationFailedException if an error occurs while creating the transport metadata,
     */
    protected ClientTransportConfigMetaData getTransportConfigMetaData(final OperationContext context, final ModelNode model)
            throws OperationFailedException {
        ClientTransportConfigMetaData metaData = new ClientTransportConfigMetaData();
        metaData.setRequiresSsl(REQUIRES_SSL.resolveModelAttribute(context, model).asBoolean());
        return metaData;
    }
}
