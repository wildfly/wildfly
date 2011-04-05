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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.web.Constants.ACCESS_LOG;
import static org.jboss.as.web.Constants.ALIAS;
import static org.jboss.as.web.Constants.CA_CERTIFICATE_FILE;
import static org.jboss.as.web.Constants.CA_REVOCATION_URL;
import static org.jboss.as.web.Constants.CERTIFICATE_FILE;
import static org.jboss.as.web.Constants.CERTIFICATE_KEY_FILE;
import static org.jboss.as.web.Constants.CIPHER_SUITE;
import static org.jboss.as.web.Constants.CONNECTOR;
import static org.jboss.as.web.Constants.CONDITION;
import static org.jboss.as.web.Constants.CONTAINER_CONFIG;
import static org.jboss.as.web.Constants.DEFAULT_WEB_MODULE;
import static org.jboss.as.web.Constants.DIRECTORY;
import static org.jboss.as.web.Constants.DISABLED;
import static org.jboss.as.web.Constants.ENABLED;
import static org.jboss.as.web.Constants.ENABLE_LOOKUPS;
import static org.jboss.as.web.Constants.EXECUTOR;
import static org.jboss.as.web.Constants.EXTENDED;
import static org.jboss.as.web.Constants.FILE_ENCONDING;
import static org.jboss.as.web.Constants.FLAGS;
import static org.jboss.as.web.Constants.JSP_CONFIGURATION;
import static org.jboss.as.web.Constants.KEY_ALIAS;
import static org.jboss.as.web.Constants.LISTINGS;
import static org.jboss.as.web.Constants.MAX_CONNECTIONS;
import static org.jboss.as.web.Constants.MAX_DEPTH;
import static org.jboss.as.web.Constants.MAX_POST_SIZE;
import static org.jboss.as.web.Constants.MAX_SAVE_POST_SIZE;
import static org.jboss.as.web.Constants.MIME_MAPPING;
import static org.jboss.as.web.Constants.NAME;
import static org.jboss.as.web.Constants.PASSWORD;
import static org.jboss.as.web.Constants.PATH;
import static org.jboss.as.web.Constants.PATTERN;
import static org.jboss.as.web.Constants.PREFIX;
import static org.jboss.as.web.Constants.PROTOCOL;
import static org.jboss.as.web.Constants.PROXY_NAME;
import static org.jboss.as.web.Constants.PROXY_PORT;
import static org.jboss.as.web.Constants.READ_ONLY;
import static org.jboss.as.web.Constants.REDIRECT_PORT;
import static org.jboss.as.web.Constants.RELATIVE_TO;
import static org.jboss.as.web.Constants.RESOLVE_HOSTS;
import static org.jboss.as.web.Constants.REWRITE;
import static org.jboss.as.web.Constants.ROTATE;
import static org.jboss.as.web.Constants.SCHEME;
import static org.jboss.as.web.Constants.SECRET;
import static org.jboss.as.web.Constants.SECURE;
import static org.jboss.as.web.Constants.SENDFILE;
import static org.jboss.as.web.Constants.SESSION_CACHE_SIZE;
import static org.jboss.as.web.Constants.SESSION_TIMEOUT;
import static org.jboss.as.web.Constants.SOCKET_BINDING;
import static org.jboss.as.web.Constants.SSL;
import static org.jboss.as.web.Constants.STATIC_RESOURCES;
import static org.jboss.as.web.Constants.SUBSTITUTION;
import static org.jboss.as.web.Constants.TEST;
import static org.jboss.as.web.Constants.VERIFY_CLIENT;
import static org.jboss.as.web.Constants.VERIFY_DEPTH;
import static org.jboss.as.web.Constants.VIRTUAL_SERVER;
import static org.jboss.as.web.Constants.WEBDAV;
import static org.jboss.as.web.Constants.WELCOME_FILE;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
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
 * @author Brian Stansberry
 */
class WebSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final WebSubsystemParser INSTANCE = new WebSubsystemParser();

    static WebSubsystemParser getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        writeAttribute(writer, Attribute.NATIVE.getLocalName(), node);
        writeAttribute(writer, Attribute.DEFAULT_VIRTUAL_SERVER.getLocalName(), node);
        if(node.hasDefined(CONTAINER_CONFIG)) {
            writeContainerConfig(writer, node.get(CONTAINER_CONFIG));
        }
        if(node.hasDefined(CONNECTOR)) {
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
                writeAttribute(writer, Attribute.MAX_CONNECTIONS.getLocalName(), config);
                if (config.hasDefined(SSL)) {
                    writer.writeStartElement(Element.SSL.getLocalName());
                    final ModelNode sslConfig = config.get(SSL);
                    writeAttribute(writer, Attribute.NAME.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.KEY_ALIAS.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.PASSWORD.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.CERTIFICATE_KEY_FILE.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.CIPHER_SUITE.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.PROTOCOL.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.VERIFY_CLIENT.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.VERIFY_DEPTH.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.CERTIFICATE_FILE.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.CA_CERTIFICATE_FILE.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.CA_REVOCATION_URL.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.SESSION_CACHE_SIZE.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.SESSION_TIMEOUT.getLocalName(), sslConfig);
                    writer.writeEndElement();
                }
                if (config.hasDefined(VIRTUAL_SERVER)) {
                    for(final ModelNode virtualServer : config.get(VIRTUAL_SERVER).asList()) {
                        writer.writeEmptyElement(VIRTUAL_SERVER);
                        writer.writeAttribute(NAME, virtualServer.asString());
                    }
                }
                writer.writeEndElement();
            }
        }
        if(node.hasDefined(VIRTUAL_SERVER)) {
            for(final Property host : node.get(VIRTUAL_SERVER).asPropertyList()) {
                final ModelNode config = host.getValue();
                writer.writeStartElement(Element.VIRTUAL_SERVER.getLocalName());
                writer.writeAttribute(NAME, host.getName());
                writeAttribute(writer, Attribute.DEFAULT_WEB_MODULE.getLocalName(), config);
                if(config.hasDefined(ALIAS)) {
                    for(final ModelNode alias : config.get(ALIAS).asList()) {
                        writer.writeEmptyElement(ALIAS);
                        writer.writeAttribute(NAME, alias.asString());
                    }
                }
                if(config.hasDefined(ACCESS_LOG)) {
                    writer.writeStartElement(Element.ACCESS_LOG.getLocalName());
                    final ModelNode accessLog = config.get(ACCESS_LOG);
                    if(accessLog.has(DIRECTORY)) {
                        writer.writeEmptyElement(DIRECTORY);
                        writeAttribute(writer, Attribute.PATH.getLocalName(), accessLog);
                        writeAttribute(writer, Attribute.RELATIVE_TO.getLocalName(), accessLog);
                    }
                    writeAttribute(writer, Attribute.PATTERN.getLocalName(), config);
                    writeAttribute(writer, Attribute.RESOLVE_HOSTS.getLocalName(), config);
                    writeAttribute(writer, Attribute.EXTENDED.getLocalName(), config);
                    writeAttribute(writer, Attribute.PREFIX.getLocalName(), config);
                    writeAttribute(writer, Attribute.ROTATE.getLocalName(), config);
                    writer.writeEndElement();
                }
                if (config.hasDefined(REWRITE)) {
                    for (final ModelNode rewrite : config.get(REWRITE).asList()) {
                        writer.writeStartElement(REWRITE);
                        if (rewrite.hasDefined(CONDITION)) {
                            for (final ModelNode condition : rewrite.get(CONDITION).asList()) {
                                writer.writeStartElement(CONDITION);
                                writeAttribute(writer, Attribute.TEST.getLocalName(), condition);
                                writeAttribute(writer, Attribute.PATTERN.getLocalName(), condition);
                                writeAttribute(writer, Attribute.FLAGS.getLocalName(), condition);
                                writer.writeEndElement();
                            }
                        }
                        writeAttribute(writer, Attribute.PATTERN.getLocalName(), rewrite);
                        writeAttribute(writer, Attribute.SUBSTITUTION.getLocalName(), rewrite);
                        writeAttribute(writer, Attribute.FLAGS.getLocalName(), rewrite);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeContainerConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
        if(config.hasDefined(STATIC_RESOURCES)) {
            writeStaticResources(writer, config.get(STATIC_RESOURCES));
        }
        if(config.hasDefined(JSP_CONFIGURATION)) {
            writeJSPConfiguration(writer, config.get(JSP_CONFIGURATION));
        }
        if(config.hasDefined(MIME_MAPPING)) {
            for(final Property entry : config.get(MIME_MAPPING).asPropertyList()) {
                writer.writeEmptyElement(Element.MIME_MAPPING.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().asString());
            }
        }
        if(config.hasDefined(WELCOME_FILE)) {
            for(final ModelNode file : config.get(WELCOME_FILE).asList()) {
                writer.writeStartElement(Element.WELCOME_FILE.getLocalName());
                writer.writeCharacters(file.asString());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeStaticResources(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        writer.writeStartElement(Element.STATIC_RESOURCES.getLocalName());

        writeAttribute(writer, Attribute.LISTINGS.getLocalName(), config);
        writeAttribute(writer, Attribute.SENDFILE.getLocalName(), config);
        writeAttribute(writer, Attribute.FILE_ENCONDING.getLocalName(), config);
        writeAttribute(writer, Attribute.READ_ONLY.getLocalName(), config);
        writeAttribute(writer, Attribute.WEBDAV.getLocalName(), config);
        writeAttribute(writer, Attribute.SECRET.getLocalName(), config);
        writeAttribute(writer, Attribute.MAX_DEPTH.getLocalName(), config);
        writeAttribute(writer, Attribute.DISABLED.getLocalName(), config);

        writer.writeEndElement();
    }

    private void writeJSPConfiguration(XMLExtendedStreamWriter writer, ModelNode jsp) throws XMLStreamException {
        writer.writeStartElement(Element.JSP_CONFIGURATION.getLocalName());

        writeAttribute(writer, Attribute.DEVELOPMENT.getLocalName(), jsp);
        writeAttribute(writer, Attribute.KEEP_GENERATED.getLocalName(), jsp);
        writeAttribute(writer, Attribute.TRIM_SPACES.getLocalName(), jsp);
        writeAttribute(writer, Attribute.TAG_POOLING.getLocalName(), jsp);
        writeAttribute(writer, Attribute.MAPPED_FILE.getLocalName(), jsp);
        writeAttribute(writer, Attribute.CHECK_INTERVAL.getLocalName(), jsp);
        writeAttribute(writer, Attribute.MODIFIFICATION_TEST_INTERVAL.getLocalName(), jsp);
        writeAttribute(writer, Attribute.RECOMPILE_ON_FAIL.getLocalName(), jsp);
        writeAttribute(writer, Attribute.SMAP.getLocalName(), jsp);
        writeAttribute(writer, Attribute.DUMP_SMAP.getLocalName(), jsp);
        writeAttribute(writer, Attribute.GENERATE_STRINGS_AS_CHAR_ARRAYS.getLocalName(), jsp);
        writeAttribute(writer, Attribute.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE.getLocalName(), jsp);
        writeAttribute(writer, Attribute.SCRATCH_DIR.getLocalName(), jsp);
        writeAttribute(writer, Attribute.SOURCE_VM.getLocalName(), jsp);
        writeAttribute(writer, Attribute.TARGET_VM.getLocalName(), jsp);
        writeAttribute(writer, Attribute.JAVA_ENCODING.getLocalName(), jsp);
        writeAttribute(writer, Attribute.X_POWERED_BY.getLocalName(), jsp);
        writeAttribute(writer, Attribute.DISPLAY_SOURCE_FRAGMENT.getLocalName(), jsp);
        writeAttribute(writer, Attribute.DISABLED.getLocalName(), jsp);
        writer.writeEndElement();
    }

    /** {@inheritDoc} */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NATIVE:
            case DEFAULT_VIRTUAL_SERVER:
                subsystem.get(attribute.getLocalName()).set(value);
                break;
            default:
                unexpectedAttribute(reader, i);
            }
        }
        list.add(subsystem);

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONTAINER_CONFIG: {
                            final ModelNode config = parseContainerConfig(reader);
                            subsystem.get(CONTAINER_CONFIG).set(config);
                            break;
                        }
                        case CONNECTOR: {
                            parseConnector(reader,address, list);
                            break;
                        }
                        case VIRTUAL_SERVER: {
                            parseHost(reader, address, list);
                            break;
                        } default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                } default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    static ModelNode parseContainerConfig(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode config = new ModelNode();
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
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
                throw unexpectedElement(reader);
            }
        }
        return config;
    }

    static ModelNode parseJSPConfiguration(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode jsp = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
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
            case ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE:
            case SCRATCH_DIR:
            case SOURCE_VM:
            case TARGET_VM:
            case JAVA_ENCODING:
            case X_POWERED_BY:
            case DISPLAY_SOURCE_FRAGMENT:
                jsp.get(attribute.getLocalName()).set(value);
                break;
            default:
                unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        return jsp;
    }

    static ModelNode parseStaticResources(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode resources = new ModelNode();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
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
                break;
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
                unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        return resources;
    }

    static void parseHost(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        String defaultWebModule = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case DEFAULT_WEB_MODULE:
                    defaultWebModule = value;
                    break;
                default:
                    unexpectedAttribute(reader, i);
            }
        }
        if(name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode host = new ModelNode();
        host.get(OP).set(ADD);
        host.get(OP_ADDR).set(address).add(VIRTUAL_SERVER, name);
        if (defaultWebModule != null) {
            host.get(DEFAULT_WEB_MODULE).set(defaultWebModule);
        }
        list.add(host);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case ALIAS:
                    host.get(ALIAS).add(readStringAttributeElement(reader, Attribute.NAME.getLocalName()));
                    break;
                case ACCESS_LOG:
                    final ModelNode log = parseHostAccessLog(reader);
                    host.get(ACCESS_LOG).set(log);
                    break;
                case REWRITE:
                    final ModelNode rewrite = parseHostRewrite(reader);
                    host.get(REWRITE).add(rewrite);
                    break;
                default:
                    throw unexpectedElement(reader);
                }
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    static ModelNode parseHostRewrite(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode rewrite = new ModelNode();
        rewrite.setEmptyObject();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case PATTERN:
                rewrite.get(PATTERN).set(value);
                break;
            case SUBSTITUTION:
                rewrite.get(SUBSTITUTION).set(value);
                break;
            case FLAGS:
                rewrite.get(REWRITE).set(value);
                break;
            default:
                unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case CONDITION:
                    final ModelNode condition = new ModelNode();
                    condition.setEmptyObject();
                    final int count2 = reader.getAttributeCount();
                    for (int i = 0; i < count2; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                        case TEST:
                            condition.get(TEST).set(value);
                            break;
                        case PATTERN:
                            condition.get(PATTERN).set(value);
                            break;
                        case FLAGS:
                            condition.get(FLAGS).set(value);
                            break;
                        default:
                            unexpectedAttribute(reader, i);
                        }
                    }
                    rewrite.get(CONDITION).add(condition);
                    break;
                default:
                    throw unexpectedElement(reader);
                }
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
        return rewrite;
    }

    static ModelNode parseHostAccessLog(XMLExtendedStreamReader reader)  throws XMLStreamException {
        final ModelNode log = new ModelNode();
        log.setEmptyObject();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case PATTERN:
                log.get(PATTERN).set(value);
                break;
            case RESOLVE_HOSTS:
                log.get(RESOLVE_HOSTS).set(value);
                break;
            case EXTENDED:
                log.get(EXTENDED).set(value);
                break;
            case PREFIX:
                log.get(PREFIX).set(value);
                break;
            case ROTATE:
                log.get(ROTATE).set(value);
                break;
            default:
                unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case DIRECTORY:
                    final ModelNode directory = new ModelNode();
                    log.get(DIRECTORY).set(directory);
                    final int count2 = reader.getAttributeCount();
                    for (int i = 0; i < count2; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                        case PATH:
                            directory.get(PATH).set(value);
                            break;
                        case RELATIVE_TO:
                            directory.get(RELATIVE_TO).set(value);
                            break;
                        default:
                            unexpectedAttribute(reader, i);
                        }
                    }
                    break;
                default:
                    throw unexpectedElement(reader);
                }
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
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
        String maxConnections = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
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
            case MAX_CONNECTIONS:
                maxConnections = value;
                break;
            default:
                unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (protocol == null) {
            missingRequired(reader, Collections.singleton(Attribute.PROTOCOL));
        }
        final ModelNode connector = new ModelNode();
        connector.get(OP).set(ADD);
        connector.get(OP_ADDR).set(address).add(CONNECTOR, name);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case SSL:
                    final ModelNode ssl = parseSsl(reader);
                    connector.get(SSL).set(ssl);
                    break;
                case VIRTUAL_SERVER:
                    connector.get(VIRTUAL_SERVER).add(readStringAttributeElement(reader, Attribute.NAME.getLocalName()));
                    break;
                default:
                    throw unexpectedElement(reader);
                }
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
        if(protocol != null) connector.get(PROTOCOL).set(protocol);
        connector.get(SOCKET_BINDING).set(bindingRef);
        if(scheme != null) connector.get(SCHEME).set(scheme);
        if(executorRef != null) connector.get(EXECUTOR).set(executorRef);
        if(enabled != null) connector.get(ENABLED).set(enabled);
        if(enableLookups != null) connector.get(ENABLE_LOOKUPS).set(enableLookups);
        if(proxyName != null) connector.get(PROXY_NAME).set(proxyName);
        if(proxyPort != null) connector.get(PROXY_PORT).set(proxyPort);
        if(maxPostSize != null) connector.get(MAX_POST_SIZE).set(maxPostSize);
        if(maxSavePostSize != null) connector.get(MAX_SAVE_POST_SIZE).set(maxSavePostSize);
        if(secure != null) connector.get(SECURE).set(secure);
        if(redirectPort != null) connector.get(REDIRECT_PORT).set(redirectPort);
        if(maxConnections != null) connector.get(MAX_CONNECTIONS).set(maxConnections);
        list.add(connector);
    }

    static ModelNode parseSsl(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode ssl = new ModelNode();
        ssl.setEmptyObject();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case NAME:
                ssl.get(NAME).set(value);
                break;
            case KEY_ALIAS:
                ssl.get(KEY_ALIAS).set(value);
                break;
            case PASSWORD:
                ssl.get(PASSWORD).set(value);
            case CERTIFICATE_KEY_FILE:
                ssl.get(CERTIFICATE_KEY_FILE).set(value);
                break;
            case CIPHER_SUITE:
                ssl.get(CIPHER_SUITE).set(value);
                break;
            case PROTOCOL:
                ssl.get(PROTOCOL).set(value);
                break;
            case VERIFY_CLIENT:
                ssl.get(VERIFY_CLIENT).set(value);
                break;
            case VERIFY_DEPTH:
                ssl.get(VERIFY_DEPTH).set(Integer.valueOf(value));
                break;
            case CERTIFICATE_FILE:
                ssl.get(CERTIFICATE_FILE).set(value);
                break;
            case CA_CERTIFICATE_FILE:
                ssl.get(CA_CERTIFICATE_FILE).set(value);
                break;
            case CA_REVOCATION_URL:
                ssl.get(CA_REVOCATION_URL).set(value);
                break;
            case SESSION_CACHE_SIZE:
                ssl.get(SESSION_CACHE_SIZE).set(value);
                break;
            case SESSION_TIMEOUT:
                ssl.get(SESSION_TIMEOUT).set(value);
                break;
           default:
                unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        return ssl;
    }

    static void writeAttribute(final XMLExtendedStreamWriter writer, final String name, ModelNode node) throws XMLStreamException {
        if(node.hasDefined(name)) {
            writer.writeAttribute(name, node.get(name).asString());
        }
    }
}
