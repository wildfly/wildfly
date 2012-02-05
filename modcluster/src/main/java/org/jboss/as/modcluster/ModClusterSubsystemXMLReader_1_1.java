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
package org.jboss.as.modcluster;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SECURITY_KEY;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SOCKET;
import static org.jboss.as.modcluster.CommonAttributes.AUTO_ENABLE_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.BALANCER;
import static org.jboss.as.modcluster.CommonAttributes.DOMAIN;
import static org.jboss.as.modcluster.CommonAttributes.DYNAMIC_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.EXCLUDED_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.FLUSH_PACKETS;
import static org.jboss.as.modcluster.CommonAttributes.FLUSH_WAIT;
import static org.jboss.as.modcluster.CommonAttributes.MAX_ATTEMPTS;
import static org.jboss.as.modcluster.CommonAttributes.MOD_CLUSTER_CONFIG;
import static org.jboss.as.modcluster.CommonAttributes.NODE_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.PING;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_LIST;
import static org.jboss.as.modcluster.CommonAttributes.PROXY_URL;
import static org.jboss.as.modcluster.CommonAttributes.SIMPLE_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.SMAX;
import static org.jboss.as.modcluster.CommonAttributes.SOCKET_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.SSL;
import static org.jboss.as.modcluster.CommonAttributes.STICKY_SESSION;
import static org.jboss.as.modcluster.CommonAttributes.STICKY_SESSION_FORCE;
import static org.jboss.as.modcluster.CommonAttributes.STICKY_SESSION_REMOVE;
import static org.jboss.as.modcluster.CommonAttributes.STOP_CONTEXT_TIMEOUT;
import static org.jboss.as.modcluster.CommonAttributes.TTL;
import static org.jboss.as.modcluster.CommonAttributes.WORKER_TIMEOUT;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Radoslav Husar
 * @since AS 7.1.0.Final
 */
public class ModClusterSubsystemXMLReader_1_1 extends ModClusterSubsystemXMLReader_1_0
        implements XMLElementReader<List<ModelNode>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        final ModelNode config = subsystem.get(MOD_CLUSTER_CONFIG);

        // Reads it
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            // These methods each parse their own section and update the existing provided model.
            switch (element) {
                case STICKY_SESSION:
                    parseStickySession(reader, config);
                    break;
                case ADVERTISE:
                    parseAdvertise(reader, config);
                    break;
                case PROXIES:
                    parseProxies(reader, config);
                    break;
                case CONTEXTS:
                    parseContexts(reader, config);
                    break;

                // Same as v1.0
                case SIMPLE_LOAD_PROVIDER:
                    final ModelNode load = parseSimpleLoadProvider(reader);
                    config.get(SIMPLE_LOAD_PROVIDER).set(load);
                    break;
                case DYNAMIC_LOAD_PROVIDER:
                    final ModelNode dynload = parseDynamicLoadProvider(reader);
                    config.get(DYNAMIC_LOAD_PROVIDER).set(dynload);
                    break;
                case SSL:
                    final ModelNode ssl = parseSSL(reader);
                    config.get(SSL).set(ssl);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }

            // TODO Remove only for debug
            // System.out.println(config.toJSONString(false));
        }
    }

    static void parseStickySession(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLE:
                    conf.get(STICKY_SESSION).set(value);
                    break;
                case FORCE:
                    conf.get(STICKY_SESSION_FORCE).set(value);
                    break;
                case REMOVE:
                    conf.get(STICKY_SESSION_REMOVE).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    static void parseAdvertise(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLE:
                    conf.get(ADVERTISE).set(value);
                    break;
                case SOCKET_BINDING:
                    conf.get(ADVERTISE_SOCKET).set(value);
                    break;
                case SECURITY_KEY:
                    conf.get(ADVERTISE_SECURITY_KEY).set(ParseUtils.parsePossibleExpression(value));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    static void parseProxies(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUT_SOCKET_BINDINGS:
                    // TODO
                    // Fow now this only uses the value naively as 1_0 parser does.
                    conf.get(PROXY_LIST).set(ParseUtils.parsePossibleExpression(value));
                    break;
                case URL:
                    conf.get(PROXY_URL).set(ParseUtils.parsePossibleExpression(value));
                    break;
                case LOAD_BALANCING_GROUP:
                    conf.get(DOMAIN).set(ParseUtils.parsePossibleExpression(value));
                    break;
                // Few more things that were missing from 1.0 XSD.
                case NODE_TIMEOUT:
                    conf.get(NODE_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case SOCKET_TIMEOUT:
                    conf.get(SOCKET_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case PING:
                    conf.get(PING).set(Integer.parseInt(value));
                    break;
                case WORKER_TIMEOUT:
                    conf.get(WORKER_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case MAX_ATTEMPTS:
                    conf.get(MAX_ATTEMPTS).set(Integer.parseInt(value));
                    break;
                case FLUSH_PACKETS:
                    conf.get(FLUSH_PACKETS).set(Boolean.parseBoolean(value));
                    break;
                case FLUSH_WAIT:
                    conf.get(FLUSH_WAIT).set(Integer.parseInt(value));
                    break;
                case SMAX:
                    conf.get(SMAX).set(Integer.parseInt(value));
                    break;
                case TTL:
                    conf.get(TTL).set(Integer.parseInt(value));
                    break;
                case BALANCER:
                    conf.get(BALANCER).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    static void parseContexts(XMLExtendedStreamReader reader, ModelNode conf) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case AUTO_ENABLE:
                    conf.get(AUTO_ENABLE_CONTEXTS).set(Boolean.parseBoolean(value));
                    break;
                case STOP_TIMEOUT:
                    conf.get(STOP_CONTEXT_TIMEOUT).set(Integer.parseInt(value));
                    break;
                case EXCLUDED_CONTEXTS:
                    conf.get(EXCLUDED_CONTEXTS).set(ParseUtils.parsePossibleExpression(value));
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }
}