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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.iiop.openjdk;

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
 * <p>
 * This class implements a parser for the IIOP subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    static final IIOPSubsystemParser INSTANCE = new IIOPSubsystemParser();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(IIOPRootDefinition.INSTANCE)
                .addChild(
                        builder(ORBDefinition.INSTANCE)
                                .addAttributes(ORBDefinition.PERSISTENT_SERVER_ID, ORBDefinition.GIOP_VERSION,
                                        ORBDefinition.SOCKET_BINDING, ORBDefinition.SSL_SOCKET_BINDING)
                                .addChild(
                                        builder(TCPDefinition.INSTANCE).addAttributes(TCPDefinition.HIGH_WATER_MARK,
                                                TCPDefinition.NUMBER_TO_RECLAIM))
                                .addChild(
                                        builder(InitializersDefinition.INSTANCE).addAttributes(InitializersDefinition.SECURITY,
                                                InitializersDefinition.TRANSACTIONS)))
                .addChild(
                        builder(NamingDefinition.INSTANCE).addAttributes(NamingDefinition.EXPORT_CORBALOC,
                                NamingDefinition.ROOT_CONTEXT))
                .addChild(
                        builder(SecurityDefinition.INSTANCE).addAttributes(SecurityDefinition.SUPPORT_SSL,
                                SecurityDefinition.SECURITY_DOMAIN, SecurityDefinition.ADD_COMPONENT_INTERCEPTOR,
                                SecurityDefinition.CLIENT_SUPPORTS, SecurityDefinition.CLIENT_REQUIRES,
                                SecurityDefinition.SERVER_SUPPORTS, SecurityDefinition.SERVER_REQUIRES))
                .addChild(
                        builder(IORSettingsDefinition.INSTANCE)
                                .addChild(
                                        builder(IORTransportConfigDefinition.INSTANCE).addAttributes(
                                                IORTransportConfigDefinition.INTEGRITY,
                                                IORTransportConfigDefinition.CONFIDENTIALITY,
                                                IORTransportConfigDefinition.TRUST_IN_CLIENT,
                                                IORTransportConfigDefinition.TRUST_IN_TARGET,
                                                IORTransportConfigDefinition.DETECT_REPLAY,
                                                IORTransportConfigDefinition.DETECT_MISORDERING))
                                .addChild(
                                        builder(IORASContextDefinition.INSTANCE).addAttributes(
                                                IORASContextDefinition.AUTH_METHOD, IORASContextDefinition.REALM,
                                                IORASContextDefinition.REQUIRED))
                                .addChild(
                                        builder(IORSASContextDefinition.INSTANCE).addAttribute(
                                                IORSASContextDefinition.CALLER_PROPAGATION)))
                .addChild(
                        builder(PropertiesDefinition.INSTANCE).addChild(
                                builder(PropertyDefinition.INSTANCE).addAttributes(PropertyDefinition.VALUE)))
                .build();
    }

    /**
     * <p>
     * Private constructor required by the {@code Singleton} pattern.
     * </p>
     */
    private IIOPSubsystemParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(IIOPRootDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());
        xmlDescription.persist(writer, model, Namespace.CURRENT.getUriString());
    }
}