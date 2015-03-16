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

package org.jboss.as.ee.structure;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for urn:jboss:ejb-client:1.1:jboss-ejb-client
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
class EJBClientDescriptor11Parser extends EJBClientDescriptor10Parser {

    public static final String NAMESPACE_1_1 = "urn:jboss:ejb-client:1.1";

    protected EJBClientDescriptor11Parser(final PropertyReplacer propertyReplacer) {
        super(propertyReplacer);
    }

    protected void parseClientContext(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLElement> visited = EnumSet.noneOf(EJBClientDescriptorXMLElement.class);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case EJB_RECEIVERS:
                            this.parseEJBReceivers(reader, ejbClientDescriptorMetaData);
                            break;
                        case CLUSTERS:
                            this.parseClusters(reader, ejbClientDescriptorMetaData);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected void parseClusters(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case CLUSTER:
                            this.parseCluster(reader, ejbClientDescriptorMetaData);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected void parseCluster(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet.of(EJBClientDescriptorXMLAttribute.NAME);
        final int count = reader.getAttributeCount();
        String clusterName = null;
        String clusterNodeSelector = null;
        long connectTimeout = 5000;
        long maxAllowedConnectedNodes = 10;
        String userName = null;
        String securityRealm = null;
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            final String value = readResolveValue(reader, i);
            switch (attribute) {
                case NAME:
                    clusterName = value;
                    break;
                case CONNECT_TIMEOUT:
                    connectTimeout = Long.parseLong(value);
                    break;
                case CLUSTER_NODE_SELECTOR:
                    clusterNodeSelector = value;
                    break;
                case MAX_ALLOWED_CONNECTED_NODES:
                    maxAllowedConnectedNodes = Long.parseLong(value);
                    break;
                case USERNAME:
                    userName = value;
                    break;
                case SECURITY_REALM:
                    securityRealm = value;
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            missingAttributes(reader.getLocation(), required);
        }
        // add a new cluster config to the client configuration metadata
        final EJBClientDescriptorMetaData.ClusterConfig clusterConfig = ejbClientDescriptorMetaData.newClusterConfig(clusterName);
        clusterConfig.setConnectTimeout(connectTimeout);
        clusterConfig.setNodeSelector(clusterNodeSelector);
        clusterConfig.setMaxAllowedConnectedNodes(maxAllowedConnectedNodes);
        clusterConfig.setSecurityRealm(securityRealm);
        clusterConfig.setUserName(userName);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTION_CREATION_OPTIONS:
                            final Properties connectionCreationOptions = this.parseConnectionCreationOptions(reader);
                            clusterConfig.setConnectionOptions(connectionCreationOptions);
                            break;
                        case CHANNEL_CREATION_OPTIONS:
                            final Properties channelCreationOptions = this.parseChannelCreationOptions(reader);
                            clusterConfig.setChannelCreationOptions(channelCreationOptions);
                            break;
                        case NODE:
                            this.parseClusterNode(reader, clusterConfig);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected Properties parseConnectionCreationOptions(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final Properties connectionCreationOptions = new Properties();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return connectionCreationOptions;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case PROPERTY:
                            connectionCreationOptions.putAll(this.parseProperty(reader));
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
        // unreachable
        return connectionCreationOptions;
    }

    protected Properties parseChannelCreationOptions(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final Properties channelCreationOptions = new Properties();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return channelCreationOptions;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case PROPERTY:
                            channelCreationOptions.putAll(this.parseProperty(reader));
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
        // unreachable
        return channelCreationOptions;
    }

    protected void parseClusterNode(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData.ClusterConfig clusterConfig) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet.of(EJBClientDescriptorXMLAttribute.NAME);
        final int count = reader.getAttributeCount();
        String nodeName = null;
        long connectTimeout = 5000;
        String userName = null;
        String securityRealm = null;
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            final String value = readResolveValue(reader, i);
            switch (attribute) {
                case NAME:
                    nodeName = value;
                    break;
                case CONNECT_TIMEOUT:
                    connectTimeout = Long.parseLong(value);
                    break;
                case USERNAME:
                    userName = value;
                    break;
                case SECURITY_REALM:
                    securityRealm = value;
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            missingAttributes(reader.getLocation(), required);
        }
        // add a new node config to the cluster config
        final EJBClientDescriptorMetaData.ClusterNodeConfig clusterNodeConfig = clusterConfig.newClusterNode(nodeName);
        clusterNodeConfig.setConnectTimeout(connectTimeout);
        clusterNodeConfig.setSecurityRealm(securityRealm);
        clusterNodeConfig.setUserName(userName);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case CONNECTION_CREATION_OPTIONS:
                            final Properties connectionCreationOptions = this.parseConnectionCreationOptions(reader);
                            clusterNodeConfig.setConnectionOptions(connectionCreationOptions);
                            break;
                        case CHANNEL_CREATION_OPTIONS:
                            final Properties channelCreationOptions = this.parseChannelCreationOptions(reader);
                            clusterNodeConfig.setChannelCreationOptions(channelCreationOptions);
                            break;
                        default:
                            unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    unexpectedContent(reader);
                }
            }
        }
        unexpectedEndOfDocument(reader.getLocation());
    }

    protected Properties parseProperty(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet.of(EJBClientDescriptorXMLAttribute.NAME,
                EJBClientDescriptorXMLAttribute.VALUE);
        final int count = reader.getAttributeCount();
        String name = null;
        String value = null;
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            final String val = readResolveValue(reader, i);
            switch (attribute) {
                case NAME:
                    name = val;
                    break;
                case VALUE:
                    value = val;
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            missingAttributes(reader.getLocation(), required);
        }
        // no child elements allowed
        requireNoContent(reader);

        final Properties property = new Properties();
        property.put(name, value);
        return property;
    }

    protected static void requireNoContent(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            unexpectedElement(reader);
        }
    }
}
