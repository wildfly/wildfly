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
 * /
 */

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

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
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class MailSubsystemParser2_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final MailSubsystemParser2_0 INSTANCE = new MailSubsystemParser2_0();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MailSubsystemResource.INSTANCE)
                .addChild(
                        builder(MailSessionDefinition.INSTANCE)
                                .addAttributes(MailSessionDefinition.DEBUG, MailSessionDefinition.JNDI_NAME, MailSessionDefinition.FROM)
                                .addChild(
                                        builder(MailServerDefinition.INSTANCE_SMTP)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD)
                                                .setXmlElementName(MailSubsystemModel.SMTP_SERVER)

                                )
                                .addChild(
                                        builder(MailServerDefinition.INSTANCE_POP3)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD)
                                                .setXmlElementName(MailSubsystemModel.POP3_SERVER)
                                )
                                .addChild(
                                        builder(MailServerDefinition.INSTANCE_IMAP)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD)
                                                .setXmlElementName(MailSubsystemModel.IMAP_SERVER)
                                )
                                .addChild(
                                        builder(MailServerDefinition.INSTANCE_CUSTOM)
                                                .addAttributes(MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL, MailServerDefinition.SSL, MailServerDefinition.TLS, MailServerDefinition.USERNAME, MailServerDefinition.PASSWORD, MailServerDefinition.PROPERTIES)
                                                .setXmlElementName(MailSubsystemModel.CUSTOM_SERVER)
                                )
                )
                .build();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(MailSubsystemResource.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());//this is bit of workaround for SPRD to work properly
        xmlDescription.persist(writer, model, Namespace.CURRENT.getUriString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}
