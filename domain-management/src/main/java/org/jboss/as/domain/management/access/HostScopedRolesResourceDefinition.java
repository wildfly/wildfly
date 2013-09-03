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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;

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
import org.jboss.as.controller.access.constraint.HostEffectConstraint;
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
 * scoped to a particular set of hosts.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class HostScopedRolesResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(HOST_SCOPED_ROLE);

    public static final SimpleAttributeDefinition BASE_ROLE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BASE_ROLE, ModelType.STRING)
            .setRestartAllServices()
            .build();


    public static final ListAttributeDefinition HOSTS = SimpleListAttributeDefinition.Builder.of(ModelDescriptionConstants.HOSTS,
                new SimpleAttributeDefinitionBuilder(HOST, ModelType.STRING)
                        .setAttributeMarshaller(new AttributeMarshaller() {
                            @Override
                            public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                                writer.writeEmptyElement(Element.HOST.getLocalName());
                                writer.writeAttribute(Attribute.NAME.getLocalName(), resourceModel.asString());
                            }
                        }).build())
            .setAllowNull(true)
            .setWrapXmlList(false)
            .build();

    private final HostScopedRoleAdd addHandler;
    private final HostScopedRoleRemove removeHandler;
    private final HostScopedRoleWriteAttributeHandler writeAttributeHandler;

    public HostScopedRolesResourceDefinition(WritableAuthorizerConfiguration authorizerConfiguration) {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control.host-scoped-role"));

        Map<String, HostEffectConstraint> constraintMap = new HashMap<String, HostEffectConstraint>();
        this.addHandler = new HostScopedRoleAdd(constraintMap, authorizerConfiguration);
        this.removeHandler =  new HostScopedRoleRemove(constraintMap, authorizerConfiguration);
        this.writeAttributeHandler = new HostScopedRoleWriteAttributeHandler(constraintMap);
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
        resourceRegistration.registerReadWriteAttribute(HOSTS, null, writeAttributeHandler);
    }
}
