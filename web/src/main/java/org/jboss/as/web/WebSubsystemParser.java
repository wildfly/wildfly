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
import static org.jboss.as.web.Constants.*;
import static org.jboss.as.web.WebMessages.MESSAGES;

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
        writeAttribute(writer, Attribute.INSTANCE_ID.getLocalName(), node);
        if(node.hasDefined(CONTAINER_CONFIG)) {
            writeContainerConfig(writer, node.get(CONTAINER_CONFIG));
        }
        if(node.hasDefined(CONNECTOR)) {
            for(final Property connector : node.get(CONNECTOR).asPropertyList()) {
                final ModelNode config = connector.getValue();
                writer.writeStartElement(Element.CONNECTOR.getLocalName());
                writer.writeAttribute(NAME, connector.getName());
                writeAttribute(writer, Attribute.PROTOCOL.getLocalName(), config);
                writeAttribute(writer, Attribute.SCHEME.getLocalName(), config);
                writeAttribute(writer, Attribute.SOCKET_BINDING.getLocalName(), config);
                writeAttribute(writer, Attribute.ENABLE_LOOKUPS.getLocalName(), config);
                writeAttribute(writer, Attribute.PROXY_NAME.getLocalName(), config);
                writeAttribute(writer, Attribute.PROXY_PORT.getLocalName(), config);
                writeAttribute(writer, Attribute.REDIRECT_PORT.getLocalName(), config);
                writeAttribute(writer, Attribute.SECURE.getLocalName(), config);
                writeAttribute(writer, Attribute.MAX_POST_SIZE.getLocalName(), config);
                writeAttribute(writer, Attribute.MAX_SAVE_POST_SIZE.getLocalName(), config);
                writeAttribute(writer, Attribute.ENABLED.getLocalName(), config);
                writeAttribute(writer, Attribute.EXECUTOR.getLocalName(), config);
                writeAttribute(writer, Attribute.MAX_CONNECTIONS.getLocalName(), config);

                if (config.get(SSL).isDefined() && config.get(SSL).has("configuration")) {
                    ModelNode sslConfig = config.get(SSL).get("configuration");
                    writer.writeStartElement(Element.SSL.getLocalName());
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
                    writeAttribute(writer, Attribute.CA_CERTIFICATE_PASSWORD.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.KEYSTORE_TYPE.getLocalName(), sslConfig);
                    writeAttribute(writer, Attribute.TRUSTSTORE_TYPE.getLocalName(), sslConfig);
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
                if (config.hasDefined(ENABLE_WELCOME_ROOT)) {
                    writer.writeAttribute(ENABLE_WELCOME_ROOT, String.valueOf(config.get(ENABLE_WELCOME_ROOT).asBoolean()));
                }

                if(config.hasDefined(ALIAS)) {
                    for(final ModelNode alias : config.get(ALIAS).asList()) {
                        writer.writeEmptyElement(ALIAS);
                        writer.writeAttribute(NAME, alias.asString());
                    }
                }

                if (config.get(ACCESS_LOG).isDefined() && config.get(ACCESS_LOG).has("configuration")) {
                    ModelNode accessLog = config.get(ACCESS_LOG).get("configuration");
                    writer.writeStartElement(Element.ACCESS_LOG.getLocalName());
                    writeAttribute(writer, Attribute.PATTERN.getLocalName(), accessLog);
                    writeAttribute(writer, Attribute.RESOLVE_HOSTS.getLocalName(), accessLog);
                    writeAttribute(writer, Attribute.EXTENDED.getLocalName(), accessLog);
                    writeAttribute(writer, Attribute.PREFIX.getLocalName(), accessLog);
                    writeAttribute(writer, Attribute.ROTATE.getLocalName(), accessLog);

                    if(accessLog.has(DIRECTORY) && accessLog.get(DIRECTORY).has("configuration")) {
                        ModelNode directory = accessLog.get(DIRECTORY).get("configuration");
                        String name = Element.DIRECTORY.getLocalName();
                        boolean startwritten = false;
                        startwritten = writeAttribute(writer, Attribute.PATH.getLocalName(), directory, startwritten, name);
                        startwritten = writeAttribute(writer, Attribute.RELATIVE_TO.getLocalName(), directory, startwritten, name);
                        if (startwritten)
                            writer.writeEndElement();
                    }
                    writer.writeEndElement();
                }

                if (config.hasDefined(REWRITE)) {
                    for (final ModelNode rewritenode : config.get(REWRITE).asList()) {
                        String name = getAddedRule(rewritenode);
                        ModelNode rewrite;
                        if (rewritenode.hasDefined(name))
                            rewrite = rewritenode.get(name);
                        else
                            rewrite = rewritenode;
                        writer.writeStartElement(REWRITE);
                        writeAttribute(writer, Attribute.PATTERN.getLocalName(), rewrite);
                        writeAttribute(writer, Attribute.SUBSTITUTION.getLocalName(), rewrite);
                        writeAttribute(writer, Attribute.FLAGS.getLocalName(), rewrite);

                        if (rewrite.hasDefined(CONDITION)) {
                            for (final ModelNode conditionnode : rewrite.get(CONDITION).asList()) {
                                String condname = getAddedConditionName(conditionnode);
                                ModelNode condition;
                                if (conditionnode.hasDefined(condname))
                                    condition = conditionnode.get(condname);
                                else
                                    condition = conditionnode;
                                writer.writeStartElement(CONDITION);
                                writeAttribute(writer, Attribute.TEST.getLocalName(), condition);
                                writeAttribute(writer, Attribute.PATTERN.getLocalName(), condition);
                                writeAttribute(writer, Attribute.FLAGS.getLocalName(), condition);
                                writer.writeEndElement();
                            }
                        }
                        writer.writeEndElement();
                    }
                }

                if(config.hasDefined(SSO) && config.get(SSO).has("configuration")) {
                    final ModelNode sso;
                    sso = config.get(SSO).get("configuration");
                    writer.writeStartElement(SSO);
                    writeAttribute(writer, Attribute.CACHE_CONTAINER.getLocalName(), sso);
                    writeAttribute(writer, Attribute.CACHE_NAME.getLocalName(), sso);
                    writeAttribute(writer, Attribute.DOMAIN.getLocalName(), sso);
                    writeAttribute(writer, Attribute.REAUTHENTICATE.getLocalName(), sso);
                    writer.writeEndElement();
                }

                // End of the VIRTUAL_SERVER
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private String getAddedConditionName(ModelNode conditionnode) {
        for(final String attribute : conditionnode.keys()) {
            if (attribute.startsWith("condition-"))
                return attribute;
        }
        return "condition-0";
    }

    private String getAddedRule(ModelNode rewritenode) {
        for(final String attribute : rewritenode.keys()) {
            if (attribute.startsWith("rule-"))
                return attribute;
        }
        return "rule-0";
    }

    private void writeContainerConfig(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {
        boolean containerConfigStartWritten = false;
        if(config.hasDefined(STATIC_RESOURCES)) {
            containerConfigStartWritten = writeStaticResources(writer, config.get(STATIC_RESOURCES));
        }
        if(config.hasDefined(JSP_CONFIGURATION)) {
            containerConfigStartWritten = writeJSPConfiguration(writer, config.get(JSP_CONFIGURATION), containerConfigStartWritten) || containerConfigStartWritten ;
        }
        ModelNode container = config;
        if(config.hasDefined(CONTAINER)) {
            // this has been added to get the stuff manageable
            container = config.get(CONTAINER);
        }
        if(container.hasDefined(MIME_MAPPING)) {
            if (!containerConfigStartWritten) {
                writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                containerConfigStartWritten = true;
            }
            for(final Property entry : container.get(MIME_MAPPING).asPropertyList()) {
                writer.writeEmptyElement(Element.MIME_MAPPING.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().asString());
            }
        }
        if(container.hasDefined(WELCOME_FILE)) {
            if (!containerConfigStartWritten) {
                writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                containerConfigStartWritten = true;
            }
            for(final ModelNode file : container.get(WELCOME_FILE).asList()) {
                writer.writeStartElement(Element.WELCOME_FILE.getLocalName());
                writer.writeCharacters(file.asString());
                writer.writeEndElement();
            }
        }
        if (containerConfigStartWritten) {
            writer.writeEndElement();
        }
    }

    private boolean writeStaticResources(XMLExtendedStreamWriter writer, ModelNode config) throws XMLStreamException {

        boolean startWritten = writeStaticResourceAttribute(writer, Attribute.LISTINGS.getLocalName(), config, false);
        startWritten = writeStaticResourceAttribute(writer, Attribute.SENDFILE.getLocalName(), config, startWritten) || startWritten;
        startWritten = writeStaticResourceAttribute(writer, Attribute.FILE_ENCONDING.getLocalName(), config, startWritten) || startWritten;
        startWritten = writeStaticResourceAttribute(writer, Attribute.READ_ONLY.getLocalName(), config, startWritten) || startWritten;
        startWritten = writeStaticResourceAttribute(writer, Attribute.WEBDAV.getLocalName(), config, startWritten) || startWritten;
        startWritten = writeStaticResourceAttribute(writer, Attribute.SECRET.getLocalName(), config, startWritten) || startWritten;
        startWritten = writeStaticResourceAttribute(writer, Attribute.MAX_DEPTH.getLocalName(), config, startWritten) || startWritten;
        startWritten = writeStaticResourceAttribute(writer, Attribute.DISABLED.getLocalName(), config, startWritten) || startWritten;

        if (startWritten) {
            writer.writeEndElement();
        }
        return startWritten;
    }

    private boolean writeStaticResourceAttribute(XMLExtendedStreamWriter writer, String attribute, ModelNode config, boolean startWritten) throws XMLStreamException {
        if (DefaultStaticResources.hasNotDefault(config, attribute)) {
            if (!startWritten) {
                writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                writer.writeStartElement(Element.STATIC_RESOURCES.getLocalName());
            }
            writer.writeAttribute(attribute, config.get(attribute).asString());
            return true;
        }
        return false;
    }

    private boolean writeJSPConfiguration(XMLExtendedStreamWriter writer, ModelNode jsp, boolean containerConfigStartWritten) throws XMLStreamException {

        boolean startWritten = writeJspConfigAttribute(writer, Attribute.DEVELOPMENT.getLocalName(), jsp, false, containerConfigStartWritten);
        startWritten = writeJspConfigAttribute(writer, Attribute.DISABLED.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.KEEP_GENERATED.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.TRIM_SPACES.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.TAG_POOLING.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.MAPPED_FILE.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.CHECK_INTERVAL.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.MODIFIFICATION_TEST_INTERVAL.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.RECOMPILE_ON_FAIL.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.SMAP.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.DUMP_SMAP.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.GENERATE_STRINGS_AS_CHAR_ARRAYS.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.SCRATCH_DIR.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.SOURCE_VM.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.TARGET_VM.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.JAVA_ENCODING.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.X_POWERED_BY.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;
        startWritten = writeJspConfigAttribute(writer, Attribute.DISPLAY_SOURCE_FRAGMENT.getLocalName(), jsp, startWritten, containerConfigStartWritten) || startWritten;

        if (startWritten) {
            writer.writeEndElement();
        }

        return startWritten;
    }

    private boolean writeJspConfigAttribute(XMLExtendedStreamWriter writer, String attribute, ModelNode config,
                                            boolean startWritten, boolean containerConfigStartWritten) throws XMLStreamException {
        if (DefaultJspConfig.hasNotDefault(config, attribute)) {
            if (!startWritten) {
                if (!containerConfigStartWritten) {
                    writer.writeStartElement(Element.CONTAINER_CONFIG.getLocalName());
                }
                writer.writeStartElement(Element.JSP_CONFIGURATION.getLocalName());
            }
            writer.writeAttribute(attribute, config.get(attribute).asString());
            return true;
        }
        return false;
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
            case INSTANCE_ID:
                subsystem.get(attribute.getLocalName()).set(value);
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        list.add(subsystem);

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0:
                case WEB_1_1:{
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
                requireNoContent(reader);
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
                throw unexpectedAttribute(reader, i);
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
                throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        return resources;
    }

    static void parseHost(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
        String name = null;
        String defaultWebModule = null;
        boolean welcome = false;
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
                    if (welcome)
                        throw new XMLStreamException(MESSAGES.noRootWebappWithWelcomeWebapp(), reader.getLocation());
                    defaultWebModule = value;
                    break;
                case ENABLE_WELCOME_ROOT:
                    welcome = Boolean.parseBoolean(value);
                    if (welcome && defaultWebModule != null)
                        throw new XMLStreamException(MESSAGES.noWelcomeWebappWithDefaultWebModule(), reader.getLocation());
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
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
        host.get(ENABLE_WELCOME_ROOT).set(welcome);
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
            case WEB_1_1: {
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
                case SSO:
                    final ModelNode sso = parseSso(reader);
                    host.get(SSO).set(sso);
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

    static ModelNode parseSso(XMLExtendedStreamReader reader) throws XMLStreamException {
        final ModelNode sso = new ModelNode();
        sso.setEmptyObject();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
            case CACHE_CONTAINER:
            case CACHE_NAME:
            case DOMAIN:
            case REAUTHENTICATE:
                sso.get(attribute.getLocalName()).set(value);
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        return sso;
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
                rewrite.get(FLAGS).set(value);
                break;
            default:
                throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0:
            case WEB_1_1: {
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
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                    requireNoContent(reader);
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
                throw unexpectedAttribute(reader, i);
            }
        }
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0:
            case WEB_1_1: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case DIRECTORY:
                    final ModelNode directory = new ModelNode();
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
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                    requireNoContent(reader);
                    log.get(DIRECTORY).set(directory);
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
                throw unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (bindingRef == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.SOCKET_BINDING));
        }
        final ModelNode connector = new ModelNode();
        connector.get(OP).set(ADD);
        connector.get(OP_ADDR).set(address).add(CONNECTOR, name);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0:
            case WEB_1_1: {
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
                ssl.get(PASSWORD).setExpression(value);
                break;
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
            case CA_CERTIFICATE_PASSWORD:
                ssl.get(CA_CERTIFICATE_PASSWORD).set(value);
                break;
            case KEYSTORE_TYPE:
                ssl.get(KEYSTORE_TYPE).set(value);
                break;
            case TRUSTSTORE_TYPE:
                ssl.get(TRUSTSTORE_TYPE).set(value);
                break;
           default:
                throw unexpectedAttribute(reader, i);
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
    private boolean writeAttribute(XMLExtendedStreamWriter writer, String name, ModelNode node, boolean startwritten, String origin) throws XMLStreamException {
        if(node.hasDefined(name)) {
            if (!startwritten) {
                startwritten = true;
                writer.writeStartElement(origin);
            }
            writer.writeAttribute(name, node.get(name).asString());
        }
        return startwritten;
    }


}
