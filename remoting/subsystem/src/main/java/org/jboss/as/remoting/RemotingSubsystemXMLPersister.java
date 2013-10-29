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

package org.jboss.as.remoting;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static org.jboss.as.remoting.CommonAttributes.*;

/**
 * Persister for remoting subsystem 2.0 version
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
class RemotingSubsystemXMLPersister implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static final RemotingSubsystemXMLPersister INSTANCE = new RemotingSubsystemXMLPersister();

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        final ModelNode model = context.getModelNode();

        RemotingSubsystem20Parser.ENDPOINT_PARSER.persist(writer, model);

        writeWorkerThreadPoolIfAttributesSet(writer, model);

        if (model.hasDefined(CONNECTOR)) {
            final ModelNode connector = model.get(CONNECTOR);
            for (String name : connector.keys()) {
                writeConnector(writer, connector.require(name), name);
            }
        }


        if (model.hasDefined(HTTP_CONNECTOR)) {
            final ModelNode connector = model.get(HTTP_CONNECTOR);
            for (String name : connector.keys()) {
                writeHttpConnector(writer, connector.require(name), name);
            }
        }

        if (model.hasDefined(OUTBOUND_CONNECTION) || model.hasDefined(REMOTE_OUTBOUND_CONNECTION) || model.hasDefined(LOCAL_OUTBOUND_CONNECTION)) {
            // write <outbound-connections> element
            writer.writeStartElement(Element.OUTBOUND_CONNECTIONS.getLocalName());

            if (model.hasDefined(OUTBOUND_CONNECTION)) {
                final List<Property> outboundConnections = model.get(OUTBOUND_CONNECTION).asPropertyList();
                for (Property property : outboundConnections) {
                    final String connectionName = property.getName();
                    // get the specific outbound-connection
                    final ModelNode genericOutboundConnectionModel = property.getValue();
                    // process and write outbound connection
                    this.writeOutboundConnection(writer, connectionName, genericOutboundConnectionModel);
                }
            }
            if (model.hasDefined(REMOTE_OUTBOUND_CONNECTION)) {
                final List<Property> remoteOutboundConnections = model.get(REMOTE_OUTBOUND_CONNECTION).asPropertyList();
                for (Property property : remoteOutboundConnections) {
                    final String connectionName = property.getName();
                    // get the specific remote outbound connection
                    final ModelNode remoteOutboundConnectionModel = property.getValue();
                    // process and write remote outbound connection
                    this.writeRemoteOutboundConnection(writer, connectionName, remoteOutboundConnectionModel);
                }
            }
            if (model.hasDefined(LOCAL_OUTBOUND_CONNECTION)) {
                final List<Property> localOutboundConnections = model.get(LOCAL_OUTBOUND_CONNECTION).asPropertyList();
                for (Property property : localOutboundConnections) {
                    final String connectionName = property.getName();
                    // get the specific local outbound connection
                    final ModelNode localOutboundConnectionModel = property.getValue();
                    // process and write local outbound connection
                    this.writeLocalOutboundConnection(writer, connectionName, localOutboundConnectionModel);

                }
            }
            // </outbound-connections>
            writer.writeEndElement();
        }

        writer.writeEndElement();

    }

    private void writeWorkerThreadPoolIfAttributesSet(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(CommonAttributes.WORKER_READ_THREADS) || node.hasDefined(CommonAttributes.WORKER_TASK_CORE_THREADS) || node.hasDefined(CommonAttributes.WORKER_TASK_KEEPALIVE) ||
                node.hasDefined(CommonAttributes.WORKER_TASK_LIMIT) || node.hasDefined(CommonAttributes.WORKER_TASK_MAX_THREADS) || node.hasDefined(CommonAttributes.WORKER_WRITE_THREADS)) {

            writer.writeStartElement(Element.WORKER_THREAD_POOL.getLocalName());

            RemotingSubsystemRootResource.WORKER_READ_THREADS.marshallAsAttribute(node, false, writer);
            RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS.marshallAsAttribute(node, false, writer);
            RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE.marshallAsAttribute(node, false, writer);
            RemotingSubsystemRootResource.WORKER_TASK_LIMIT.marshallAsAttribute(node, false, writer);
            RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS.marshallAsAttribute(node, false, writer);
            RemotingSubsystemRootResource.WORKER_WRITE_THREADS.marshallAsAttribute(node, false, writer);

            writer.writeEndElement();
        }

    }

    private void writeConnector(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.CONNECTOR.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);

        ConnectorResource.SOCKET_BINDING.marshallAsAttribute(node, writer);
        if (node.hasDefined(SECURITY_REALM)) {
            writer.writeAttribute(Attribute.SECURITY_REALM.getLocalName(), node.require(SECURITY_REALM).asString());
        }
        ConnectorResource.AUTHENTICATION_PROVIDER.marshallAsElement(node, writer);


        if (node.hasDefined(PROPERTY)) {
            writeProperties(writer, node.get(PROPERTY));
        }
        if (node.hasDefined(SECURITY) && node.get(SECURITY).hasDefined(SASL)) {
            writeSasl(writer, node.get(SECURITY, SASL));
        }
        writer.writeEndElement();
    }


    private void writeHttpConnector(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.HTTP_CONNECTOR.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);

        HttpConnectorResource.CONNECTOR_REF.marshallAsAttribute(node, writer);
        if (node.hasDefined(SECURITY_REALM)) {
            HttpConnectorResource.SECURITY_REALM.marshallAsAttribute(node, writer);
        }
        ConnectorResource.AUTHENTICATION_PROVIDER.marshallAsElement(node, writer);

        if (node.hasDefined(PROPERTY)) {
            writeProperties(writer, node.get(PROPERTY));
        }
        if (node.hasDefined(SECURITY) && node.get(SECURITY).hasDefined(SASL)) {
            writeSasl(writer, node.get(SECURITY, SASL));
        }
        writer.writeEndElement();
    }

    private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.PROPERTIES.getLocalName());
        for (Property prop : node.asPropertyList()) {
            writer.writeStartElement(Element.PROPERTY.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
            PropertyResource.VALUE.marshallAsAttribute(prop.getValue(), writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeSasl(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.SASL.getLocalName());
        SaslResource.INCLUDE_MECHANISMS_ATTRIBUTE.marshallAsElement(node, writer);
        SaslResource.QOP_ATTRIBUTE.marshallAsElement(node, writer);
        SaslResource.STRENGTH_ATTRIBUTE.marshallAsElement(node, writer);
        SaslResource.SERVER_AUTH_ATTRIBUTE.marshallAsElement(node, writer);
        SaslResource.REUSE_SESSION_ATTRIBUTE.marshallAsElement(node, writer);

        if (node.hasDefined(SASL_POLICY)) {
            writePolicy(writer, node.get(SASL_POLICY));
        }
        if (node.hasDefined(PROPERTY)) {
            writeProperties(writer, node.get(PROPERTY));
        }

        writer.writeEndElement();
    }

    private void writePolicy(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.POLICY.getLocalName());
        final ModelNode policy = node.get(POLICY);
        SaslPolicyResource.FORWARD_SECRECY.marshallAsElement(policy, writer);
        SaslPolicyResource.NO_ACTIVE.marshallAsElement(policy, writer);
        SaslPolicyResource.NO_ANONYMOUS.marshallAsElement(policy, writer);
        SaslPolicyResource.NO_DICTIONARY.marshallAsElement(policy, writer);
        SaslPolicyResource.NO_PLAIN_TEXT.marshallAsElement(policy, writer);
        SaslPolicyResource.PASS_CREDENTIALS.marshallAsElement(policy, writer);
        writer.writeEndElement();
    }

    private void writeOutboundConnection(final XMLExtendedStreamWriter writer, final String connectionName, final ModelNode model) throws XMLStreamException {
        // <outbound-connection>
        writer.writeStartElement(Element.OUTBOUND_CONNECTION.getLocalName());

        writer.writeAttribute(Attribute.NAME.getLocalName(), connectionName);

        final String uri = model.get(URI).asString();
        writer.writeAttribute(Attribute.URI.getLocalName(), uri);

        // write the connection-creation-options if any
        if (model.hasDefined(PROPERTY)) {
            writeProperties(writer, model.get(PROPERTY));
        }

        // </outbound-connection>
        writer.writeEndElement();
    }

    private void writeRemoteOutboundConnection(final XMLExtendedStreamWriter writer, final String connectionName, final ModelNode model) throws XMLStreamException {
        // <remote-outbound-connection>
        writer.writeStartElement(Element.REMOTE_OUTBOUND_CONNECTION.getLocalName());

        writer.writeAttribute(Attribute.NAME.getLocalName(), connectionName);

        final String outboundSocketRef = model.get(OUTBOUND_SOCKET_BINDING_REF).asString();
        writer.writeAttribute(Attribute.OUTBOUND_SOCKET_BINDING_REF.getLocalName(), outboundSocketRef);

        if (model.hasDefined(CommonAttributes.USERNAME)) {
            writer.writeAttribute(Attribute.USERNAME.getLocalName(), model.require(CommonAttributes.USERNAME).asString());
        }

        if (model.hasDefined(CommonAttributes.SECURITY_REALM)) {
            writer.writeAttribute(Attribute.SECURITY_REALM.getLocalName(), model.require(SECURITY_REALM).asString());
        }

        if (model.hasDefined(CommonAttributes.PROTOCOL)) {
            writer.writeAttribute(Attribute.PROTOCOL.getLocalName(), model.require(PROTOCOL).asString());
        }
        // write the connection-creation-options if any
        if (model.hasDefined(PROPERTY)) {
            writeProperties(writer, model.get(PROPERTY));
        }

        // </remote-outbound-connection>
        writer.writeEndElement();
    }

    private void writeLocalOutboundConnection(final XMLExtendedStreamWriter writer, final String connectionName, final ModelNode model) throws XMLStreamException {
        // <local-outbound-connection>
        writer.writeStartElement(Element.LOCAL_OUTBOUND_CONNECTION.getLocalName());

        writer.writeAttribute(Attribute.NAME.getLocalName(), connectionName);

        final String outboundSocketRef = model.get(OUTBOUND_SOCKET_BINDING_REF).asString();
        writer.writeAttribute(Attribute.OUTBOUND_SOCKET_BINDING_REF.getLocalName(), outboundSocketRef);

        // write the connection-creation-options if any
        if (model.hasDefined(PROPERTY)) {
            writeProperties(writer, model.get(PROPERTY));
        }

        // </local-outbound-connection>
        writer.writeEndElement();
    }

}
