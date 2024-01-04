/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * Parser for urn:jboss:ejb-client:1.4:jboss-ejb-client
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class EJBClientDescriptor15Parser extends EJBClientDescriptor14Parser {

    public static final String NAMESPACE_1_5 = "urn:jboss:ejb-client:1.5";

    protected EJBClientDescriptor15Parser(final PropertyReplacer propertyReplacer) {
        super(propertyReplacer);
    }

    protected void parseCluster(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet.of(EJBClientDescriptorXMLAttribute.NAME);
        final int count = reader.getAttributeCount();
        String clusterName = null;
        String clusterNodeSelector = null;
        long connectTimeout = 5000;
        long maxAllowedConnectedNodes = 10;
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

    protected void parseClusterNode(final XMLExtendedStreamReader reader, final EJBClientDescriptorMetaData.ClusterConfig clusterConfig) throws XMLStreamException {
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet.of(EJBClientDescriptorXMLAttribute.NAME);
        final int count = reader.getAttributeCount();
        String nodeName = null;
        long connectTimeout = 5000;
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

}
