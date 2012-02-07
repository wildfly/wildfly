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

import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SECURITY_KEY;
import static org.jboss.as.modcluster.CommonAttributes.ADVERTISE_SOCKET;
import static org.jboss.as.modcluster.CommonAttributes.AUTO_ENABLE_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.BALANCER;
import static org.jboss.as.modcluster.CommonAttributes.CAPACITY;
import static org.jboss.as.modcluster.CommonAttributes.CLASS;
import static org.jboss.as.modcluster.CommonAttributes.CONFIGURATION;
import static org.jboss.as.modcluster.CommonAttributes.CUSTOM_LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.DECAY;
import static org.jboss.as.modcluster.CommonAttributes.DOMAIN;
import static org.jboss.as.modcluster.CommonAttributes.DYNAMIC_LOAD_PROVIDER;
import static org.jboss.as.modcluster.CommonAttributes.EXCLUDED_CONTEXTS;
import static org.jboss.as.modcluster.CommonAttributes.FACTOR;
import static org.jboss.as.modcluster.CommonAttributes.FLUSH_PACKETS;
import static org.jboss.as.modcluster.CommonAttributes.FLUSH_WAIT;
import static org.jboss.as.modcluster.CommonAttributes.HISTORY;
import static org.jboss.as.modcluster.CommonAttributes.LOAD_METRIC;
import static org.jboss.as.modcluster.CommonAttributes.MAX_ATTEMPTS;
import static org.jboss.as.modcluster.CommonAttributes.MOD_CLUSTER_CONFIG;
import static org.jboss.as.modcluster.CommonAttributes.NAME;
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
import static org.jboss.as.modcluster.CommonAttributes.TYPE;
import static org.jboss.as.modcluster.CommonAttributes.VALUE;
import static org.jboss.as.modcluster.CommonAttributes.WEIGHT;
import static org.jboss.as.modcluster.CommonAttributes.WORKER_TIMEOUT;

import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class ModClusterSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {
    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);

        ModelNode node = context.getModelNode();
        if (node.get(MOD_CLUSTER_CONFIG).isDefined() && node.get(MOD_CLUSTER_CONFIG).has(CONFIGURATION))
            writeModClusterConfig(writer, node.get(MOD_CLUSTER_CONFIG).get(CONFIGURATION));
        else
            writeModClusterConfig(writer, node);
        writer.writeEndElement();
    }

    static void writeModClusterConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.MOD_CLUSTER_CONFIG.getLocalName());
        // write Attributes
        writePropConf(writer, config);

        // write the elements.
        if (config.hasDefined(SIMPLE_LOAD_PROVIDER)) {
            writeSimpleLoadProvider(writer, config.get(SIMPLE_LOAD_PROVIDER));
        }
        if (config.hasDefined(DYNAMIC_LOAD_PROVIDER)) {
            writeDynamicLoadProvider(writer, config.get(DYNAMIC_LOAD_PROVIDER));
        }
        if (config.get(SSL).isDefined() && config.get(SSL).has(CONFIGURATION)) {
            writeSSL(writer, config.get(SSL).get(CONFIGURATION));
        }
        writer.writeEndElement();
    }

    /* prop-confType */
    static void writePropConf(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writeAttribute(writer, ADVERTISE_SOCKET, config);
        writeAttribute(writer, PROXY_LIST, config);
        writeAttribute(writer, PROXY_URL, config);
        writeAttribute(writer, ADVERTISE, config);
        writeAttribute(writer, ADVERTISE_SECURITY_KEY, config);
        writeAttribute(writer, EXCLUDED_CONTEXTS, config);
        writeAttribute(writer, AUTO_ENABLE_CONTEXTS, config);
        writeAttribute(writer, STOP_CONTEXT_TIMEOUT, config);
        writeAttribute(writer, SOCKET_TIMEOUT, config);

        writeAttribute(writer, STICKY_SESSION, config);
        writeAttribute(writer, STICKY_SESSION_REMOVE, config);
        writeAttribute(writer, STICKY_SESSION_FORCE, config);
        writeAttribute(writer, WORKER_TIMEOUT, config);
        writeAttribute(writer, MAX_ATTEMPTS, config);
        writeAttribute(writer, FLUSH_PACKETS, config);
        writeAttribute(writer, FLUSH_WAIT, config);
        writeAttribute(writer, PING, config);
        writeAttribute(writer, SMAX, config);
        writeAttribute(writer, TTL, config);
        writeAttribute(writer, NODE_TIMEOUT, config);
        writeAttribute(writer, BALANCER, config);
        writeAttribute(writer, DOMAIN, config);
    }

    static void writeSSL(XMLExtendedStreamWriter writer, ModelNode sslConfig) throws XMLStreamException {
        writer.writeStartElement(Element.SSL.getLocalName());
        writeAttribute(writer, Attribute.KEY_ALIAS.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.PASSWORD.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CERTIFICATE_KEY_FILE.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CIPHER_SUITE.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.PROTOCOL.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CA_CERTIFICATE_FILE.getLocalName(), sslConfig);
        writeAttribute(writer, Attribute.CA_REVOCATION_URL.getLocalName(), sslConfig);
        writer.writeEndElement();
    }

    /* Simple Load provider */
    static void writeSimpleLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SIMPLE_LOAD_PROVIDER.getLocalName());
        writeAttribute(writer, FACTOR, config);
        writer.writeEndElement();
    }

    /* Dynamic load provider */
    static void writeDynamicLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.DYNAMIC_LOAD_PROVIDER.getLocalName());
        writeAttribute(writer, HISTORY, config);
        writeAttribute(writer, DECAY, config);

        // write the elements.
        if (config.hasDefined(LOAD_METRIC)) {
            writeLoadMetric(writer, config.get(LOAD_METRIC));
        }
        if (config.hasDefined(CUSTOM_LOAD_METRIC)) {
            writeCustomLoadMetric(writer, config.get(CUSTOM_LOAD_METRIC));
        }
        writer.writeEndElement();
    }

    /* Load Metric parsing logic */
    static void writeLoadMetric(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        final List<ModelNode> array = config.asList();
        Iterator<ModelNode> it = array.iterator();
        while (it.hasNext()) {
            final ModelNode node = it.next();
            writer.writeStartElement(Element.LOAD_METRIC.getLocalName());
            writeAttribute(writer, TYPE, node);
            writeAttribute(writer, WEIGHT, node);
            writeAttribute(writer, CAPACITY, node);
            if (node.get(CommonAttributes.PROPERTY).isDefined()) {
                for (Property property : node.get(CommonAttributes.PROPERTY).asPropertyList()) {
                    writeProperty(writer, property);
                }
            }
            writer.writeEndElement();
        }
    }

    /* Custom Load Metric parsing logic */
    static void writeCustomLoadMetric(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        final List<ModelNode> array = config.asList();
        Iterator<ModelNode> it = array.iterator();
        while (it.hasNext()) {
            final ModelNode node = it.next();
            writer.writeStartElement(Element.CUSTOM_LOAD_METRIC.getLocalName());
            writeAttribute(writer, CAPACITY, node);
            writeAttribute(writer, WEIGHT, node);
            writeAttribute(writer, CLASS, node);
            if (node.get(CommonAttributes.PROPERTY).isDefined()) {
                for (Property property : node.get(CommonAttributes.PROPERTY).asPropertyList()) {
                    writeProperty(writer, property);
                }
            }
            writer.writeEndElement();
        }
    }

    static void writeAttribute(final XMLExtendedStreamWriter writer, final String name, ModelNode node)
            throws XMLStreamException {
        if (node.hasDefined(name)) {
            writer.writeAttribute(name, node.get(name).asString());
        }
    }

    /* Property logic */
    static void writeProperty(final XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(Element.PROPERTY.getLocalName());
        writer.writeAttribute(NAME, property.getName());
        writer.writeAttribute(VALUE, property.getValue().asString());
        writer.writeEndElement();
    }
}
