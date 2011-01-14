/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.web.CommonAttributes.*;
import static org.jboss.as.web.CommonAttributes.ALIAS;
import static org.jboss.as.web.CommonAttributes.CONNECTOR;
import static org.jboss.as.web.CommonAttributes.DEFAULT_HOST;
import static org.jboss.as.web.CommonAttributes.DISABLED;
import static org.jboss.as.web.CommonAttributes.ENABLED;
import static org.jboss.as.web.CommonAttributes.ENABLE_LOOKUPS;
import static org.jboss.as.web.CommonAttributes.EXECUTOR;
import static org.jboss.as.web.CommonAttributes.FILE_ENCONDING;
import static org.jboss.as.web.CommonAttributes.JSP_CONFIGURATION;
import static org.jboss.as.web.CommonAttributes.LISTINGS;
import static org.jboss.as.web.CommonAttributes.MAX_DEPTH;
import static org.jboss.as.web.CommonAttributes.MAX_POST_SIZE;
import static org.jboss.as.web.CommonAttributes.MAX_SAVE_POST_SIZE;
import static org.jboss.as.web.CommonAttributes.MIME_MAPPING;
import static org.jboss.as.web.CommonAttributes.PROTOCOL;
import static org.jboss.as.web.CommonAttributes.PROXY_NAME;
import static org.jboss.as.web.CommonAttributes.PROXY_PORT;
import static org.jboss.as.web.CommonAttributes.READ_ONLY;
import static org.jboss.as.web.CommonAttributes.REDIRECT_PORT;
import static org.jboss.as.web.CommonAttributes.REWRITE;
import static org.jboss.as.web.CommonAttributes.SCHEME;
import static org.jboss.as.web.CommonAttributes.SECRET;
import static org.jboss.as.web.CommonAttributes.SECURE;
import static org.jboss.as.web.CommonAttributes.SENDFILE;
import static org.jboss.as.web.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.web.CommonAttributes.STATIC_RESOURCES;
import static org.jboss.as.web.CommonAttributes.WEBDAV;
import static org.jboss.as.web.CommonAttributes.WELCOME_FILE;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The web subsystem parser.
 *
 * @author Emanuel Muckenhuber
 */
class NewWebSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

    private static final NewWebSubsystemParser INSTANCE = new NewWebSubsystemParser();

    static NewWebSubsystemParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if(node.has(DEFAULT_HOST)) {
            writer.writeAttribute(Attribute.DEFAULT_HOST.getLocalName(), node.get(DEFAULT_HOST).asString());
        }
        if(node.has(CONTAINER_CONFIG)) {
            // FIXME
        }
        if(node.has(CONNECTOR)) {
            for(final Property connector : node.get(CONNECTOR).asPropertyList()) {
                final ModelNode config = connector.getValue();
                writer.writeStartElement(Element.CONNECTOR.getLocalName());
                writer.writeAttribute(NAME, connector.getName());
                writeAttribute(writer, Attribute.PROTOCOL.getLocalName(), config);
                writeAttribute(writer, Attribute.SOCKET_BINDING.getLocalName(), config);
                writeAttribute(writer, Attribute.SCHEME.getLocalName(), config);
                writeAttribute(writer, Attribute.ENABLED.getLocalName(), config);
                writeAttribute(writer, Attribute.ENABLE_LOOKUPS.getLocalName(), config);
                writeAttribute(writer, Attribute.PROXY_NAME.getLocalName(), config);
                writeAttribute(writer, Attribute.PROXY_PORT.getLocalName(), config);
                writeAttribute(writer, Attribute.SECURE.getLocalName(), config);
                writeAttribute(writer, Attribute.EXECUTOR.getLocalName(), config);
                writeAttribute(writer, Attribute.MAX_POST_SIZE.getLocalName(), config);
                writeAttribute(writer, Attribute.MAX_SAVE_POST_SIZE.getLocalName(), config);
                writer.writeEndElement();
            }
        }
        if(node.has(VIRTUAL_SERVER)) {
            for(final Property host : node.get(VIRTUAL_SERVER).asPropertyList()) {
                final ModelNode config = host.getValue();
                writer.writeStartElement(Element.VIRTUAL_SERVER.getLocalName());
                writer.writeAttribute(NAME, host.getName());
                if(config.has(ALIAS)) {
                    for(final ModelNode alias : config.get(ALIAS).asList()) {
                        writer.writeEmptyElement(ALIAS);
                        writer.writeAttribute(NAME, alias.asString());
                    }
                }
                // TODO other config elements
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).setEmptyObject();
        list.add(subsystem);

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONTAINER_CONFIG: {
                            final ModelNode config = parseContainerConfig(reader);
                            subsystem.get(REQUEST_PROPERTIES, CONTAINER_CONFIG).set(config);
                            break;
                        }
                        case CONNECTOR: {
                            parseConnector(reader, new ModelNode(), list);
                            break;
                        }
                        case VIRTUAL_SERVER: {
                            parseHost(reader, new ModelNode(), list);
                            break;
                        } default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    static ModelNode parseContainerConfig(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode config = new ModelNode();
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case STATIC_RESOURCES: {
                final ModelNode resourceServing = parseStaticResources(reader);
                config.get(STATIC_RESOURCES).set(resourceServing);
                break;
            }
            case JSP_CONFIGURATION: {
                final ModelNode jspConfiguration = parseJSPConfiguration(reader);
                config.get(JSP_CONFIGURATION).set(jspConfiguration);
                break;
            }
            case MIME_MAPPING: {
                final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
                config.get(MIME_MAPPING).get(array[0]).set(array[1]);
                break;
            }
            case WELCOME_FILE: {
                final String welcomeFile = reader.getElementText().trim();
                config.get(WELCOME_FILE).add(welcomeFile);
                break;
            }
            default:
                throw ParseUtils.unexpectedElement(reader);
            }
        }
        return config;
    }

    static ModelNode parseJSPConfiguration(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode jsp = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case DEVELOPMENT:
                case DISABLED:
                case KEEP_GENERATED:
                case TRIM_SPACES:
                case TAG_POOLING:
                case MAPPED_FILE:
                case CHECK_INTERVAL:
                case MODIFIFICATION_TEST_INTERVAL:
                case RECOMPILE_ON_FAIL:
                case SMAP:
                case DUMP_SMAP:
                case GENERATE_STRINGS_AS_CHAR_ARRAYS:
                case ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUT:
                case SCRATCH_DIR:
                case SOURCE_VM:
                case TARGET_VM:
                case JAVA_ENCODING:
                case X_POWERED_BY:
                case DISPLAY_SOOURCE_FRAGMENT:
                    jsp.get(attribute.getLocalName()).set(value);
                    break;
                default:
                    ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        return jsp;
    }

    static ModelNode parseStaticResources(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode resources = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case LISTINGS:
                    resources.get(LISTINGS).set(value);
                    break;
                case SENDFILE:
                    resources.get(SENDFILE).set(value);
                    break;
                case FILE_ENCONDING:
                    resources.get(FILE_ENCONDING).set(value);
                case READ_ONLY:
                    resources.get(READ_ONLY).set(value);
                    break;
                case WEBDAV:
                    resources.get(WEBDAV).set(value);
                    break;
                case SECRET:
                    resources.get(SECRET).set(value);
                    break;
                case MAX_DEPTH:
                    resources.get(MAX_DEPTH).set(value);
                    break;
                case DISABLED:
                    resources.get(DISABLED).set(value);
                    break;
                default:
                    ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        return resources;
    }

    static void parseHost(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    } default: {
                        ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
        }
        if(name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode host = new ModelNode();
        host.get(OP).set(ADD);
        host.get(OP_ADDR).set(address.clone().add(name));
        list.add(host);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case ALIAS:
                    host.get(ALIAS).add(readSingleAttributeNoContent(reader));
                    break;
                case ACCESS_LOG:
                    final ModelNode log = parseHostAccessLog(reader);
                    host.get(ACCESS_LOG).set(log);
                    break;
                case REWRITE:
                    final ModelNode rewrite = parseHostRewrite(reader);
                    host.get(REWRITE).set(rewrite);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
                }
                break;
            }
            default:
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    static ModelNode parseHostRewrite(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode rewrite = new ModelNode();
        return rewrite;
    }

    static ModelNode parseHostAccessLog(XMLExtendedStreamReader reader)  throws XMLStreamException {
        final ModelNode log = new ModelNode();
        return log;
    }

    static void parseConnector(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        String protocol = null;
        String bindingRef = null;
        String scheme = null;
        String executorRef = null;
        String enabled = null;
        String enableLookups = null;
        String proxyName = null;
        String proxyPort = null;
        String maxPostSize = null;
        String maxSavePostSize = null;
        String secure = null;
        String redirectPort = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case SOCKET_BINDING:
                    bindingRef = value;
                    break;
                case SCHEME:
                    scheme = value;
                    break;
                case PROTOCOL:
                    protocol = value;
                    break;
                case EXECUTOR:
                    executorRef = value;
                    break;
                case ENABLED:
                    enabled = value;
                    break;
                case ENABLE_LOOKUPS:
                    enableLookups = value;
                    break;
                case PROXY_NAME:
                    proxyName = value;
                    break;
                case PROXY_PORT:
                    proxyPort = value;
                    break;
                case MAX_POST_SIZE:
                    maxPostSize = value;
                    break;
                case MAX_SAVE_POST_SIZE:
                    maxSavePostSize = value;
                    break;
                case SECURE:
                    secure = value;
                    break;
                case REDIRECT_PORT:
                    redirectPort = value;
                    break;
                default:
                    ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (protocol == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PROTOCOL));
        }
        final ModelNode connector = new ModelNode();
        connector.get(OP).set(ADD);
        connector.get(OP_ADDR).set(address.clone().add(name));
        connector.get(REQUEST_PROPERTIES, PROTOCOL).set(protocol);
        connector.get(REQUEST_PROPERTIES, SOCKET_BINDING).set(bindingRef);
        connector.get(REQUEST_PROPERTIES, SCHEME).set(scheme);
        connector.get(REQUEST_PROPERTIES, EXECUTOR).set(executorRef);
        connector.get(REQUEST_PROPERTIES, ENABLED).set(enabled);
        connector.get(REQUEST_PROPERTIES, ENABLE_LOOKUPS).set(enableLookups);
        connector.get(REQUEST_PROPERTIES, PROXY_NAME).set(proxyName);
        connector.get(REQUEST_PROPERTIES, PROXY_PORT).set(proxyPort);
        connector.get(REQUEST_PROPERTIES, MAX_POST_SIZE).set(maxPostSize);
        connector.get(REQUEST_PROPERTIES, MAX_SAVE_POST_SIZE).set(maxSavePostSize);
        connector.get(REQUEST_PROPERTIES, SECURE).set(secure);
        connector.get(REQUEST_PROPERTIES, REDIRECT_PORT).set(redirectPort);
        list.add(connector);
    }


    static String readSingleAttributeNoContent(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if(count > 1) {
            throw ParseUtils.unexpectedAttribute(reader, 1);
        }
        final String value = reader.getAttributeValue(0);
        ParseUtils.requireNoContent(reader);
        return value.trim();
    }

    static void writeAttribute(final XMLExtendedStreamWriter writer, final String name, ModelNode node) throws XMLStreamException {
        if(node.has(name)) {
            writer.writeAttribute(name, node.get(name).asString());
        }
    }
}
