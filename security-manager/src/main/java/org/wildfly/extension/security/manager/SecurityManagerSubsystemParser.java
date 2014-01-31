/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.security.manager.PermissionResourceDefinition.PermissionResourceXMLBuilder;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * This class implements a parser for the security manager subsystem.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class SecurityManagerSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    static final SecurityManagerSubsystemParser INSTANCE = new SecurityManagerSubsystemParser();

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(SecurityManagerRootDefinition.INSTANCE)
                .addChild(builder(DeploymentPermissionsResourceDefinition.INSTANCE)
                        .setXmlElementName(Constants.DEPLOYMENT_PERMISSIONS)
                        .addChild(builder(new PermissionSetResourceDefinition(Constants.MINIMUM_SET))
                                .setXmlElementName(Constants.MINIMUM_SET)
                                .addChild(new PermissionResourceXMLBuilder(PermissionResourceDefinition.INSTANCE)
                                        .setXmlElementName(Constants.PERMISSION)
                                        .addAttributes(
                                                PermissionResourceDefinition.CLASS,
                                                PermissionResourceDefinition.NAME,
                                                PermissionResourceDefinition.ACTIONS,
                                                PermissionResourceDefinition.MODULE
                                        )
                                )
                        )
                        .addChild(builder(new PermissionSetResourceDefinition(Constants.MAXIMUM_SET))
                                .setXmlElementName(Constants.MAXIMUM_SET)
                                .addChild(new PermissionResourceXMLBuilder(PermissionResourceDefinition.INSTANCE)
                                        .setXmlElementName(Constants.PERMISSION)
                                        .addAttributes(
                                                PermissionResourceDefinition.CLASS,
                                                PermissionResourceDefinition.NAME,
                                                PermissionResourceDefinition.ACTIONS,
                                                PermissionResourceDefinition.MODULE
                                        )
                                )
                        )
                )
                .build();
    }


    /**
     * Private constructor to enforce usage of the static singleton instance.
     */
    private SecurityManagerSubsystemParser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> modelNodes) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, modelNodes);
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(SecurityManagerRootDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());
        xmlDescription.persist(writer, model, Namespace.CURRENT.getUriString());
    }
}
