/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ServerGroupEffectConstraint;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for an administrative role that is
 * scoped to a particular set of server groups.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ServerGroupScopedRoleResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(SERVER_GROUP_SCOPED_ROLE);

    public static final SimpleAttributeDefinition BASE_ROLE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BASE_ROLE, ModelType.STRING)
            .setRestartAllServices()
            .build();


    public static final ListAttributeDefinition SERVER_GROUPS =
            SimpleListAttributeDefinition.Builder.of(ModelDescriptionConstants.SERVER_GROUPS,
                    new SimpleAttributeDefinitionBuilder(SERVER_GROUP, ModelType.STRING)
                        .setAttributeMarshaller(new AttributeMarshaller() {
                            @Override
                            public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                                writer.writeEmptyElement(Element.SERVER_GROUP.getLocalName());
                                writer.writeAttribute(Attribute.NAME.getLocalName(), resourceModel.asString());
                            }
                        })
                        .build())
            .setMinSize(1)
            .setWrapXmlList(false)
            .build();

    private final ServerGroupScopedRoleAdd addHandler;
    private final ServerGroupScopedRoleRemove removeHandler;
    private final ServerGroupScopedRoleWriteAttributeHandler writeAttributeHandler;

    public ServerGroupScopedRoleResourceDefinition(WritableAuthorizerConfiguration authorizerConfiguration) {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control.server-group-scoped-role"));

        Map<String, ServerGroupEffectConstraint> constraintMap = new HashMap<String, ServerGroupEffectConstraint>();
        this.addHandler = new ServerGroupScopedRoleAdd(constraintMap, authorizerConfiguration);
        this.removeHandler =  new ServerGroupScopedRoleRemove(constraintMap, authorizerConfiguration);
        this.writeAttributeHandler = new ServerGroupScopedRoleWriteAttributeHandler(constraintMap);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        registerAddOperation(resourceRegistration, addHandler);
        OperationDefinition removeDef = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, getResourceDescriptionResolver())
                .build();
        resourceRegistration.registerOperationHandler(removeDef, removeHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        resourceRegistration.registerReadWriteAttribute(BASE_ROLE, null, new ReloadRequiredWriteAttributeHandler(BASE_ROLE));
        resourceRegistration.registerReadWriteAttribute(SERVER_GROUPS, null, writeAttributeHandler);
    }
}
