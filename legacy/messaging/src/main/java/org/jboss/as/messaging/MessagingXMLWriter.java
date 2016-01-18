/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY;
import static org.jboss.as.messaging.ClusterConnectionDefinition.CONNECTOR_REFS;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.DIVERT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.GROUPING_HANDLER;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.HTTP_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.HTTP_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.IN_VM_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.JMS_BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.JMS_CONNECTION_FACTORIES;
import static org.jboss.as.messaging.CommonAttributes.JMS_DESTINATIONS;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.ROLE;
import static org.jboss.as.messaging.Element.SOURCE;
import static org.jboss.as.messaging.Element.TARGET;
import static org.jboss.as.messaging.Namespace.CURRENT;
import static org.jboss.as.messaging.PathDefinition.PATHS;
import static org.jboss.as.messaging.PathDefinition.RELATIVE_TO;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.messaging.jms.ConnectionFactoryAttribute;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class MessagingXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    static final MessagingXMLWriter INSTANCE = new MessagingXMLWriter();

    private static final char[] NEW_LINE = new char[]{'\n'};

    private MessagingXMLWriter() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(CURRENT.getUriString(), false);
        final ModelNode node = context.getModelNode();

        if (node.hasDefined(HORNETQ_SERVER)) {
            final ModelNode servers = node.get(HORNETQ_SERVER);
            boolean first = true;
            for (String name : servers.keys()) {
                writeHornetQServer(writer, name, servers.get(name));
                if (!first) {
                    writeNewLine(writer);
                } else {
                    first = false;
                }
            }
        }

        if (node.hasDefined(JMS_BRIDGE)) {
            final ModelNode jmsBridges = node.get(JMS_BRIDGE);
            boolean first = true;
            for (String name : jmsBridges.keys()) {
                writeJmsBridge(writer, name, jmsBridges.get(name));
                if (!first) {
                    writeNewLine(writer);
                } else {
                    first = false;
                }
            }
        }
        writer.writeEndElement();
    }

    private static void writeHornetQServer(final XMLExtendedStreamWriter writer, final String serverName, final ModelNode node) throws XMLStreamException {

        writer.writeStartElement(Element.HORNETQ_SERVER.getLocalName());

        if (!DEFAULT.equals(serverName)) {
            writer.writeAttribute(Attribute.NAME.getLocalName(), serverName);
        }

        for (AttributeDefinition simpleAttribute : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            simpleAttribute.marshallAsElement(node, writer);
        }

        final ModelNode paths = node.get(ModelDescriptionConstants.PATH);
        writeDirectory(writer, Element.PAGING_DIRECTORY, paths);
        writeDirectory(writer, Element.BINDINGS_DIRECTORY, paths);
        writeDirectory(writer, Element.JOURNAL_DIRECTORY, paths);
        writeDirectory(writer, Element.LARGE_MESSAGES_DIRECTORY, paths);

        // New line after the simpler elements
        writeNewLine(writer);

        writeConnectors(writer, node);
        writeAcceptors(writer, node);
        writeBroadcastGroups(writer, node.get(BROADCAST_GROUP));
        writeDiscoveryGroups(writer, node.get(DISCOVERY_GROUP));
        writeDiverts(writer, node.get(DIVERT));
        writeQueues(writer, node.get(CommonAttributes.QUEUE));
        writeBridges(writer, node.get(CommonAttributes.BRIDGE));
        writeClusterConnections(writer, node.get(CommonAttributes.CLUSTER_CONNECTION));
        writeGroupingHandler(writer, node.get(GROUPING_HANDLER));
        writeSecuritySettings(writer, node.get(CommonAttributes.SECURITY_SETTING));
        writeAddressSettings(writer, node.get(ADDRESS_SETTING));
        writeConnectorServices(writer, node.get(CommonAttributes.CONNECTOR_SERVICE));

        if (node.hasDefined(CONNECTION_FACTORY) || node.hasDefined(POOLED_CONNECTION_FACTORY)) {
            ModelNode cf = node.get(CONNECTION_FACTORY);
            ModelNode pcf = node.get(POOLED_CONNECTION_FACTORY);
            boolean hasCf = cf.isDefined() && cf.keys().size() > 0;
            boolean hasPcf = pcf.isDefined() && pcf.keys().size() > 0;
            if (hasCf || hasPcf) {
                writer.writeStartElement(JMS_CONNECTION_FACTORIES);
                writeConnectionFactories(writer, cf);
                writePooledConnectionFactories(writer, pcf);
                writer.writeEndElement();
                writeNewLine(writer);
            }
        }

        if (node.hasDefined(JMS_QUEUE) || node.hasDefined(JMS_TOPIC)) {
            ModelNode queue = node.get(JMS_QUEUE);
            ModelNode topic = node.get(JMS_TOPIC);
            boolean hasQueue = queue.isDefined() && queue.keys().size() > 0;
            boolean hasTopic = topic.isDefined() && topic.keys().size() > 0;
            if (hasQueue || hasTopic) {
                writer.writeStartElement(JMS_DESTINATIONS);
                writeJmsQueues(writer, node.get(JMS_QUEUE));
                writeTopics(writer, node.get(JMS_TOPIC));
                writer.writeEndElement();
            }
        }

        writer.writeEndElement();
    }

    private static void writeConnectors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(CONNECTOR) || node.hasDefined(REMOTE_CONNECTOR) || node.hasDefined(IN_VM_CONNECTOR)) {
            writer.writeStartElement(Element.CONNECTORS.getLocalName());
            if(node.hasDefined(HTTP_CONNECTOR)) {
                for(final Property property : node.get(HTTP_CONNECTOR).asPropertyList()) {
                    writer.writeStartElement(Element.HTTP_CONNECTOR.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    HTTPConnectorDefinition.SOCKET_BINDING.marshallAsAttribute(property.getValue(), writer);
                    writeTransportParam(writer, property.getValue().get(PARAM));
                    writer.writeEndElement();
                }
            }
            if(node.hasDefined(REMOTE_CONNECTOR)) {
                for(final Property property : node.get(REMOTE_CONNECTOR).asPropertyList()) {
                    writer.writeStartElement(Element.NETTY_CONNECTOR.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    RemoteTransportDefinition.SOCKET_BINDING.marshallAsAttribute(property.getValue(), writer);
                    writeTransportParam(writer, property.getValue().get(PARAM));
                    writer.writeEndElement();
                }
            }
            if(node.hasDefined(IN_VM_CONNECTOR)) {
                for(final Property property : node.get(IN_VM_CONNECTOR).asPropertyList()) {
                    writer.writeStartElement(Element.IN_VM_CONNECTOR.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    InVMTransportDefinition.SERVER_ID.marshallAsAttribute(property.getValue(), writer);
                    writeTransportParam(writer, property.getValue().get(PARAM));
                    writer.writeEndElement();
                }
            }
            if(node.hasDefined(CONNECTOR)) {
                for(final Property property : node.get(CONNECTOR).asPropertyList()) {
                    writer.writeStartElement(Element.CONNECTOR.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    GenericTransportDefinition.SOCKET_BINDING.marshallAsAttribute(property.getValue(), writer);

                    writeTransportParam(writer, property.getValue().get(PARAM));

                    CommonAttributes.FACTORY_CLASS.marshallAsElement(property.getValue(), writer);

                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeAcceptors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(ACCEPTOR) || node.hasDefined(REMOTE_ACCEPTOR) || node.hasDefined(IN_VM_ACCEPTOR)) {
            writer.writeStartElement(Element.ACCEPTORS.getLocalName());
            if(node.hasDefined(HTTP_ACCEPTOR)) {
                for(final Property property : node.get(HTTP_ACCEPTOR).asPropertyList()) {
                    writer.writeStartElement(Element.HTTP_ACCEPTOR.getLocalName());
                    HTTPAcceptorDefinition.HTTP_LISTENER.marshallAsAttribute(property.getValue(), writer);
                    writeAcceptorContent(writer, property);
                    writer.writeEndElement();
                }
            }
            if(node.hasDefined(REMOTE_ACCEPTOR)) {
                for(final Property property : node.get(REMOTE_ACCEPTOR).asPropertyList()) {
                    writer.writeStartElement(Element.NETTY_ACCEPTOR.getLocalName());
                    writeAcceptorContent(writer, property);
                    writer.writeEndElement();
                }
            }
            if(node.hasDefined(IN_VM_ACCEPTOR)) {
                for(final Property property : node.get(IN_VM_ACCEPTOR).asPropertyList()) {
                    writer.writeStartElement(Element.IN_VM_ACCEPTOR.getLocalName());
                    writeAcceptorContent(writer, property);
                    writer.writeEndElement();
                }
            }
            if(node.hasDefined(ACCEPTOR)) {
                for(final Property property : node.get(ACCEPTOR).asPropertyList()) {
                    writer.writeStartElement(Element.ACCEPTOR.getLocalName());
                    writeAcceptorContent(writer, property);
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeAcceptorContent(final XMLExtendedStreamWriter writer, final Property property) throws XMLStreamException {
        writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
        final ModelNode value = property.getValue();

        RemoteTransportDefinition.SOCKET_BINDING.marshallAsAttribute(value, writer);
        InVMTransportDefinition.SERVER_ID.marshallAsAttribute(value, writer);

        writeTransportParam(writer, value.get(PARAM));

        CommonAttributes.FACTORY_CLASS.marshallAsElement(value, writer);
    }

    private static void writeTransportParam(final XMLExtendedStreamWriter writer, final ModelNode param) throws XMLStreamException {
        if (param.isDefined()) {
            for(final Property parameter : param.asPropertyList()) {
                writer.writeStartElement(Element.PARAM.getLocalName());
                writer.writeAttribute(Attribute.KEY.getLocalName(), parameter.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), parameter.getValue().get(TransportParamDefinition.VALUE.getName()).asString());
                writer.writeEndElement();
            }
        }
    }

    private static void writeBroadcastGroups(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.BROADCAST_GROUPS.getLocalName());
            for(final Property property : properties) {
                writer.writeStartElement(Element.BROADCAST_GROUP.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : BroadcastGroupDefinition.ATTRIBUTES) {
                    attribute.marshallAsElement(property.getValue(), writer);
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeDiscoveryGroups(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.DISCOVERY_GROUPS.getLocalName());
            for(final Property property : properties) {
                writer.writeStartElement(Element.DISCOVERY_GROUP.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : DiscoveryGroupDefinition.ATTRIBUTES) {
                    attribute.marshallAsElement(property.getValue(), writer);
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeDiverts(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.DIVERTS.getLocalName());
            for(final Property property : properties) {
                writer.writeStartElement(Element.DIVERT.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : DivertDefinition.ATTRIBUTES) {
                    if (CommonAttributes.FILTER == attribute) {
                        writeFilter(writer, property.getValue());
                    } else {
                        attribute.marshallAsElement(property.getValue(), writer);
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        if (node.asInt() > 0) {
            writer.writeStartElement(Element.CORE_QUEUES.getLocalName());
            for (String queueName : node.keys()) {
                writer.writeStartElement(Element.QUEUE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), queueName);
                final ModelNode queue = node.get(queueName);
                QueueDefinition.ADDRESS.marshallAsElement(queue, writer);
                writeFilter(writer, queue);
                DURABLE.marshallAsElement(queue, writer);

                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeBridges(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.BRIDGES.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.BRIDGE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                for (AttributeDefinition attribute : BridgeDefinition.ATTRIBUTES) {
                    if (CommonAttributes.FILTER == attribute) {
                        writeFilter(writer, property.getValue());
                    } else {
                        attribute.marshallAsElement(property.getValue(), writer);
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeClusterConnections(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.CLUSTER_CONNECTIONS.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.CLUSTER_CONNECTION.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                final ModelNode cluster = property.getValue();
                for (AttributeDefinition attribute : ClusterConnectionDefinition.ATTRIBUTES) {
                    if (attribute == ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY) {
                        // we nest it in static-connectors
                        continue;
                    }
                    if (attribute == CONNECTOR_REFS) {
                        if (attribute.isMarshallable(cluster)) {
                            writer.writeStartElement(Element.STATIC_CONNECTORS.getLocalName());
                            ALLOW_DIRECT_CONNECTIONS_ONLY.marshallAsAttribute(cluster, writer);
                            CONNECTOR_REFS.marshallAsElement(cluster, writer);
                            writer.writeEndElement();
                        } else if (ALLOW_DIRECT_CONNECTIONS_ONLY.isMarshallable(cluster)) {
                            writer.writeEmptyElement(Element.STATIC_CONNECTORS.getLocalName());
                            ALLOW_DIRECT_CONNECTIONS_ONLY.marshallAsAttribute(cluster, writer);
                        }
                    } else {
                        attribute.marshallAsElement(property.getValue(), writer);
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeGroupingHandler(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        boolean wroteHandler = false;
        for (Property handler : node.asPropertyList()) {
            if (wroteHandler) {
                throw MessagingLogger.ROOT_LOGGER.multipleChildrenFound(GROUPING_HANDLER);
            } else {
                wroteHandler = true;
            }
            writer.writeStartElement(Element.GROUPING_HANDLER.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), handler.getName());
            final ModelNode resourceModel = handler.getValue();
            for (AttributeDefinition attr : GroupingHandlerDefinition.ATTRIBUTES) {
                attr.marshallAsElement(resourceModel, writer);
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    // TODO use a custom attribute marshaller
    private static void writeFilter(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(CommonAttributes.FILTER.getName())) {
            writer.writeEmptyElement(CommonAttributes.FILTER.getXmlName());
            writer.writeAttribute(CommonAttributes.STRING, node.get(CommonAttributes.FILTER.getName()).asString());
        }
    }

    private static void writeDirectory(final XMLExtendedStreamWriter writer, final Element element, final ModelNode node) throws XMLStreamException {
        final String localName = element.getLocalName();
        if(node.hasDefined(localName)) {
            final ModelNode localNode = node.get(localName);
            if (RELATIVE_TO.isMarshallable(localNode) ||  PATHS.get(localName).isMarshallable(localNode)) {
                writer.writeEmptyElement(localName);
                PATHS.get(localName).marshallAsAttribute(node.get(localName), writer);
                RELATIVE_TO.marshallAsAttribute(node.get(localName), writer);
            }
        }
    }

    private static void writeSecuritySettings(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.SECURITY_SETTINGS.getLocalName());

            for (Property matchRoles : properties) {
                writer.writeStartElement(Element.SECURITY_SETTING.getLocalName());
                writer.writeAttribute(Attribute.MATCH.getLocalName(), matchRoles.getName());

                // TODO use a custom attribute marshaller
                if (matchRoles.getValue().hasDefined(ROLE)) {

                    ArrayList<String> send = new ArrayList<String>();
                    ArrayList<String> consume = new ArrayList<String>();
                    ArrayList<String> createDurableQueue = new ArrayList<String>();
                    ArrayList<String> deleteDurableQueue = new ArrayList<String>();
                    ArrayList<String> createNonDurableQueue = new ArrayList<String>();
                    ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
                    ArrayList<String> manageRoles = new ArrayList<String>();

                    for (Property rolePerms : matchRoles.getValue().get(ROLE).asPropertyList()) {
                        final String role = rolePerms.getName();
                        final ModelNode perms = rolePerms.getValue();
                        if (perms.get(SecurityRoleDefinition.SEND.getName()).asBoolean(false)) {
                            send.add(role);
                        }
                        if (perms.get(SecurityRoleDefinition.CONSUME.getName()).asBoolean(false)) {
                            consume.add(role);
                        }
                        if (perms.get(SecurityRoleDefinition.CREATE_DURABLE_QUEUE.getName()).asBoolean(false)) {
                            createDurableQueue.add(role);
                        }
                        if (perms.get(SecurityRoleDefinition.DELETE_DURABLE_QUEUE.getName()).asBoolean(false)) {
                            deleteDurableQueue.add(role);
                        }
                        if (perms.get(SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE.getName()).asBoolean(false)) {
                            createNonDurableQueue.add(role);
                        }
                        if (perms.get(SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE.getName()).asBoolean(false)) {
                            deleteNonDurableQueue.add(role);
                        }
                        if (perms.get(SecurityRoleDefinition.MANAGE.getName()).asBoolean(false)) {
                            manageRoles.add(role);
                        }
                    }

                    writePermission(writer, SecurityRoleDefinition.SEND.getXmlName(), send);
                    writePermission(writer, SecurityRoleDefinition.CONSUME.getXmlName(), consume);
                    writePermission(writer, SecurityRoleDefinition.CREATE_DURABLE_QUEUE.getXmlName(), createDurableQueue);
                    writePermission(writer, SecurityRoleDefinition.DELETE_DURABLE_QUEUE.getXmlName(), deleteDurableQueue);
                    writePermission(writer, SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE.getXmlName(), createNonDurableQueue);
                    writePermission(writer, SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE.getXmlName(), deleteNonDurableQueue);
                    writePermission(writer, SecurityRoleDefinition.MANAGE.getXmlName(), manageRoles);
                }

                writer.writeEndElement();
            }

            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writePermission(final XMLExtendedStreamWriter writer, final String type, final List<String> roles) throws XMLStreamException {
        if (roles.size() == 0) {
            return;
        }
        writer.writeStartElement(Element.PERMISSION_ELEMENT_NAME.getLocalName());
        StringBuilder sb = new StringBuilder();
        for (String role : roles) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(role);
        }
        writer.writeAttribute(Attribute.TYPE_ATTR_NAME.getLocalName(), type);
        writer.writeAttribute(Attribute.ROLES_ATTR_NAME.getLocalName(), sb.toString());
        writer.writeEndElement();
    }

    private static void writeAddressSettings(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.ADDRESS_SETTINGS.getLocalName());
            for (Property matchSetting : properties) {
                writer.writeStartElement(Element.ADDRESS_SETTING.getLocalName());
                writer.writeAttribute(Attribute.MATCH.getLocalName(), matchSetting.getName());
                final ModelNode setting = matchSetting.getValue();

                for (AttributeDefinition attribute : AddressSettingDefinition.ATTRIBUTES) {
                    attribute.marshallAsElement(setting, writer);
                }

                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeConnectorServices(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        List<Property> properties = node.asPropertyList();
        if (!properties.isEmpty()) {
            writer.writeStartElement(Element.CONNECTOR_SERVICES.getLocalName());
            for(final Property property : node.asPropertyList()) {
                writer.writeStartElement(Element.CONNECTOR_SERVICE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                final ModelNode service = property.getValue();
                for (AttributeDefinition attribute : ConnectorServiceDefinition.ATTRIBUTES) {
                    attribute.marshallAsElement(property.getValue(), writer);
                }
                // TODO use a custom attribute marshaller
                if (service.hasDefined(CommonAttributes.PARAM)) {
                    for (Property param : service.get(CommonAttributes.PARAM).asPropertyList()) {
                        writer.writeEmptyElement(Element.PARAM.getLocalName());
                        writer.writeAttribute(Attribute.KEY.getLocalName(), param.getName());
                        writer.writeAttribute(Attribute.VALUE.getLocalName(), param.getValue().get(ConnectorServiceParamDefinition.VALUE.getName()).asString());
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }
    }

    private static void writeConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        if (node.asInt() > 0) {
            for (String name : node.keys()) {
                final ModelNode factory = node.get(name);
                if (factory.isDefined()) {
                    writer.writeStartElement(Element.CONNECTION_FACTORY.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);

                    for (AttributeDefinition attribute : ConnectionFactoryDefinition.ATTRIBUTES) {
                        attribute.marshallAsElement(factory, writer);
                    }

                    writer.writeEndElement();
                }
            }
        }
    }

    private static void writePooledConnectionFactories(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        if (node.asInt() > 0) {
            for (String name : node.keys()) {
                final ModelNode factory = node.get(name);
                if (factory.isDefined()) {
                    writer.writeStartElement(Element.POOLED_CONNECTION_FACTORY.getLocalName());

                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);

                    // write inbound config attributes first...
                    if(hasDefinedInboundConfigAttributes(factory)) {
                        writer.writeStartElement(Element.INBOUND_CONFIG.getLocalName());
                        for (ConnectionFactoryAttribute attribute : PooledConnectionFactoryDefinition.ATTRIBUTES) {
                            if (attribute.isInboundConfig()) {
                                attribute.getDefinition().marshallAsElement(factory, writer);
                            }
                        }
                        writer.writeEndElement();
                    }

                    // ... then the attributes that are not part of the inbound config
                    for (ConnectionFactoryAttribute attribute : PooledConnectionFactoryDefinition.ATTRIBUTES) {
                        if (!attribute.isInboundConfig()) {
                            attribute.getDefinition().marshallAsElement(factory, writer);
                        }
                    }

                    writer.writeEndElement();
                }
            }
        }
    }

    private static boolean hasDefinedInboundConfigAttributes(ModelNode pcf) {
        for (ConnectionFactoryAttribute attribute : PooledConnectionFactoryDefinition.ATTRIBUTES) {
            if (attribute.isInboundConfig() && pcf.hasDefined(attribute.getDefinition().getName())) {
                return true;
            }
        }
        return false;
    }

    private static void writeJmsQueues(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        if (node.asInt() > 0) {
            for (String name : node.keys()) {
                final ModelNode queue = node.get(name);
                if (queue.isDefined()) {
                    writer.writeStartElement(Element.JMS_QUEUE.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);

                    for (AttributeDefinition attribute : JMSQueueDefinition.ATTRIBUTES) {
                        attribute.marshallAsElement(queue, writer);
                    }

                    writer.writeEndElement();
                }
            }
        }
    }

    private static void writeTopics(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (!node.isDefined()) {
            return;
        }
        if (node.asInt() > 0) {
            for (String name : node.keys()) {
                final ModelNode topic = node.get(name);
                if (topic.isDefined()) {
                    writer.writeStartElement(Element.JMS_TOPIC.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);

                    for (AttributeDefinition attribute : JMSTopicDefinition.ATTRIBUTES) {
                        attribute.marshallAsElement(topic, writer);
                    }

                    writer.writeEndElement();
                }
            }
        }
    }


    private void writeJmsBridge(XMLExtendedStreamWriter writer, String bridgeName, ModelNode value) throws XMLStreamException {
        writer.writeStartElement(Element.JMS_BRIDGE.getLocalName());

        if (!DEFAULT.equals(bridgeName)) {
            writer.writeAttribute(Attribute.NAME.getLocalName(), bridgeName);
        }

        JMSBridgeDefinition.MODULE.marshallAsAttribute(value, writer);

        writer.writeStartElement(SOURCE.getLocalName());
        for (AttributeDefinition attr : JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES) {
            attr.marshallAsElement(value, writer);
        }
        writer.writeEndElement();

        writer.writeStartElement(TARGET.getLocalName());
        for (AttributeDefinition attr : JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES) {
            attr.marshallAsElement(value, writer);
        }
        writer.writeEndElement();

        for (AttributeDefinition attr : JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES) {
            if (attr == JMSBridgeDefinition.MODULE) {
                // handled as a XML attribute
                continue;
            }
            attr.marshallAsElement(value, writer);
        }

        writer.writeEndElement();
    }

    private static void writeNewLine(XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(NEW_LINE, 0, 1);
    }

}
