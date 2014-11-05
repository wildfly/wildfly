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
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for urn:jboss:ejb-client:1.2:jboss-ejb-client
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class EJBClientDescriptor12Parser extends EJBClientDescriptor11Parser {

    public static final String NAMESPACE_1_2 = "urn:jboss:ejb-client:1.2";

    public static final EJBClientDescriptor12Parser INSTANCE = new EJBClientDescriptor12Parser();

    protected EJBClientDescriptor12Parser() {
    }

    protected void parseClientContext(final XMLExtendedStreamReader reader,
            final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader
                    .getAttributeLocalName(i));
            final String val = reader.getAttributeValue(i);
            switch (attribute) {
                case INVOCATION_TIMEOUT:
                    final Long invocationTimeout = Long.parseLong(val.trim());
                    ejbClientDescriptorMetaData.setInvocationTimeout(invocationTimeout);
                    break;
                case DEPLOYMENT_NODE_SELECTOR:
                    ejbClientDescriptorMetaData.setDeploymentNodeSelector(val.trim());
                    break;
                default:
                    unexpectedContent(reader);
            }
        }

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

    protected void parseEJBReceivers(final XMLExtendedStreamReader reader,
            final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {

        // initialize the local-receiver-pass-by-value to the default true
        Boolean localReceiverPassByValue = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader
                    .getAttributeLocalName(i));
            final String val = reader.getAttributeValue(i);
            switch (attribute) {
                case EXCLUDE_LOCAL_RECEIVER:
                    final boolean excludeLocalReceiver = Boolean.parseBoolean(val.trim());
                    ejbClientDescriptorMetaData.setExcludeLocalReceiver(excludeLocalReceiver);
                    break;
                case LOCAL_RECEIVER_PASS_BY_VALUE:
                    localReceiverPassByValue = Boolean.parseBoolean(val.trim());
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        // set the local receiver pass by value into the metadata
        ejbClientDescriptorMetaData.setLocalReceiverPassByValue(localReceiverPassByValue);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case REMOTING_EJB_RECEIVER:
                            this.parseRemotingReceiver(reader, ejbClientDescriptorMetaData);
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

    protected void parseRemotingReceiver(final XMLExtendedStreamReader reader,
            final EJBClientDescriptorMetaData ejbClientDescriptorMetaData) throws XMLStreamException {
        String outboundConnectionRef = null;
        final Set<EJBClientDescriptorXMLAttribute> required = EnumSet
                .of(EJBClientDescriptorXMLAttribute.OUTBOUND_CONNECTION_REF);
        final int count = reader.getAttributeCount();
        EJBClientDescriptorMetaData.RemotingReceiverConfiguration remotingReceiverConfiguration = null;
        long connectTimeout = 5000;
        for (int i = 0; i < count; i++) {
            final EJBClientDescriptorXMLAttribute attribute = EJBClientDescriptorXMLAttribute.forName(reader
                    .getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case OUTBOUND_CONNECTION_REF:
                    outboundConnectionRef = reader.getAttributeValue(i).trim();
                    remotingReceiverConfiguration = ejbClientDescriptorMetaData
                            .addRemotingReceiverConnectionRef(outboundConnectionRef);
                    break;
                case CONNECT_TIMEOUT:
                    connectTimeout = reader.getLongAttributeValue(i);
                    break;
                default:
                    unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            missingAttributes(reader.getLocation(), required);
        }
        // set the timeout
        remotingReceiverConfiguration.setConnectionTimeout(connectTimeout);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    final EJBClientDescriptorXMLElement element = EJBClientDescriptorXMLElement.forName(reader.getLocalName());
                    switch (element) {
                        case CHANNEL_CREATION_OPTIONS:
                            final Properties channelCreationOptions = this.parseChannelCreationOptions(reader);
                            remotingReceiverConfiguration.setChannelCreationOptions(channelCreationOptions);
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
