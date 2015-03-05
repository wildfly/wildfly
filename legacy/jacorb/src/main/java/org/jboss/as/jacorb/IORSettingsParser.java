/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import java.util.List;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * <p>
 * This class implements a parser for the {@code IORSettingsDefinition} resource.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class IORSettingsParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

    static final IORSettingsParser INSTANCE = new IORSettingsParser();

    static final PersistentResourceXMLDescription xmlDescription = builder(IORSettingsDefinition.INSTANCE)
            .setXmlElementName(JacORBSubsystemConstants.IOR_SETTINGS)
            .addChild(builder(IORTransportConfigDefinition.INSTANCE)
                            .addAttribute(IORTransportConfigDefinition.INTEGRITY)
                            .addAttribute(IORTransportConfigDefinition.CONFIDENTIALITY)
                            .addAttribute(IORTransportConfigDefinition.TRUST_IN_CLIENT)
                            .addAttribute(IORTransportConfigDefinition.TRUST_IN_TARGET)
                            .addAttribute(IORTransportConfigDefinition.DETECT_REPLAY)
                            .addAttribute(IORTransportConfigDefinition.DETECT_MISORDERING)
            )
            .addChild(builder(IORASContextDefinition.INSTANCE)
                            .addAttribute(IORASContextDefinition.AUTH_METHOD)
                            .addAttribute(IORASContextDefinition.REALM)
                            .addAttribute(IORASContextDefinition.REQUIRED)
            )
            .addChild(builder(IORSASContextDefinition.INSTANCE)
                            .addAttribute(IORSASContextDefinition.CALLER_PROPAGATION)
            )
            .build();

    private IORSettingsParser() {
    }

    @Override
    public void readElement(final XMLExtendedStreamReader xmlExtendedStreamReader, final List<ModelNode> modelNodes) throws XMLStreamException {
        xmlDescription.parse(xmlExtendedStreamReader, PathAddress.pathAddress(JacORBSubsystemResource.INSTANCE.getPathElement()), modelNodes);
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter xmlExtendedStreamWriter, final ModelNode modelNode) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(IORSettingsDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(modelNode);
        xmlDescription.persist(xmlExtendedStreamWriter, model);
    }
}

