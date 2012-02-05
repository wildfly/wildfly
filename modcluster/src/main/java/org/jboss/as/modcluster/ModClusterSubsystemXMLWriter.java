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
import static org.jboss.as.modcluster.CommonAttributes.ENABLE;
import static org.jboss.as.modcluster.CommonAttributes.FORCE;
import static org.jboss.as.modcluster.CommonAttributes.REMOVE;
// TODO: no * import please
import static org.jboss.as.modcluster.CommonAttributes.*;

import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
public class ModClusterSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);

        ModelNode node = context.getModelNode();
        if (node.get(MOD_CLUSTER_CONFIG).isDefined() && node.get(MOD_CLUSTER_CONFIG).has(CONFIGURATION)) {
            writeModClusterConfig(writer, node.get(MOD_CLUSTER_CONFIG).get(CONFIGURATION));
        } else {
            // TODO: Is this necessary?
            writeModClusterConfig(writer, node);
        }

        writer.writeEndElement();
    }

    static void writeModClusterConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {

        // Sticky sessions
        if (config.hasDefined(STICKY_SESSION) || config.hasDefined(STICKY_SESSION_FORCE)
                || config.hasDefined(STICKY_SESSION_REMOVE)) {
            // If one of the sticky session options is defined.
            writeStickySession(writer, config);
        }

        // Advertise
        if (config.hasDefined(ADVERTISE) || config.hasDefined(ADVERTISE_SECURITY_KEY) || config.hasDefined(ADVERTISE_SOCKET)) {
            writeAdvertise(writer, config);
        }

        // Proxies
        if (config.hasDefined(PROXY_URL) || config.hasDefined(PROXY_LIST) || config.hasDefined(DOMAIN)
                || config.hasDefined(PING) || config.hasDefined(NODE_TIMEOUT) || config.hasDefined(SOCKET_TIMEOUT)
                || config.hasDefined(WORKER_TIMEOUT) || config.hasDefined(MAX_ATTEMPTS) || config.hasDefined(FLUSH_PACKETS)
                || config.hasDefined(FLUSH_WAIT) || config.hasDefined(SMAX) || config.hasDefined(TTL)
                || config.hasDefined(BALANCER)) {
            writeProxies(writer, config);
        }

        // Contexts
        if (config.hasDefined(AUTO_ENABLE_CONTEXTS) || config.hasDefined(STOP_CONTEXT_TIMEOUT)
                || config.hasDefined(EXCLUDED_CONTEXTS)) {
            writeContexts(writer, config);
        }

        // Load providers
        if (config.hasDefined(SIMPLE_LOAD_PROVIDER)) {
            writeSimpleLoadProvider(writer, config.get(SIMPLE_LOAD_PROVIDER));
        }
        if (config.hasDefined(DYNAMIC_LOAD_PROVIDER)) {
            writeDynamicLoadProvider(writer, config.get(DYNAMIC_LOAD_PROVIDER));
        }

        // SSL config
        if (config.get(SSL).isDefined() && config.get(SSL).has(CONFIGURATION)) {
            writeSSL(writer, config.get(SSL).get(CONFIGURATION));
        }

    }

    static void writeStickySession(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.STICKY_SESSION.getLocalName());

        writeAttributeAs(writer, ENABLE, STICKY_SESSION, config);
        writeAttributeAs(writer, FORCE, STICKY_SESSION_FORCE, config);
        writeAttributeAs(writer, REMOVE, STICKY_SESSION_REMOVE, config);

        writer.writeEndElement();
    }

    static void writeAdvertise(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.ADVERTISE.getLocalName());

        writeAttributeAs(writer, ENABLE, ADVERTISE, config);
        writeAttributeAs(writer, SOCKET_BINDING, ADVERTISE_SOCKET, config);
        writeAttributeAs(writer, SECURITY_KEY, ADVERTISE_SECURITY_KEY, config);

        writer.writeEndElement();
    }

    static void writeProxies(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.PROXIES.getLocalName());

        writeAttributeAs(writer, URL, PROXY_URL, config);
        writeAttributeAs(writer, OUTBOUT_SOCKET_BINDINGS, PROXY_LIST, config);
        writeAttributeAs(writer, LOAD_BALANCING_GROUP, DOMAIN, config);

        writeAttribute(writer, PING, config);
        writeAttribute(writer, NODE_TIMEOUT, config);
        writeAttribute(writer, SOCKET_TIMEOUT, config);

        writeAttribute(writer, WORKER_TIMEOUT, config);
        writeAttribute(writer, MAX_ATTEMPTS, config);
        writeAttribute(writer, FLUSH_PACKETS, config);
        writeAttribute(writer, FLUSH_WAIT, config);
        writeAttribute(writer, SMAX, config);
        writeAttribute(writer, TTL, config);
        writeAttribute(writer, BALANCER, config);

        writer.writeEndElement();
    }

    static void writeContexts(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.CONTEXTS.getLocalName());

        writeAttributeAs(writer, AUTO_ENABLE, AUTO_ENABLE_CONTEXTS, config);
        writeAttributeAs(writer, STOP_TIMEOUT, STOP_CONTEXT_TIMEOUT, config);
        writeAttributeAs(writer, EXCLUDED_CONTEXTS, EXCLUDED_CONTEXTS, config);

        writer.writeEndElement();
    }

    /**
     * SSL
     */
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

    /**
     * Simple Load provider
     */
    static void writeSimpleLoadProvider(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.SIMPLE_LOAD_PROVIDER.getLocalName());
        writeAttribute(writer, FACTOR, config);
        writer.writeEndElement();
    }

    /**
     * Dynamic load provider
     */
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

    /**
     * Load Metric parsing logic
     */
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

    /**
     * Custom Load Metric parsing logic
     */
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

    /**
     * Enables you to specify what is the output name and which model you want to persist. TODO: Is there a better way?
     *
     * @since schema v1.1
     */
    static void writeAttributeAs(final XMLExtendedStreamWriter writer, final String name, final String modelName,
            ModelNode node) throws XMLStreamException {
        if (node.hasDefined(modelName)) {
            writer.writeAttribute(name, node.get(modelName).asString());
        }
    }

    /**
     * Property logic
     */
    static void writeProperty(final XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        writer.writeStartElement(Element.PROPERTY.getLocalName());
        writer.writeAttribute(NAME, property.getName());
        writer.writeAttribute(VALUE, property.getValue().asString());
        writer.writeEndElement();
    }
}
